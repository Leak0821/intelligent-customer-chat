package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchStatus;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReplySendLifecycleService {
    private final WorkflowRunRepository workflowRunRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final ReplyDispatchRepository replyDispatchRepository;
    private final MailReceiptRepository mailReceiptRepository;
    private final OutboundMailSender outboundMailSender;
    private final DispatchRetryPolicy dispatchRetryPolicy;

    public ReplySendLifecycleService(WorkflowRunRepository workflowRunRepository,
                                     ReplyDraftRepository replyDraftRepository,
                                     ReplyDispatchRepository replyDispatchRepository,
                                     MailReceiptRepository mailReceiptRepository,
                                     OutboundMailSender outboundMailSender,
                                     DispatchRetryPolicy dispatchRetryPolicy) {
        this.workflowRunRepository = workflowRunRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.mailReceiptRepository = mailReceiptRepository;
        this.outboundMailSender = outboundMailSender;
        this.dispatchRetryPolicy = dispatchRetryPolicy;
    }

    public ReplyDraft approveForSend(String runId, String approvalNote) {
        ReplyDraft draft = requireDraft(runId);
        if (draft.getStatus() == ReplyDraftStatus.BLOCKED || draft.getStatus() == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            throw new IllegalStateException("draft is not eligible for send approval, status=" + draft.getStatus());
        }
        // 第一阶段用显式放行代替真实人工审核台，先把“可发”和“已生成”分开。
        draft.updateSendReadiness(SendReadiness.READY_FOR_SEND, "dispatch_reply", approvalNote);
        replyDraftRepository.save(draft);
        return draft;
    }

    public ReplyDispatch dispatch(String runId) {
        return dispatchInternal(runId, false);
    }

    public ReplyDispatch retryDispatch(String runId) {
        return dispatchInternal(runId, true);
    }

    private ReplyDispatch dispatchInternal(String runId, boolean retryMode) {
        WorkflowRun run = workflowRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("workflow run not found for runId=" + runId));
        ReplyDraft draft = requireDraft(runId);
        if (draft.getSendReadiness() != SendReadiness.READY_FOR_SEND) {
            throw new IllegalStateException("draft is not ready for send, sendReadiness=" + draft.getSendReadiness());
        }
        MailReceipt receipt = mailReceiptRepository.findByMessageId(run.getMessageId())
                .orElseThrow(() -> new NoSuchElementException("mail receipt not found for messageId=" + run.getMessageId()));
        ReplyDispatch dispatch = replyDispatchRepository.findLatestByRunId(runId)
                .map(existing -> validateExistingDispatch(existing, retryMode))
                .orElseGet(() -> {
                    if (retryMode) {
                        throw new NoSuchElementException("dispatch not found for retry, runId=" + runId);
                    }
                    return ReplyDispatch.create(runId, draft.getDraftId(), receipt.getSender(), draft.getSubject(), draft.getBody(), dispatchRetryPolicy.maxAttempts());
                });
        OutboundMailSendResult sendResult = outboundMailSender.send(
                new OutboundMailRequest(receipt.getSender(), draft.getSubject(), draft.getBody())
        );
        dispatch = applySendResult(dispatch, sendResult);
        replyDispatchRepository.save(dispatch);
        if (sendResult.success()) {
            draft.updateSendReadiness(SendReadiness.HOLD, "already_dispatched", "reply dispatched through outbound adapter");
            replyDraftRepository.save(draft);
        } else if (dispatch.isRetryPending()) {
            draft.updateSendReadiness(SendReadiness.READY_FOR_SEND, "await_dispatch_retry", "dispatch failed but retry has been scheduled");
            replyDraftRepository.save(draft);
        } else if (dispatch.isFailedFinal()) {
            draft.updateSendReadiness(SendReadiness.HOLD, "investigate_dispatch_failure", "dispatch exhausted retries and requires manual follow-up");
            replyDraftRepository.save(draft);
        }
        return dispatch;
    }

    public List<ReplyDispatch> findDispatches(String runId) {
        return replyDispatchRepository.findByRunId(runId);
    }

    private ReplyDraft requireDraft(String runId) {
        return replyDraftRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("draft not found for runId=" + runId));
    }

    private ReplyDispatch validateExistingDispatch(ReplyDispatch existing, boolean retryMode) {
        if (existing.isSent()) {
            throw new IllegalStateException("draft already dispatched for runId=" + existing.getRunId());
        }
        if (!retryMode && existing.isRetryPending()) {
            throw new IllegalStateException("dispatch retry already scheduled for runId=" + existing.getRunId());
        }
        if (retryMode && !existing.isRetryPending()) {
            throw new IllegalStateException("dispatch is not pending retry for runId=" + existing.getRunId() + ", status=" + existing.getStatus());
        }
        return existing;
    }

    private ReplyDispatch applySendResult(ReplyDispatch dispatch, OutboundMailSendResult sendResult) {
        if (!sendResult.success()) {
            OffsetDateTime nextRetryAt = dispatchRetryPolicy.computeNextRetryAt(dispatch, sendResult.attemptedAt());
            dispatch.markAttemptResult(false, null, sendResult.errorMessage(), sendResult.attemptedAt(), nextRetryAt);
            return dispatch;
        }
        dispatch.markAttemptResult(true, sendResult.providerMessageId(), null, sendResult.attemptedAt(), null);
        return dispatch;
    }
}

package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReplySendLifecycleService {
    private final WorkflowRunRepository workflowRunRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final ReplyDispatchRepository replyDispatchRepository;
    private final MailReceiptRepository mailReceiptRepository;
    private final OutboundMailSender outboundMailSender;

    public ReplySendLifecycleService(WorkflowRunRepository workflowRunRepository,
                                     ReplyDraftRepository replyDraftRepository,
                                     ReplyDispatchRepository replyDispatchRepository,
                                     MailReceiptRepository mailReceiptRepository,
                                     OutboundMailSender outboundMailSender) {
        this.workflowRunRepository = workflowRunRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.mailReceiptRepository = mailReceiptRepository;
        this.outboundMailSender = outboundMailSender;
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
        WorkflowRun run = workflowRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("workflow run not found for runId=" + runId));
        ReplyDraft draft = requireDraft(runId);
        if (draft.getSendReadiness() != SendReadiness.READY_FOR_SEND) {
            throw new IllegalStateException("draft is not ready for send, sendReadiness=" + draft.getSendReadiness());
        }
        replyDispatchRepository.findLatestByRunId(runId)
                .filter(dispatch -> dispatch.getStatus() == com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchStatus.SENT)
                .ifPresent(dispatch -> {
                    throw new IllegalStateException("draft already dispatched for runId=" + runId);
                });

        MailReceipt receipt = mailReceiptRepository.findByMessageId(run.getMessageId())
                .orElseThrow(() -> new NoSuchElementException("mail receipt not found for messageId=" + run.getMessageId()));
        OutboundMailSendResult sendResult = outboundMailSender.send(
                new OutboundMailRequest(receipt.getSender(), draft.getSubject(), draft.getBody())
        );
        ReplyDispatch dispatch = sendResult.success()
                ? ReplyDispatch.sent(runId, draft.getDraftId(), receipt.getSender(), draft.getSubject(), draft.getBody(), sendResult.providerMessageId())
                : ReplyDispatch.failed(runId, draft.getDraftId(), receipt.getSender(), draft.getSubject(), draft.getBody(), sendResult.errorMessage());
        replyDispatchRepository.save(dispatch);
        if (sendResult.success()) {
            draft.updateSendReadiness(SendReadiness.HOLD, "already_dispatched", "reply dispatched through outbound adapter");
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
}

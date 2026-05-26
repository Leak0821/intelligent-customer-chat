package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReplyDispatchCompensationService {
    private final ReplyDispatchRepository replyDispatchRepository;
    private final ReplySendLifecycleService replySendLifecycleService;
    private final DispatchRetryPolicy dispatchRetryPolicy;

    public ReplyDispatchCompensationService(ReplyDispatchRepository replyDispatchRepository,
                                            ReplySendLifecycleService replySendLifecycleService,
                                            DispatchRetryPolicy dispatchRetryPolicy) {
        this.replyDispatchRepository = replyDispatchRepository;
        this.replySendLifecycleService = replySendLifecycleService;
        this.dispatchRetryPolicy = dispatchRetryPolicy;
    }

    public ReplyDispatch retry(String runId) {
        ReplyDispatch dispatch = replyDispatchRepository.findLatestByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("dispatch not found for runId=" + runId));
        if (dispatch.isSent()) {
            throw new IllegalStateException("dispatch already sent for runId=" + runId);
        }
        if (!dispatch.isRetryPending()) {
            throw new IllegalStateException("dispatch is not waiting for retry, status=" + dispatch.getStatus());
        }
        return replySendLifecycleService.retryDispatch(runId);
    }

    public DispatchRetryBatchResult retryDueDispatches() {
        List<ReplyDispatch> dueDispatches = replyDispatchRepository.findRetryableDueBefore(
                OffsetDateTime.now(),
                dispatchRetryPolicy.batchSize()
        );
        int sentCount = 0;
        int retryPendingCount = 0;
        int failedFinalCount = 0;
        for (ReplyDispatch dispatch : dueDispatches) {
            ReplyDispatch retried = replySendLifecycleService.retryDispatch(dispatch.getRunId());
            if (retried.isSent()) {
                sentCount += 1;
            } else if (retried.isRetryPending()) {
                retryPendingCount += 1;
            } else if (retried.isFailedFinal()) {
                failedFinalCount += 1;
            }
        }
        return new DispatchRetryBatchResult(dueDispatches.size(), sentCount, retryPendingCount, failedFinalCount);
    }
}

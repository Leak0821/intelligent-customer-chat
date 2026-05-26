package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.reply.ReplySendLifecycleService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.DispatchTriggerSource;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:intelligent_customer_chat_review_lifecycle;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("test")
class ReplyReviewLifecycleServiceTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private ReplyReviewLifecycleService replyReviewLifecycleService;

    @Autowired
    private ReplyDraftRevisionService replyDraftRevisionService;

    @Autowired
    private ReplySendLifecycleService replySendLifecycleService;

    @Test
    void shouldRecordApprovalAndUseReviewAuditOnDispatch() {
        var run = mailIngestionService.process(new InboundMail(
                "msg-review-1",
                "thread-review-1",
                "customer@example.com",
                "Need recommendation",
                "Please recommend a product for my living room.",
                OffsetDateTime.now()
        ));

        var approvedDraft = replyReviewLifecycleService.approveForSend(run.getRunId(), "auditor-a", "content verified");
        assertThat(approvedDraft.getSendReadiness()).isEqualTo(SendReadiness.READY_FOR_SEND);
        assertThat(approvedDraft.getDraftVersion()).isEqualTo(1);

        var reviews = replyReviewLifecycleService.findReviews(run.getRunId());
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getAction()).isEqualTo(ReviewAction.APPROVE_SEND);
        assertThat(reviews.get(0).getReviewer()).isEqualTo("auditor-a");

        var dispatch = replySendLifecycleService.dispatch(run.getRunId());
        assertThat(dispatch.getLatestTriggerSource()).isEqualTo(DispatchTriggerSource.MANUAL_APPROVAL);
        assertThat(dispatch.getLatestTriggeredBy()).isEqualTo("auditor-a");
        assertThat(dispatch.getLatestTriggerReason()).isEqualTo("content verified");
    }

    @Test
    void shouldRecordRejectAndHoldDraftForManualFollowUp() {
        var run = mailIngestionService.process(new InboundMail(
                "msg-review-2",
                "thread-review-2",
                "customer@example.com",
                "Need recommendation",
                "Please recommend a product for my living room.",
                OffsetDateTime.now()
        ));

        var rejectedDraft = replyReviewLifecycleService.rejectSend(run.getRunId(), "auditor-b", "promise wording needs revision");
        assertThat(rejectedDraft.getStatus()).isEqualTo(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(rejectedDraft.getSendReadiness()).isEqualTo(SendReadiness.HOLD);
        assertThat(rejectedDraft.getNextAction()).isEqualTo("manual_review_required");

        var reviews = replyReviewLifecycleService.findReviews(run.getRunId());
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getAction()).isEqualTo(ReviewAction.REJECT_SEND);
        assertThat(reviews.get(0).getReviewer()).isEqualTo("auditor-b");
        assertThat(reviews.get(0).getReviewNote()).isEqualTo("promise wording needs revision");
    }

    @Test
    void shouldReviseRejectedDraftAndResubmitForReview() {
        var run = mailIngestionService.process(new InboundMail(
                "msg-review-3",
                "thread-review-3",
                "customer@example.com",
                "Need recommendation",
                "Please recommend a product for my living room.",
                OffsetDateTime.now()
        ));

        replyReviewLifecycleService.rejectSend(run.getRunId(), "auditor-c", "first draft too generic");
        var revisedDraft = replyDraftRevisionService.revise(
                run.getRunId(),
                "editor-a",
                "Re: Need recommendation",
                "Updated reply with clearer feature explanation.",
                "refined product explanation",
                true
        );

        assertThat(revisedDraft.getDraftVersion()).isEqualTo(2);
        assertThat(revisedDraft.getLastEditedBy()).isEqualTo("editor-a");
        assertThat(revisedDraft.getStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(revisedDraft.getSendReadiness()).isEqualTo(SendReadiness.PENDING_REVIEW);
        assertThat(revisedDraft.getNextAction()).isEqualTo("await_review_decision");

        var reviews = replyReviewLifecycleService.findReviews(run.getRunId());
        assertThat(reviews).hasSize(3);
        assertThat(reviews.get(0).getAction()).isEqualTo(ReviewAction.REJECT_SEND);
        assertThat(reviews.get(1).getAction()).isEqualTo(ReviewAction.REVISE_DRAFT);
        assertThat(reviews.get(2).getAction()).isEqualTo(ReviewAction.RESUBMIT_REVIEW);
    }
}

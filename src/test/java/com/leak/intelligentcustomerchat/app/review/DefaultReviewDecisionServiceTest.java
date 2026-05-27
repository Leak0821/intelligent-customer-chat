package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultReviewDecisionServiceTest {
    private final DefaultReviewDecisionService service = new DefaultReviewDecisionService(new ReviewFeedbackTagger());

    @Test
    void shouldKeepFollowUpDraftAsFollowUpNeeded() {
        ReplyDraft draft = ReplyDraft.create("run-1", "subject", "body", ReplyDraftStatus.FOLLOW_UP_NEEDED, "missing order id");

        var decision = service.review(draft, context(
                CustomerScene.AFTER_SALES,
                "order_status",
                BusinessFactStatus.INSUFFICIENT_INPUT,
                1
        ));

        assertThat(decision.finalStatus()).isEqualTo(ReplyDraftStatus.FOLLOW_UP_NEEDED);
    }

    @Test
    void shouldRequireManualReviewWhenBusinessFactsConflict() {
        ReplyDraft draft = ReplyDraft.create("run-2", "subject", "body", ReplyDraftStatus.DRAFT_READY, "ready");

        var decision = service.review(draft, context(
                CustomerScene.AFTER_SALES,
                "logistics_tracking",
                BusinessFactStatus.CONFLICT,
                1
        ));

        assertThat(decision.finalStatus()).isEqualTo(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(decision.reviewReason()).contains("business facts conflict");
        assertThat(decision.reviewSignals()).contains("fact_gap");
    }

    @Test
    void shouldRequireManualReviewWhenPreSalesHasNoKnowledgeSupport() {
        ReplyDraft draft = ReplyDraft.create("run-3", "subject", "body", ReplyDraftStatus.DRAFT_READY, "ready");

        var decision = service.review(draft, context(
                CustomerScene.PRE_SALES,
                "product_recommendation",
                BusinessFactStatus.NOT_REQUIRED,
                0
        ));

        assertThat(decision.finalStatus()).isEqualTo(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(decision.reviewReason()).contains("knowledge support");
        assertThat(decision.reviewSignals()).contains("knowledge_gap");
    }

    @Test
    void shouldKeepDraftReadyWhenSignalsLookSafe() {
        ReplyDraft draft = ReplyDraft.create("run-4", "subject", "body", ReplyDraftStatus.DRAFT_READY, "ready");

        var decision = service.review(draft, context(
                CustomerScene.AFTER_SALES,
                "order_status",
                BusinessFactStatus.SUCCESS,
                2
        ));

        assertThat(decision.finalStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(decision.reviewSignals()).isEmpty();
    }

    private ReviewDecisionContext context(CustomerScene scene,
                                          String subIntent,
                                          BusinessFactStatus businessFactStatus,
                                          int recallCount) {
        return new ReviewDecisionContext(
                new IntentRouteResult(scene, subIntent, ProcessingDisposition.CONTINUE, "test"),
                new BusinessFactResult(
                        businessFactStatus,
                        "test-source",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        OffsetDateTime.now()
                ),
                new KnowledgeRetrieveResult("test-knowledge", List.of(), recallCount)
        );
    }
}

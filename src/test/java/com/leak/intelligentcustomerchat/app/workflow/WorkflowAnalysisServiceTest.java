package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentRoutingService;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrieveService;
import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAnalysisServiceTest {

    @Test
    void shouldAssembleIntermediateArtifactsForDemoAnalysis() {
        InboundMail inboundMail = new InboundMail(
                "msg-1",
                "thread-1",
                "customer@example.com",
                "Need help",
                "Please check tracking number ZX987654",
                OffsetDateTime.now()
        );
        InboundMail cleanedMail = inboundMail.withRawBody("Please check tracking number ZX987654");
        IntentNormalizationResult normalizationResult = new IntentNormalizationResult(
                "Customer wants the tracking status.",
                "What is the tracking status?",
                List.of(),
                List.of(CustomerScene.AFTER_SALES),
                List.of("logistics_tracking"),
                List.of("order_id_or_tracking_no"),
                List.of(),
                ProcessingDisposition.CONTINUE
        );
        IntentRouteResult routeResult = new IntentRouteResult(CustomerScene.AFTER_SALES, "logistics_tracking", ProcessingDisposition.CONTINUE, "stub");
        ContextSnapshot contextSnapshot = new ContextSnapshot("customer asked again after previous email", List.of("tracking_number=ZX987654"), List.of());
        BusinessFactResult businessFactResult = new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                "stub-gateway",
                List.of("tracking_number=ZX987654"),
                List.of("current logistics status=in_transit"),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
        KnowledgeRetrieveResult knowledgeRetrieveResult = new KnowledgeRetrieveResult("stub-knowledge", List.of(), 0);
        ReplyDraft draft = ReplyDraft.create("preview-run", "Re: Need help", "Draft body", ReplyDraftStatus.DRAFT_READY, "notes");
        ReviewDecision reviewDecision = new ReviewDecision(ReplyDraftStatus.DRAFT_READY, false, "ready");

        WorkflowAnalysisService service = new WorkflowAnalysisService(
                mail -> cleanedMail,
                mail -> normalizationResult,
                normalization -> routeResult,
                (mail, route) -> contextSnapshot,
                (mail, normalization, route, context) -> businessFactResult,
                (normalization, route, facts) -> knowledgeRetrieveResult,
                (run, mail, normalization, route, context, facts, knowledge) -> {
                    assertThat(run.getMessageId()).isEqualTo("msg-1");
                    assertThat(run.getThreadId()).isEqualTo("thread-1");
                    return draft;
                },
                (replyDraft, context) -> {
                    assertThat(context.routeResult()).isEqualTo(routeResult);
                    assertThat(context.businessFactResult()).isEqualTo(businessFactResult);
                    assertThat(context.knowledgeRetrieveResult()).isEqualTo(knowledgeRetrieveResult);
                    return reviewDecision;
                }
        );

        WorkflowAnalysisView view = service.analyze(inboundMail);

        assertThat(view.cleanedMail()).isEqualTo(cleanedMail);
        assertThat(view.normalizationResult()).isEqualTo(normalizationResult);
        assertThat(view.routeResult()).isEqualTo(routeResult);
        assertThat(view.contextSnapshot()).isEqualTo(contextSnapshot);
        assertThat(view.businessFactResult()).isEqualTo(businessFactResult);
        assertThat(view.knowledgeRetrieveResult()).isEqualTo(knowledgeRetrieveResult);
        assertThat(view.draft()).isEqualTo(draft);
        assertThat(view.reviewDecision()).isEqualTo(reviewDecision);
    }
}

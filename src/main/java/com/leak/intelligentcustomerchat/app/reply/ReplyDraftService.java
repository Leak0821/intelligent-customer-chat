package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;

public interface ReplyDraftService {
    ReplyDraft draft(WorkflowRun run,
                     InboundMail mail,
                     IntentNormalizationResult normalizationResult,
                     IntentRouteResult routeResult,
                     ContextSnapshot contextSnapshot,
                     BusinessFactResult businessFactResult,
                     KnowledgeRetrieveResult knowledgeRetrieveResult);

    default ReplyDraftingResult draftWithDiagnostics(WorkflowRun run,
                                                     InboundMail mail,
                                                     IntentNormalizationResult normalizationResult,
                                                     IntentRouteResult routeResult,
                                                     ContextSnapshot contextSnapshot,
                                                     BusinessFactResult businessFactResult,
                                                     KnowledgeRetrieveResult knowledgeRetrieveResult) {
        ReplyDraft draft = draft(run, mail, normalizationResult, routeResult, contextSnapshot, businessFactResult, knowledgeRetrieveResult);
        return new ReplyDraftingResult(draft, ReplyDraftingDiagnostics.unknown(draft));
    }
}

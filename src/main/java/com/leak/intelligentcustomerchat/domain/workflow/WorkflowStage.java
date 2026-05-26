package com.leak.intelligentcustomerchat.domain.workflow;

public enum WorkflowStage {
    MAIL_RECEIVED,
    MAIL_CLEANED,
    INTENT_NORMALIZED,
    INTENT_ROUTED,
    CONTEXT_LOADED,
    BUSINESS_FACTS_READY,
    KNOWLEDGE_READY,
    REPLY_DRAFTED,
    REVIEWED,
    COMPLETED,
    BLOCKED
}

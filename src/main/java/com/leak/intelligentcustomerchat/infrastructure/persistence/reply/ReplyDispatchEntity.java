package com.leak.intelligentcustomerchat.infrastructure.persistence.reply;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("reply_dispatches")
public class ReplyDispatchEntity {
    @TableId("dispatch_id")
    private String dispatchId;

    @TableField("run_id")
    private String runId;

    @TableField("draft_id")
    private String draftId;

    @TableField("recipient")
    private String recipient;

    @TableField("subject")
    private String subject;

    @TableField("body_snapshot")
    private String bodySnapshot;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("max_attempts")
    private Integer maxAttempts;

    @TableField("status")
    private String status;

    @TableField("latest_trigger_source")
    private String latestTriggerSource;

    @TableField("latest_triggered_by")
    private String latestTriggeredBy;

    @TableField("latest_trigger_reason")
    private String latestTriggerReason;

    @TableField("provider_message_id")
    private String providerMessageId;

    @TableField("error_message")
    private String errorMessage;

    @TableField("last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodySnapshot() {
        return bodySnapshot;
    }

    public void setBodySnapshot(String bodySnapshot) {
        this.bodySnapshot = bodySnapshot;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLatestTriggerSource() {
        return latestTriggerSource;
    }

    public void setLatestTriggerSource(String latestTriggerSource) {
        this.latestTriggerSource = latestTriggerSource;
    }

    public String getLatestTriggeredBy() {
        return latestTriggeredBy;
    }

    public void setLatestTriggeredBy(String latestTriggeredBy) {
        this.latestTriggeredBy = latestTriggeredBy;
    }

    public String getLatestTriggerReason() {
        return latestTriggerReason;
    }

    public void setLatestTriggerReason(String latestTriggerReason) {
        this.latestTriggerReason = latestTriggerReason;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

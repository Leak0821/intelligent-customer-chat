package com.leak.intelligentcustomerchat.infrastructure.persistence.context;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("conversation_summaries")
public class ConversationSummaryEntity {
    @TableId("summary_id")
    private String summaryId;

    @TableField("thread_id")
    private String threadId;

    @TableField("summary_text")
    private String summaryText;

    @TableField("summary_source")
    private String summarySource;

    @TableField("covered_message_count")
    private Integer coveredMessageCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(String summaryId) {
        this.summaryId = summaryId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getSummarySource() {
        return summarySource;
    }

    public void setSummarySource(String summarySource) {
        this.summarySource = summarySource;
    }

    public Integer getCoveredMessageCount() {
        return coveredMessageCount;
    }

    public void setCoveredMessageCount(Integer coveredMessageCount) {
        this.coveredMessageCount = coveredMessageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

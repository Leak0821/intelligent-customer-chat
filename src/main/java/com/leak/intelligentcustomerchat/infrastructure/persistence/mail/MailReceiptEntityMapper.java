package com.leak.intelligentcustomerchat.infrastructure.persistence.mail;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class MailReceiptEntityMapper {
    private static final ZoneOffset STORAGE_ZONE_OFFSET = ZoneOffset.UTC;

    private MailReceiptEntityMapper() {
    }

    static MailReceiptEntity toEntity(MailReceipt receipt) {
        MailReceiptEntity entity = new MailReceiptEntity();
        entity.setReceiptId(receipt.getReceiptId());
        entity.setSourceKey(receipt.getSourceKey());
        entity.setFolderName(receipt.getFolderName());
        entity.setUid(receipt.getUid());
        entity.setMessageId(receipt.getMessageId());
        entity.setThreadId(receipt.getThreadId());
        entity.setSender(receipt.getSender());
        entity.setSubject(receipt.getSubject());
        entity.setReceivedAt(toLocalDateTime(receipt.getReceivedAt()));
        entity.setStatus(receipt.getStatus().name());
        entity.setWorkflowRunId(receipt.getWorkflowRunId());
        entity.setErrorMessage(receipt.getErrorMessage());
        entity.setCreatedAt(toLocalDateTime(receipt.getCreatedAt()));
        entity.setUpdatedAt(toLocalDateTime(receipt.getUpdatedAt()));
        return entity;
    }

    static MailReceipt toDomain(MailReceiptEntity entity) {
        return MailReceipt.restore(
                entity.getReceiptId(),
                entity.getSourceKey(),
                entity.getFolderName(),
                entity.getUid(),
                entity.getMessageId(),
                entity.getThreadId(),
                entity.getSender(),
                entity.getSubject(),
                toOffsetDateTime(entity.getReceivedAt()),
                MailReceiptStatus.valueOf(entity.getStatus()),
                entity.getWorkflowRunId(),
                entity.getErrorMessage(),
                toOffsetDateTime(entity.getCreatedAt()),
                toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value.withOffsetSameInstant(STORAGE_ZONE_OFFSET).toLocalDateTime();
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value.atOffset(STORAGE_ZONE_OFFSET);
    }
}

package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MailIngestionServiceTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private MailReceiptRepository mailReceiptRepository;

    @Test
    void shouldMarkReceiptAsProcessedAfterWorkflowStarts() {
        InboundMail mail = new InboundMail(
                "msg-receipt-1",
                "thread-receipt-1",
                "buyer@example.com",
                "Need logistics update",
                "Please help check my order status.",
                OffsetDateTime.now()
        );
        MailReceipt receipt = MailReceipt.fetched(
                UUID.randomUUID().toString(),
                "imap:test:buyer",
                "INBOX",
                1001L,
                mail
        );
        mailReceiptRepository.save(receipt);

        WorkflowRun run = mailIngestionService.process(mail);

        MailReceipt persisted = mailReceiptRepository.findByMessageId(mail.messageId()).orElseThrow();
        assertThat(run.getRunId()).isEqualTo(persisted.getWorkflowRunId());
        assertThat(persisted.getStatus()).isEqualTo(MailReceiptStatus.PROCESSED);
    }

    @Test
    void shouldQueueThenProcessReceiptAsSeparateSteps() {
        InboundMail mail = new InboundMail(
                "msg-receipt-2",
                "thread-receipt-2",
                "buyer@example.com",
                "Need logistics update",
                "Please help check my order status.",
                OffsetDateTime.now()
        );

        MailPollingResult enqueueResult = mailIngestionService.enqueue(new com.leak.intelligentcustomerchat.domain.mail.MailFetchResult(List.of(mail), List.of()));
        MailReceipt queued = mailReceiptRepository.findByMessageId(mail.messageId()).orElseThrow();

        assertThat(enqueueResult.queuedCount()).isEqualTo(1);
        assertThat(queued.getStatus()).isEqualTo(MailReceiptStatus.QUEUED);

        MailPollingResult processingResult = mailIngestionService.processPendingReceipts(10);
        MailReceipt processed = mailReceiptRepository.findByMessageId(mail.messageId()).orElseThrow();

        assertThat(processingResult.processedCount()).isEqualTo(1);
        assertThat(processed.getStatus()).isEqualTo(MailReceiptStatus.PROCESSED);
        assertThat(processed.getWorkflowRunId()).isNotBlank();
    }

    @Test
    void shouldManuallyEnqueueReceiptBeforeAsyncProcessing() {
        InboundMail mail = new InboundMail(
                "msg-receipt-3",
                "thread-receipt-3",
                "buyer@example.com",
                "Need async help",
                "Please enqueue this mail first.",
                OffsetDateTime.now()
        );

        MailReceipt queued = mailIngestionService.enqueueManual(mail);
        MailReceipt persisted = mailReceiptRepository.findByMessageId(mail.messageId()).orElseThrow();

        assertThat(queued.getStatus()).isEqualTo(MailReceiptStatus.QUEUED);
        assertThat(persisted.getStatus()).isEqualTo(MailReceiptStatus.QUEUED);

        MailPollingResult processingResult = mailIngestionService.processPendingReceipts(10);
        MailReceipt processed = mailReceiptRepository.findByMessageId(mail.messageId()).orElseThrow();

        assertThat(processingResult.processedCount()).isGreaterThanOrEqualTo(1);
        assertThat(processed.getStatus()).isEqualTo(MailReceiptStatus.PROCESSED);
    }
}

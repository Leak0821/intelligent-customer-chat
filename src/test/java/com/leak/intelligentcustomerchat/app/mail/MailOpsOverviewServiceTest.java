package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.config.MailProperties;
import com.leak.intelligentcustomerchat.config.XxlJobProperties;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailFetchResult;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.infrastructure.mail.MailSourceAdapter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MailOpsOverviewServiceTest {

    @Test
    void shouldReportLocalSchedulerModeAndReceiptStats() {
        InMemoryMailReceiptRepository repository = new InMemoryMailReceiptRepository();
        repository.save(receipt("receipt-1", "message-1", MailReceiptStatus.FETCHED));
        repository.save(receipt("receipt-2", "message-2", MailReceiptStatus.QUEUED));
        repository.save(receipt("receipt-3", "message-3", MailReceiptStatus.PROCESSED));
        repository.save(receipt("receipt-4", "message-4", MailReceiptStatus.FAILED));

        MailIngestionService mailIngestionService = new MailIngestionService(
                new StubMailSourceAdapter(),
                repository,
                new StubWorkflowRunService()
        );
        MailOpsOverviewService service = new MailOpsOverviewService(
                mailIngestionService,
                repository,
                new MailProperties(true, "imap", 20, "imap.example.com", 993, "user", "pwd", "INBOX", true, false, true, 60000L, 5000, 5000),
                new XxlJobProperties(false, "http://127.0.0.1:8088/xxl-job-admin", "", "icc-executor", "", "", 9999, "/tmp/xxl-job/jobhandler", 30)
        );

        MailOpsOverviewView overview = service.overview(3);

        assertThat(overview.schedulerMode()).isEqualTo("local-scheduler");
        assertThat(overview.mailEnabled()).isTrue();
        assertThat(overview.pollingEnabled()).isTrue();
        assertThat(overview.receiptStats().total()).isEqualTo(4);
        assertThat(overview.receiptStats().fetched()).isEqualTo(1);
        assertThat(overview.receiptStats().queued()).isEqualTo(1);
        assertThat(overview.receiptStats().processed()).isEqualTo(1);
        assertThat(overview.receiptStats().failed()).isEqualTo(1);
        assertThat(overview.recentReceipts()).hasSize(3);
    }

    @Test
    void shouldReportManualTriggerModeWhenPollingIsDisabled() {
        InMemoryMailReceiptRepository repository = new InMemoryMailReceiptRepository();
        MailIngestionService mailIngestionService = new MailIngestionService(
                new StubMailSourceAdapter(),
                repository,
                new StubWorkflowRunService()
        );
        MailOpsOverviewService service = new MailOpsOverviewService(
                mailIngestionService,
                repository,
                new MailProperties(true, "imap", 20, "imap.example.com", 993, "user", "pwd", "INBOX", true, false, false, 60000L, 5000, 5000),
                new XxlJobProperties(true, "http://127.0.0.1:8088/xxl-job-admin", "", "icc-executor", "", "", 9999, "/tmp/xxl-job/jobhandler", 30)
        );

        MailOpsOverviewView overview = service.overview(5);

        assertThat(overview.schedulerMode()).isEqualTo("manual-trigger");
    }

    private static MailReceipt receipt(String receiptId, String messageId, MailReceiptStatus status) {
        MailReceipt receipt = MailReceipt.manual(receiptId, new InboundMail(
                messageId,
                "thread-" + messageId,
                "customer@example.com",
                "subject-" + messageId,
                "body-" + messageId,
                OffsetDateTime.now()
        ));
        return switch (status) {
            case FETCHED -> receipt;
            case QUEUED -> {
                receipt.markQueued();
                yield receipt;
            }
            case PROCESSED -> {
                receipt.markProcessed("run-" + messageId);
                yield receipt;
            }
            case FAILED -> {
                receipt.markFailed("failed-" + messageId);
                yield receipt;
            }
        };
    }

    private static final class StubMailSourceAdapter implements MailSourceAdapter {
        @Override
        public MailFetchResult fetchNewMails() {
            return new MailFetchResult(List.of(), List.of());
        }
    }

    private static final class StubWorkflowRunService extends WorkflowRunService {
        private StubWorkflowRunService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public WorkflowRun start(InboundMail inboundMail) {
            return WorkflowRun.start(inboundMail.messageId(), inboundMail.threadId());
        }
    }

    private static final class InMemoryMailReceiptRepository implements MailReceiptRepository {
        private final Map<String, MailReceipt> receipts = new LinkedHashMap<>();

        @Override
        public boolean existsBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
            return receipts.values().stream()
                    .anyMatch(receipt -> receipt.getSourceKey().equals(sourceKey)
                            && receipt.getFolderName().equals(folderName)
                            && receipt.getUid() == uid);
        }

        @Override
        public MailReceipt save(MailReceipt receipt) {
            receipts.put(receipt.getReceiptId(), receipt);
            return receipt;
        }

        @Override
        public Optional<MailReceipt> findBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
            return receipts.values().stream()
                    .filter(receipt -> receipt.getSourceKey().equals(sourceKey)
                            && receipt.getFolderName().equals(folderName)
                            && receipt.getUid() == uid)
                    .findFirst();
        }

        @Override
        public Optional<MailReceipt> findByMessageId(String messageId) {
            return receipts.values().stream()
                    .filter(receipt -> receipt.getMessageId().equals(messageId))
                    .findFirst();
        }

        @Override
        public List<MailReceipt> findPendingForProcessing(int limit) {
            return receipts.values().stream()
                    .filter(receipt -> receipt.getStatus() == MailReceiptStatus.FETCHED
                            || receipt.getStatus() == MailReceiptStatus.QUEUED)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<MailReceipt> findRecent(int limit) {
            return receipts.values().stream()
                    .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countAll() {
            return receipts.size();
        }

        @Override
        public long countByStatus(MailReceiptStatus status) {
            return receipts.values().stream()
                    .filter(receipt -> receipt.getStatus() == status)
                    .count();
        }
    }
}

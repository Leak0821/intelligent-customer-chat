package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.mail.MailOpsOverviewService;
import com.leak.intelligentcustomerchat.app.mail.MailOpsOverviewView;
import com.leak.intelligentcustomerchat.app.mail.MailReceiptStatsView;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailAdminControllerTest {

    @Test
    void shouldExposeMailOverview() {
        MailIngestionService mailIngestionService = mock(MailIngestionService.class);
        MailOpsOverviewService mailOpsOverviewService = mock(MailOpsOverviewService.class);
        MailOpsOverviewView overview = new MailOpsOverviewView(
                true,
                "imap",
                true,
                "local-scheduler",
                20,
                60000L,
                new MailReceiptStatsView(3, 1, 1, 1, 0),
                List.of()
        );
        when(mailOpsOverviewService.overview(8)).thenReturn(overview);

        MailAdminController controller = new MailAdminController(mailIngestionService, mailOpsOverviewService);

        assertThat(controller.overview(8)).isEqualTo(overview);
    }

    @Test
    void shouldPollAndProcessThroughIngestionService() {
        MailIngestionService mailIngestionService = mock(MailIngestionService.class);
        MailOpsOverviewService mailOpsOverviewService = mock(MailOpsOverviewService.class);
        MailPollingResult result = new MailPollingResult(1, 1, 1, 0, List.of("run-1"), List.of());
        when(mailIngestionService.fetchAndProcess()).thenReturn(result);

        MailAdminController controller = new MailAdminController(mailIngestionService, mailOpsOverviewService);

        assertThat(controller.pollAndProcess()).isEqualTo(result);
    }

    @Test
    void shouldManuallyEnqueueMailThroughIngestionService() {
        MailIngestionService mailIngestionService = mock(MailIngestionService.class);
        MailOpsOverviewService mailOpsOverviewService = mock(MailOpsOverviewService.class);
        MailReceipt receipt = MailReceipt.restore(
                "receipt-1",
                "manual-ingestion",
                "manual",
                1L,
                "msg-1",
                "thread-1",
                "buyer@example.com",
                "Need help",
                "body",
                OffsetDateTime.now(),
                MailReceiptStatus.QUEUED,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(mailIngestionService.enqueueManual(any())).thenReturn(receipt);

        MailAdminController controller = new MailAdminController(mailIngestionService, mailOpsOverviewService);

        assertThat(controller.manualEnqueue(new MailAdminController.ManualMailRequest(
                "msg-1",
                "thread-1",
                "buyer@example.com",
                "Need help",
                "body"
        ))).isEqualTo(receipt);
    }
}

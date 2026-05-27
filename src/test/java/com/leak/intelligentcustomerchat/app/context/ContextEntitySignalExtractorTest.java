package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextEntitySignalExtractorTest {
    private final ContextEntitySignalExtractor extractor = new ContextEntitySignalExtractor();

    @Test
    void shouldExtractIdentifiersFromStructuredSignals() {
        ContextEntitySignals signals = extractor.extract(new ContextSnapshot(
                "customer asked for another update",
                List.of("order_id=EFGH5678", "tracking_number=TRACK5678"),
                List.of()
        ));

        assertThat(signals.orderId()).isEqualTo("EFGH5678");
        assertThat(signals.trackingNumber()).isEqualTo("TRACK5678");
        assertThat(signals.strongSignals()).containsExactly("order_id=EFGH5678", "tracking_number=TRACK5678");
    }

    @Test
    void shouldNotTreatPlainWordsAsReusableIdentifiers() {
        ContextEntitySignals signals = extractor.extract(
                "customer asked for another update",
                List.of("order", "tracking"),
                List.of("please check this again soon")
        );

        assertThat(signals.hasReusableIdentifier()).isFalse();
        assertThat(signals.strongSignals()).isEmpty();
    }
}

package com.leak.intelligentcustomerchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail.outbound")
public record MailOutboundProperties(
        boolean enabled,
        MailOutboundProvider provider,
        String fromAddress,
        String fromName,
        String host,
        int port,
        String username,
        String password,
        boolean authEnabled,
        boolean startTlsEnabled,
        boolean sslEnabled,
        int connectionTimeoutMillis,
        int timeoutMillis,
        int writeTimeoutMillis
) {
    public MailOutboundProperties {
        provider = provider == null ? MailOutboundProvider.NOOP : provider;
        fromAddress = normalize(fromAddress, "support@example.com");
        fromName = normalize(fromName, "intelligent-customer-chat");
        host = normalize(host, "");
        username = normalize(username, "");
        password = password == null ? "" : password;
        port = port <= 0 ? 25 : port;
        connectionTimeoutMillis = defaultIfNonPositive(connectionTimeoutMillis, 5000);
        timeoutMillis = defaultIfNonPositive(timeoutMillis, 5000);
        writeTimeoutMillis = defaultIfNonPositive(writeTimeoutMillis, 5000);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int defaultIfNonPositive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}

package com.leak.intelligentcustomerchat.infrastructure.mail;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Component
public class ImapMessageMapper {

    public InboundMail toInboundMail(Message message, long uid, String sourceKey, String folderName) throws MessagingException, IOException {
        String messageId = headerOrFallback(message, "Message-ID", sourceKey + ":" + folderName + ":" + uid);
        String threadId = resolveThreadId(message, messageId);
        String sender = resolveSender(message.getFrom());
        String subject = Objects.requireNonNullElse(message.getSubject(), "(no subject)");
        String rawBody = extractText(message).replaceAll("\\s+", " ").trim();
        OffsetDateTime receivedAt = resolveReceivedAt(message);
        return new InboundMail(messageId, threadId, sender, subject, rawBody, receivedAt);
    }

    private String resolveThreadId(Message message, String messageId) throws MessagingException {
        String inReplyTo = headerOrNull(message, "In-Reply-To");
        if (inReplyTo != null && !inReplyTo.isBlank()) {
            return inReplyTo;
        }
        String references = headerOrNull(message, "References");
        if (references != null && !references.isBlank()) {
            String[] parts = references.trim().split("\\s+");
            return parts[parts.length - 1];
        }
        return messageId;
    }

    private String resolveSender(Address[] from) {
        if (from == null || from.length == 0) {
            return "unknown";
        }
        Address first = from[0];
        if (first instanceof InternetAddress internetAddress) {
            return internetAddress.getAddress() == null ? internetAddress.toUnicodeString() : internetAddress.getAddress();
        }
        return first.toString();
    }

    private OffsetDateTime resolveReceivedAt(Message message) throws MessagingException {
        Date date = message.getReceivedDate() != null ? message.getReceivedDate() : message.getSentDate();
        if (date == null) {
            return OffsetDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    String extractText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        }
        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            return stripHtml(content == null ? "" : content.toString());
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String plainText = "";
            String htmlText = "";
            for (int index = 0; index < multipart.getCount(); index++) {
                String bodyText = extractText(multipart.getBodyPart(index));
                if (bodyText.isBlank()) {
                    continue;
                }
                if (multipart.getBodyPart(index).isMimeType("text/plain") && plainText.isBlank()) {
                    plainText = bodyText;
                } else if (htmlText.isBlank()) {
                    htmlText = bodyText;
                }
            }
            return plainText.isBlank() ? htmlText : plainText;
        }
        if (part.isMimeType("message/rfc822")) {
            return extractText((Part) part.getContent());
        }
        return "";
    }

    private String stripHtml(String html) {
        return html
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }

    private String headerOrFallback(Message message, String name, String fallback) throws MessagingException {
        String value = headerOrNull(message, name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String headerOrNull(Message message, String name) throws MessagingException {
        String[] values = message.getHeader(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}

package com.leak.intelligentcustomerchat.infrastructure.mail;

import com.leak.intelligentcustomerchat.config.MailProperties;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailFetchResult;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.search.FlagTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Component
public class ImapMailSourceAdapter implements MailSourceAdapter {
    private static final Logger log = LoggerFactory.getLogger(ImapMailSourceAdapter.class);

    private final MailProperties mailProperties;
    private final MailReceiptRepository mailReceiptRepository;
    private final ImapMessageMapper imapMessageMapper;

    public ImapMailSourceAdapter(MailProperties mailProperties,
                                 MailReceiptRepository mailReceiptRepository,
                                 ImapMessageMapper imapMessageMapper) {
        this.mailProperties = mailProperties;
        this.mailReceiptRepository = mailReceiptRepository;
        this.imapMessageMapper = imapMessageMapper;
    }

    @Override
    public MailFetchResult fetchNewMails() {
        if (!mailProperties.enabled()) {
            log.info("IMAP fetch skipped because app.mail.enabled=false");
            return MailFetchResult.empty();
        }
        if (mailProperties.host() == null || mailProperties.host().isBlank()
                || mailProperties.username() == null || mailProperties.username().isBlank()
                || mailProperties.password() == null || mailProperties.password().isBlank()) {
            return new MailFetchResult(List.of(), List.of("imap configuration incomplete"));
        }

        List<InboundMail> mails = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String sourceKey = buildSourceKey();
        String protocol = mailProperties.sslEnabled() ? "imaps" : "imap";

        Properties sessionProperties = new Properties();
        sessionProperties.put("mail.store.protocol", protocol);
        sessionProperties.put("mail." + protocol + ".connectiontimeout", String.valueOf(mailProperties.connectionTimeoutMillis()));
        sessionProperties.put("mail." + protocol + ".timeout", String.valueOf(mailProperties.readTimeoutMillis()));
        Session session = Session.getInstance(sessionProperties);

        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore(protocol);
            store.connect(mailProperties.host(), mailProperties.port(), mailProperties.username(), mailProperties.password());
            folder = store.getFolder(mailProperties.folder());
            if (folder == null || !folder.exists()) {
                return new MailFetchResult(List.of(), List.of("imap folder does not exist: " + mailProperties.folder()));
            }

            int openMode = mailProperties.markSeenAfterFetch() ? Folder.READ_WRITE : Folder.READ_ONLY;
            folder.open(openMode);
            if (!(folder instanceof UIDFolder uidFolder)) {
                return new MailFetchResult(List.of(), List.of("imap folder does not support UID operations"));
            }

            for (Message message : resolveCandidateMessages(folder)) {
                long uid = uidFolder.getUID(message);
                // 这里先用 source + folder + uid 做首版幂等键，后续如果要支持迁移账号或多文件夹归并，再补更稳的消息指纹。
                if (mailReceiptRepository.existsBySourceFolderAndUid(sourceKey, folder.getFullName(), uid)) {
                    continue;
                }
                try {
                    InboundMail inboundMail = imapMessageMapper.toInboundMail(message, uid, sourceKey, folder.getFullName());
                    MailReceipt receipt = MailReceipt.fetched(UUID.randomUUID().toString(), sourceKey, folder.getFullName(), uid, inboundMail);
                    mailReceiptRepository.save(receipt);
                    mails.add(inboundMail);

                    if (mailProperties.markSeenAfterFetch()) {
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                } catch (Exception ex) {
                    String error = "failed to map mail uid=" + uid + ": " + ex.getMessage();
                    errors.add(error);
                    log.warn(error, ex);
                }
            }
        } catch (Exception ex) {
            String error = "imap fetch failed: " + ex.getMessage();
            errors.add(error);
            log.error(error, ex);
        } finally {
            closeQuietly(folder);
            closeQuietly(store);
        }
        return new MailFetchResult(List.copyOf(mails), List.copyOf(errors));
    }

    private Message[] resolveCandidateMessages(Folder folder) throws Exception {
        Message[] unreadMessages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        if (unreadMessages != null && unreadMessages.length > 0) {
            return limitAndSort(unreadMessages);
        }

        int total = folder.getMessageCount();
        if (total <= 0) {
            return new Message[0];
        }
        int start = Math.max(1, total - mailProperties.pollSize() + 1);
        return limitAndSort(folder.getMessages(start, total));
    }

    private Message[] limitAndSort(Message[] messages) {
        return Arrays.stream(messages)
                .sorted(Comparator.comparingInt(Message::getMessageNumber))
                .limit(mailProperties.pollSize())
                .toArray(Message[]::new);
    }

    private String buildSourceKey() {
        return mailProperties.source() + ":" + mailProperties.host() + ":" + mailProperties.username();
    }

    private void closeQuietly(Folder folder) {
        if (folder == null) {
            return;
        }
        try {
            if (folder.isOpen()) {
                folder.close(false);
            }
        } catch (Exception ex) {
            log.debug("failed to close imap folder cleanly", ex);
        }
    }

    private void closeQuietly(Store store) {
        if (store == null) {
            return;
        }
        try {
            store.close();
        } catch (Exception ex) {
            log.debug("failed to close imap store cleanly", ex);
        }
    }
}

package com.leak.intelligentcustomerchat.domain.mail;

import java.util.List;
import java.util.Optional;

public interface MailReceiptRepository {
    boolean existsBySourceFolderAndUid(String sourceKey, String folderName, long uid);

    MailReceipt save(MailReceipt receipt);

    Optional<MailReceipt> findBySourceFolderAndUid(String sourceKey, String folderName, long uid);

    Optional<MailReceipt> findByMessageId(String messageId);

    List<MailReceipt> findPendingForProcessing(int limit);

    List<MailReceipt> findRecent(int limit);

    long countAll();

    long countByStatus(MailReceiptStatus status);
}

package com.leak.intelligentcustomerchat.infrastructure.persistence.mail;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisMailReceiptRepository implements MailReceiptRepository {
    private final MailReceiptMapper mailReceiptMapper;

    public MybatisMailReceiptRepository(MailReceiptMapper mailReceiptMapper) {
        this.mailReceiptMapper = mailReceiptMapper;
    }

    @Override
    public boolean existsBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
        LambdaQueryWrapper<MailReceiptEntity> query = new LambdaQueryWrapper<MailReceiptEntity>()
                .eq(MailReceiptEntity::getSourceKey, sourceKey)
                .eq(MailReceiptEntity::getFolderName, folderName)
                .eq(MailReceiptEntity::getUid, uid);
        return mailReceiptMapper.selectCount(query) > 0;
    }

    @Override
    public MailReceipt save(MailReceipt receipt) {
        MailReceiptEntity entity = MailReceiptEntityMapper.toEntity(receipt);
        if (mailReceiptMapper.selectById(receipt.getReceiptId()) == null) {
            mailReceiptMapper.insert(entity);
        } else {
            mailReceiptMapper.updateById(entity);
        }
        return receipt;
    }

    @Override
    public Optional<MailReceipt> findBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
        LambdaQueryWrapper<MailReceiptEntity> query = new LambdaQueryWrapper<MailReceiptEntity>()
                .eq(MailReceiptEntity::getSourceKey, sourceKey)
                .eq(MailReceiptEntity::getFolderName, folderName)
                .eq(MailReceiptEntity::getUid, uid)
                .last("limit 1");
        MailReceiptEntity entity = mailReceiptMapper.selectOne(query);
        return entity == null ? Optional.empty() : Optional.of(MailReceiptEntityMapper.toDomain(entity));
    }

    @Override
    public Optional<MailReceipt> findByMessageId(String messageId) {
        LambdaQueryWrapper<MailReceiptEntity> query = new LambdaQueryWrapper<MailReceiptEntity>()
                .eq(MailReceiptEntity::getMessageId, messageId)
                .orderByDesc(MailReceiptEntity::getCreatedAt)
                .last("limit 1");
        MailReceiptEntity entity = mailReceiptMapper.selectOne(query);
        return entity == null ? Optional.empty() : Optional.of(MailReceiptEntityMapper.toDomain(entity));
    }

    @Override
    public List<MailReceipt> findPendingForProcessing(int limit) {
        LambdaQueryWrapper<MailReceiptEntity> query = new LambdaQueryWrapper<MailReceiptEntity>()
                .in(MailReceiptEntity::getStatus, MailReceiptStatus.FETCHED.name(), MailReceiptStatus.QUEUED.name())
                .orderByAsc(MailReceiptEntity::getCreatedAt)
                .last("limit " + limit);
        return mailReceiptMapper.selectList(query).stream()
                .map(MailReceiptEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<MailReceipt> findRecent(int limit) {
        LambdaQueryWrapper<MailReceiptEntity> query = new LambdaQueryWrapper<MailReceiptEntity>()
                .orderByDesc(MailReceiptEntity::getCreatedAt)
                .last("limit " + limit);
        return mailReceiptMapper.selectList(query).stream()
                .map(MailReceiptEntityMapper::toDomain)
                .toList();
    }
}

package com.leak.intelligentcustomerchat.infrastructure.scheduler;

import com.leak.intelligentcustomerchat.config.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.scheduler.xxl", name = "enabled", havingValue = "true")
public class XxlJobExecutorConfig {

    @Bean(destroyMethod = "destroy")
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobProperties properties) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.adminAddresses());
        executor.setAccessToken(properties.accessToken());
        executor.setAppname(properties.executorAppName());
        executor.setAddress(properties.executorAddress());
        executor.setIp(properties.executorIp());
        executor.setPort(properties.executorPort());
        executor.setLogPath(properties.logPath());
        executor.setLogRetentionDays(properties.logRetentionDays());
        return executor;
    }
}

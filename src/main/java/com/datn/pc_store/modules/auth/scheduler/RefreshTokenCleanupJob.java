package com.datn.pc_store.modules.auth.scheduler;

import com.datn.pc_store.modules.auth.service.RefreshTokenService;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;
    private final SystemLogPublisher systemLogPublisher;

    @Scheduled(cron = "${app.cleanup.refresh-token-cron: 0 0 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void cleanup() {
        try {
            int deleted = refreshTokenService.deleteExpired();
            log.info("Đã dọn {} refresh token hết hạn!", deleted);
            if (systemLogPublisher != null) {
                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .username("SYSTEM")
                        .module(SystemLogModule.SYSTEM)
                        .action(SystemLogAction.SCHEDULED_JOB)
                        .status(SystemLogStatus.SUCCESS)
                        .resourceType("REFRESH_TOKEN")
                        .resourceCode("RefreshTokenCleanupJob")
                        .description("Chạy tác vụ định kỳ dọn dẹp refresh token hết hạn")
                        .detailMap(Map.of("deletedCount", deleted))
                        .build());
            }
        } catch (Exception e) {
            log.error("Dọn refresh token thất bại!", e);
            if (systemLogPublisher != null) {
                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .username("SYSTEM")
                        .module(SystemLogModule.SYSTEM)
                        .action(SystemLogAction.SCHEDULED_JOB)
                        .status(SystemLogStatus.FAILED)
                        .resourceType("REFRESH_TOKEN")
                        .resourceCode("RefreshTokenCleanupJob")
                        .description("Tác vụ định kỳ dọn dẹp refresh token thất bại: " + e.getMessage())
                        .build());
            }
        }
    }
}

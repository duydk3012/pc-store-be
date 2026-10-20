package com.datn.pc_store.modules.auth.scheduler;

import com.datn.pc_store.modules.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "${app.cleanup.refresh-token-cron: 0 0 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void cleanup() {
        try {
            int deleted = refreshTokenService.deleteExpired();
            log.info("Đã dọn {} refresh token hết hạn!", deleted);
        } catch (Exception e) {
            log.error("Dọn refresh token thất bại!", e);
        }
    }
}

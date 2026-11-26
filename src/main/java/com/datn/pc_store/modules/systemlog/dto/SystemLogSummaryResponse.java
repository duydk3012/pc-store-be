package com.datn.pc_store.modules.systemlog.dto;

import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogSummaryResponse {

    private long totalLogs;
    private long successCount;
    private long failedCount;
    private double successRate;
    private List<ModuleStatItem> moduleBreakdown;
    private List<ActionStatItem> actionBreakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModuleStatItem {
        private SystemLogModule module;
        private String moduleLabel;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionStatItem {
        private SystemLogAction action;
        private String actionLabel;
        private long count;
    }
}

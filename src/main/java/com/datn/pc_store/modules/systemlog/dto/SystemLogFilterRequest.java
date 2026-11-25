package com.datn.pc_store.modules.systemlog.dto;

import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.user.enums.RoleType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogFilterRequest {

    private String keyword;
    private SystemLogModule module;
    private SystemLogAction action;
    private SystemLogStatus status;
    private Long userId;
    private RoleType userRole;
    private String resourceType;
    private Long resourceId;
    private Instant fromDate;
    private Instant toDate;
}

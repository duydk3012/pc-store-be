package com.datn.pc_store.modules.systemlog.dto;

import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.user.enums.RoleType;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogCreateCommand {

    private Long userId;
    private String username;
    private RoleType userRole;

    private SystemLogModule module;
    private SystemLogAction action;

    @Builder.Default
    private SystemLogStatus status = SystemLogStatus.SUCCESS;

    private String resourceType;
    private Long resourceId;
    private String resourceCode;

    private String description;
    private String detailJson;
    private Map<String, Object> detailMap;

    private String ipAddress;
    private String userAgent;
    private String httpMethod;
    private String requestUri;
}

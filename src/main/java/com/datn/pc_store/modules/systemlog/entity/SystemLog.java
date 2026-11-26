package com.datn.pc_store.modules.systemlog.entity;

import com.datn.pc_store.modules.systemlog.enums.*;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.RoleTypeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Convert(converter = RoleTypeConverter.class)
    @Column(name = "user_role", columnDefinition = "TINYINT UNSIGNED")
    private RoleType userRole;

    @Convert(converter = SystemLogModuleConverter.class)
    @Column(name = "module", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private SystemLogModule module;

    @Convert(converter = SystemLogActionConverter.class)
    @Column(name = "action", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private SystemLogAction action;

    @Builder.Default
    @Convert(converter = SystemLogStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private SystemLogStatus status = SystemLogStatus.SUCCESS;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_code", length = 100)
    private String resourceCode;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_uri", length = 255)
    private String requestUri;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = Instant.now();
        }
        if (this.status == null) {
            this.status = SystemLogStatus.SUCCESS;
        }
    }
}

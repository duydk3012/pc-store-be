package com.datn.pc_store.modules.systemlog.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.common.util.PaginationUtils;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.dto.SystemLogFilterRequest;
import com.datn.pc_store.modules.systemlog.dto.SystemLogResponse;
import com.datn.pc_store.modules.systemlog.dto.SystemLogSummaryResponse;
import com.datn.pc_store.modules.systemlog.entity.SystemLog;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.datn.pc_store.modules.systemlog.repository.SystemLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogService {

    public static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "userId", "username", "userRole", "module", "action", "status",
            "resourceType", "resourceId", "resourceCode", "ipAddress",
            "httpMethod", "requestUri", "createdDate"
    );

    private final SystemLogRepository systemLogRepository;

    /**
     * Lưu bản ghi nhật ký trong một giao dịch độc lập (REQUIRES_NEW).
     * Bọc try-catch an toàn để lỗi ghi log không bao giờ làm hỏng giao dịch nghiệp vụ chính.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SystemLogResponse saveLog(SystemLogCreateCommand command) {
        if (command == null || command.getModule() == null || command.getAction() == null) {
            return null;
        }
        try {
            SystemLogPublisher.enrichContextAndMaskSensitiveData(command);

            String description = command.getDescription();
            if (description == null || description.isBlank()) {
                description = command.getAction().getLabel() + " - " + command.getModule().getLabel();
            }

            SystemLog entity = SystemLog.builder()
                    .userId(command.getUserId())
                    .username(truncate(command.getUsername(), 50))
                    .userRole(command.getUserRole())
                    .module(command.getModule())
                    .action(command.getAction())
                    .status(command.getStatus() != null ? command.getStatus() : SystemLogStatus.SUCCESS)
                    .resourceType(truncate(command.getResourceType(), 50))
                    .resourceId(command.getResourceId())
                    .resourceCode(truncate(command.getResourceCode(), 100))
                    .description(truncate(description, 500))
                    .detailJson(command.getDetailJson())
                    .ipAddress(truncate(command.getIpAddress(), 45))
                    .userAgent(truncate(command.getUserAgent(), 500))
                    .httpMethod(truncate(command.getHttpMethod(), 10))
                    .requestUri(truncate(command.getRequestUri(), 255))
                    .createdDate(Instant.now())
                    .build();

            SystemLog saved = systemLogRepository.save(entity);
            return toResponse(saved);
        } catch (Exception ex) {
            log.error("Lỗi khi lưu nhật ký hệ thống (module={}, action={}): {}",
                    command.getModule(), command.getAction(), ex.getMessage(), ex);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<SystemLogResponse> getLogs(SystemLogFilterRequest filter, Pageable pageable) {
        SystemLogFilterRequest effectiveFilter = filter != null ? filter : new SystemLogFilterRequest();
        PaginationUtils.validateDateRange(effectiveFilter.getFromDate(), effectiveFilter.getToDate());
        PaginationUtils.validateSort(pageable, SORTABLE_FIELDS);

        Specification<SystemLog> spec = buildSpecification(effectiveFilter);
        Page<SystemLog> page = systemLogRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SystemLogResponse getLogById(Long id) {
        SystemLog systemLog = systemLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhật ký hệ thống", "id", id));
        return toResponse(systemLog);
    }

    @Transactional(readOnly = true)
    public PageResponse<SystemLogResponse> getLogsByResource(String resourceType, Long resourceId, Pageable pageable) {
        PaginationUtils.validateSort(pageable, SORTABLE_FIELDS);
        String cleanType = resourceType != null ? resourceType.trim() : "";
        Page<SystemLog> page = systemLogRepository.findByResourceTypeIgnoreCaseAndResourceId(
                cleanType, resourceId, pageable
        );
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SystemLogSummaryResponse getSummary(Instant fromDate, Instant toDate) {
        PaginationUtils.validateDateRange(fromDate, toDate);

        List<Object[]> statusRows = systemLogRepository.countGroupedByStatus(fromDate, toDate);
        long successCount = 0L;
        long failedCount = 0L;

        if (statusRows != null) {
            for (Object[] row : statusRows) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    SystemLogStatus st = null;
                    if (row[0] instanceof SystemLogStatus s) {
                        st = s;
                    } else if (row[0] instanceof Number num) {
                        st = SystemLogStatus.fromValue(num.intValue());
                    }
                    long count = row[1] instanceof Number num ? num.longValue() : 0L;
                    if (st == SystemLogStatus.SUCCESS) {
                        successCount += count;
                    } else if (st == SystemLogStatus.FAILED) {
                        failedCount += count;
                    }
                }
            }
        }

        long totalLogs = successCount + failedCount;
        double successRate = totalLogs > 0
                ? Math.round((successCount * 10000.0) / totalLogs) / 100.0
                : 0.0;

        Map<SystemLogModule, Long> moduleCountMap = new EnumMap<>(SystemLogModule.class);
        List<Object[]> moduleRows = systemLogRepository.countGroupedByModule(fromDate, toDate);
        if (moduleRows != null) {
            for (Object[] row : moduleRows) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    SystemLogModule mod = null;
                    if (row[0] instanceof SystemLogModule m) {
                        mod = m;
                    } else if (row[0] instanceof Number num) {
                        mod = SystemLogModule.fromValue(num.intValue());
                    }
                    if (mod != null) {
                        long count = row[1] instanceof Number num ? num.longValue() : 0L;
                        moduleCountMap.put(mod, count);
                    }
                }
            }
        }

        List<SystemLogSummaryResponse.ModuleStatItem> moduleBreakdown = new ArrayList<>();
        for (SystemLogModule mod : SystemLogModule.values()) {
            moduleBreakdown.add(SystemLogSummaryResponse.ModuleStatItem.builder()
                    .module(mod)
                    .moduleLabel(mod.getLabel())
                    .count(moduleCountMap.getOrDefault(mod, 0L))
                    .build());
        }

        Map<SystemLogAction, Long> actionCountMap = new EnumMap<>(SystemLogAction.class);
        List<Object[]> actionRows = systemLogRepository.countGroupedByAction(fromDate, toDate);
        if (actionRows != null) {
            for (Object[] row : actionRows) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    SystemLogAction act = null;
                    if (row[0] instanceof SystemLogAction a) {
                        act = a;
                    } else if (row[0] instanceof Number num) {
                        act = SystemLogAction.fromValue(num.intValue());
                    }
                    if (act != null) {
                        long count = row[1] instanceof Number num ? num.longValue() : 0L;
                        actionCountMap.put(act, count);
                    }
                }
            }
        }

        List<SystemLogSummaryResponse.ActionStatItem> actionBreakdown = new ArrayList<>();
        for (SystemLogAction act : SystemLogAction.values()) {
            actionBreakdown.add(SystemLogSummaryResponse.ActionStatItem.builder()
                    .action(act)
                    .actionLabel(act.getLabel())
                    .count(actionCountMap.getOrDefault(act, 0L))
                    .build());
        }

        return SystemLogSummaryResponse.builder()
                .totalLogs(totalLogs)
                .successCount(successCount)
                .failedCount(failedCount)
                .successRate(successRate)
                .moduleBreakdown(moduleBreakdown)
                .actionBreakdown(actionBreakdown)
                .build();
    }

    private Specification<SystemLog> buildSpecification(SystemLogFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("resourceCode")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("ipAddress")), pattern),
                        cb.like(cb.lower(root.get("requestUri")), pattern)
                ));
            }
            if (filter.getModule() != null) {
                predicates.add(cb.equal(root.get("module"), filter.getModule()));
            }
            if (filter.getAction() != null) {
                predicates.add(cb.equal(root.get("action"), filter.getAction()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }
            if (filter.getUserRole() != null) {
                predicates.add(cb.equal(root.get("userRole"), filter.getUserRole()));
            }
            if (filter.getResourceType() != null && !filter.getResourceType().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("resourceType")),
                        filter.getResourceType().trim().toLowerCase()
                ));
            }
            if (filter.getResourceId() != null) {
                predicates.add(cb.equal(root.get("resourceId"), filter.getResourceId()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public SystemLogResponse toResponse(SystemLog entity) {
        if (entity == null) {
            return null;
        }
        return SystemLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .userRole(entity.getUserRole())
                .module(entity.getModule())
                .moduleLabel(entity.getModule() != null ? entity.getModule().getLabel() : null)
                .action(entity.getAction())
                .actionLabel(entity.getAction() != null ? entity.getAction().getLabel() : null)
                .status(entity.getStatus())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .resourceCode(entity.getResourceCode())
                .description(entity.getDescription())
                .detailJson(entity.getDetailJson())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .httpMethod(entity.getHttpMethod())
                .requestUri(entity.getRequestUri())
                .createdDate(entity.getCreatedDate())
                .build();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}

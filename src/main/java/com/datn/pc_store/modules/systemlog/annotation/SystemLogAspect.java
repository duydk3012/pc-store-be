package com.datn.pc_store.modules.systemlog.annotation;

import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Spring AOP Advisor & Interceptor tự động bắt các method gắn {@link LogActivity},
 * trích xuất thông tin kết quả trả về (resourceId, resourceCode) và phát sự kiện ghi log.
 */
@Slf4j
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@RequiredArgsConstructor
public class SystemLogAspect extends AbstractPointcutAdvisor {

    private final Pointcut pointcut = new AnnotationMatchingPointcut(null, LogActivity.class, true);
    private final MethodInterceptor advice = this::invoke;
    private final SystemLogPublisher systemLogPublisher;

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();

        try {
            Method method = invocation.getMethod();
            LogActivity logActivity = AnnotationUtils.findAnnotation(method, LogActivity.class);
            if (logActivity != null) {
                Long resourceId = extractLongProperty(result, "getId", "id");
                String resourceCode = extractStringProperty(result, "getCode", "code", "getRequestCode", "requestCode", "getUsername", "username");

                Object[] args = invocation.getArguments();
                if (resourceId == null && args != null) {
                    for (Object arg : args) {
                        if (arg instanceof Long longVal) {
                            resourceId = longVal;
                            break;
                        }
                        Long argId = extractLongProperty(arg, "getId", "id");
                        if (argId != null) {
                            resourceId = argId;
                            break;
                        }
                    }
                }
                if (resourceCode == null && args != null) {
                    for (Object arg : args) {
                        String argCode = extractStringProperty(arg, "getCode", "code", "getUsername", "username");
                        if (argCode != null && !argCode.isBlank()) {
                            resourceCode = argCode;
                            break;
                        }
                    }
                }

                String desc = logActivity.description();
                if (desc == null || desc.isBlank()) {
                    desc = logActivity.action().getLabel() + " " + logActivity.module().getLabel();
                }
                if (resourceCode != null && !desc.contains(resourceCode)) {
                    desc = desc + " (" + resourceCode + ")";
                }

                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .module(logActivity.module())
                        .action(logActivity.action())
                        .status(SystemLogStatus.SUCCESS)
                        .resourceType(logActivity.resourceType().isBlank() ? null : logActivity.resourceType())
                        .resourceId(resourceId)
                        .resourceCode(resourceCode)
                        .description(desc)
                        .build());
            }
        } catch (Exception ex) {
            log.warn("Không thể ghi nhận log tự động từ @LogActivity: {}", ex.getMessage());
        }

        return result;
    }

    private static Long extractLongProperty(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                Object val = m.invoke(target);
                if (val instanceof Number num) {
                    return num.longValue();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String extractStringProperty(Object target, String... methodNames) {
        if (target == null || target instanceof String || target instanceof Number || target instanceof Boolean) {
            return null;
        }
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                Object val = m.invoke(target);
                if (val instanceof String str && !str.isBlank()) {
                    return str;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

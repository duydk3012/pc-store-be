package com.datn.pc_store.modules.systemlog.annotation;

import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu trên method Service hoặc Controller để tự động ghi nhận nhật ký hệ thống
 * sau khi thao tác thực thi thành công.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogActivity {

    SystemLogModule module();

    SystemLogAction action();

    String resourceType() default "";

    String description() default "";
}

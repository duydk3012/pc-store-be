package com.datn.pc_store.modules.systemlog.event;

import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SystemLogEvent extends ApplicationEvent {

    private final SystemLogCreateCommand command;

    public SystemLogEvent(Object source, SystemLogCreateCommand command) {
        super(source);
        this.command = command;
    }
}

package com.datn.pc_store.modules.systemlog.event;

import com.datn.pc_store.modules.systemlog.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogEventListener {

    private final SystemLogService systemLogService;

    @EventListener
    public void onSystemLogEvent(SystemLogEvent event) {
        if (event == null || event.getCommand() == null) {
            return;
        }
        systemLogService.saveLog(event.getCommand());
    }
}

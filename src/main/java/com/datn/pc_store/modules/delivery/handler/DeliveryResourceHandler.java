package com.datn.pc_store.modules.delivery.handler;

import com.datn.pc_store.modules.delivery.enums.DeliveryResourceType;
import com.datn.pc_store.modules.delivery.enums.DeliveryStatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface DeliveryResourceHandler {
    DeliveryResourceType getResourceType();

    /** Locks the source document and puts it in its shipping state for this transaction. */
    void startDelivery(Long resourceId);

    String getResourceCode(Long resourceId);

    default Map<Long, String> getResourceCodes(Collection<Long> resourceIds) {
        Map<Long, String> result = new HashMap<>();
        if (resourceIds == null || resourceIds.isEmpty()) {
            return result;
        }
        for (Long id : resourceIds) {
            result.put(id, getResourceCode(id));
        }
        return result;
    }

    void onTerminalStatus(Long resourceId, DeliveryStatus status);
}

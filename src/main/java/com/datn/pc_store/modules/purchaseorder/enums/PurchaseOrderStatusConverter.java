package com.datn.pc_store.modules.purchaseorder.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PurchaseOrderStatusConverter implements AttributeConverter<PurchaseOrderStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(PurchaseOrderStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public PurchaseOrderStatus convertToEntityAttribute(Integer value) {
        return value == null ? null : PurchaseOrderStatus.fromValue(value);
    }
}

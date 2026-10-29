package com.datn.pc_store.modules.purchasereturn.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PurchaseReturnStatusConverter implements AttributeConverter<PurchaseReturnStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(PurchaseReturnStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PurchaseReturnStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : PurchaseReturnStatus.fromValue(dbData);
    }
}

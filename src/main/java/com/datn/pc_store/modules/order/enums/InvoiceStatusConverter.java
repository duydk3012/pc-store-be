package com.datn.pc_store.modules.order.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InvoiceStatusConverter implements AttributeConverter<InvoiceStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(InvoiceStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public InvoiceStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : InvoiceStatus.fromValue(dbData);
    }
}

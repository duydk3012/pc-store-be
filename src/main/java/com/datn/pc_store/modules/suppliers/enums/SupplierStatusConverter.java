package com.datn.pc_store.modules.suppliers.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SupplierStatusConverter implements AttributeConverter<SupplierStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SupplierStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public SupplierStatus convertToEntityAttribute(Integer dbData) {
        return dbData != null ? SupplierStatus.fromValue(dbData) : null;
    }
}

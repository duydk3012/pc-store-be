package com.datn.pc_store.modules.warranty.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WarrantyPartTypeConverter implements AttributeConverter<WarrantyPartType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(WarrantyPartType attribute) {
        return attribute != null ? (byte) attribute.getValue() : null;
    }

    @Override
    public WarrantyPartType convertToEntityAttribute(Byte dbData) {
        return dbData != null ? WarrantyPartType.fromValue(Byte.toUnsignedInt(dbData)) : null;
    }
}

package com.datn.pc_store.modules.systemlog.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SystemLogActionConverter implements AttributeConverter<SystemLogAction, Byte> {
    @Override
    public Byte convertToDatabaseColumn(SystemLogAction action) {
        return action == null ? null : (byte) action.getValue();
    }

    @Override
    public SystemLogAction convertToEntityAttribute(Byte value) {
        return value == null ? null : SystemLogAction.fromValue(Byte.toUnsignedInt(value));
    }
}

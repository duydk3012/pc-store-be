package com.datn.pc_store.modules.systemlog.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SystemLogStatusConverter implements AttributeConverter<SystemLogStatus, Byte> {
    @Override
    public Byte convertToDatabaseColumn(SystemLogStatus status) {
        return status == null ? null : (byte) status.getValue();
    }

    @Override
    public SystemLogStatus convertToEntityAttribute(Byte value) {
        return value == null ? null : SystemLogStatus.fromValue(Byte.toUnsignedInt(value));
    }
}

package com.datn.pc_store.modules.systemlog.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SystemLogModuleConverter implements AttributeConverter<SystemLogModule, Byte> {
    @Override
    public Byte convertToDatabaseColumn(SystemLogModule module) {
        return module == null ? null : (byte) module.getValue();
    }

    @Override
    public SystemLogModule convertToEntityAttribute(Byte value) {
        return value == null ? null : SystemLogModule.fromValue(Byte.toUnsignedInt(value));
    }
}

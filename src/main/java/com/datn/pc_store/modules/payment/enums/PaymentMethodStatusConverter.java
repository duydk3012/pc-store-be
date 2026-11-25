package com.datn.pc_store.modules.payment.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMethodStatusConverter implements AttributeConverter<PaymentMethodStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(PaymentMethodStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PaymentMethodStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : PaymentMethodStatus.fromValue(dbData);
    }
}

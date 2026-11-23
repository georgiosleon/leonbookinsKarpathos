package com.leonsoft.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class MoneyCentsConverter implements AttributeConverter<BigDecimal, Long> {

    @Override
    public Long convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) {
            return 0L;
        }
        return attribute.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(dbData, 2);
    }
}

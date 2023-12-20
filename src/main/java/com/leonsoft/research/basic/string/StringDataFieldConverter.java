package com.leonsoft.research.basic.string;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringDataFieldConverter implements AttributeConverter<StringDataField, String> {

    @Override
    public String convertToDatabaseColumn(StringDataField stringDataField) {
        if (stringDataField == null) {
            return null;
        }
        return stringDataField.getValue();
    }

    @Override
    public StringDataField convertToEntityAttribute(String dbString) {
        StringDataField stringDataField = new StringDataField();
        stringDataField.setValue( dbString );
        return stringDataField;
    }
}
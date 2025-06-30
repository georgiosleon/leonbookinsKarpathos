package com.leonsoft.research.basic.string;

import com.leonsoft.research.basic.AbstractDataField;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data


public class StringDataField extends AbstractDataField {

    private String value;

    @Override
    public Boolean valid() {
        return true;
    }

}

package com.leonsoft.research.basic.datetime;

import com.leonsoft.research.basic.AbstractDataField;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LocalDateDataField extends AbstractDataField {

    private LocalDate value;
    @Override
    public Boolean valid() {
        return true;
    }

}

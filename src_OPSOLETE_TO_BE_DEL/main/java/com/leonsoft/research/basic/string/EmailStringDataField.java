package com.leonsoft.research.basic.string;

import com.leonsoft.research.basic.string.StringDataField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailStringDataField extends StringDataField {

    private String value;

    @Override
    public Boolean valid() {
        String email = value;
        Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = pattern.matcher(email);
        return mat.matches();
    }

}

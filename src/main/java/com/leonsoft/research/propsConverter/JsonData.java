package com.leonsoft.research.propsConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonData {

    @JsonIgnore
    private String SYP_ID;
    @JsonProperty
    private String SYP_NAME_NM;
    @JsonProperty
    private String SYP_VALUE_NM;
    @JsonIgnore
    private String SYP_DESCRIPTION_DESC;

    @JsonIgnore
    private String SYP_TYPE_NM;

}

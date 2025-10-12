package com.leonsoft.research.basic;


import lombok.Data;

@Data
abstract public class AbstractDataField {

//    private Boolean required = true;
    private Boolean optional = true;
    private Boolean unique =  false;
    private Boolean nullable =  true;

    abstract public Boolean valid();

}

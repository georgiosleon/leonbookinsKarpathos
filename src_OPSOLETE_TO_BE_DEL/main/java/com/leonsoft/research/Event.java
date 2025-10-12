package com.leonsoft.research;

import com.leonsoft.research.basic.string.EmailStringDataField;
import com.leonsoft.research.basic.datetime.LocalDateDataField;
import com.leonsoft.research.basic.string.StringDataField;
import com.leonsoft.research.basic.string.StringDataFieldConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Event {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    @Convert(converter = StringDataFieldConverter.class)
    private StringDataField name  = new StringDataField();

    @Column(name = "notes")
    @Convert(converter = StringDataFieldConverter.class)
    private StringDataField notes  = new StringDataField();

//    @Column(name = "email")
//    private EmailStringDataField email   = new EmailStringDataField();

//    @Column(name = "start_date")
//    private LocalDateDataField startDate   = new LocalDateDataField();
//
//    @Column(name = "end_date")
//    private LocalDateDataField endDate   = new LocalDateDataField();


    //HashMap<String, Object>  data = new HashMap<>();

}

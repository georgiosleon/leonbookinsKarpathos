package com.leonsoft.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Data
@EqualsAndHashCode

public class BookingTotals {

    private Integer totNights = 0;
    private Double totCharge = 0.0;
    private Double totReceived = 0.0;
    private Double totCommission = 0.0;
    private Double totBalance = 0.0;  // balance =   charge  -  ( received + commission )
    private Integer totGuests = 0;

//    private String totCancelledStatus;
//    private String totActiveStatus;

//    private String totActiveStatus;
//    private String totActiveStatus;
//    private String totActiveStatus;

//    private String agency;
//    private String country;
//    private String room;

}

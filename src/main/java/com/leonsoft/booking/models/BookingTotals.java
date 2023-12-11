package com.leonsoft.booking.models;

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
    private Double totCharge =  Double.valueOf(0.0);
    private Double totReceived =  Double.valueOf(0.0);
    private Double totCommission =  Double.valueOf(0.0);
    private Double totBalance = Double.valueOf(0.0);   // balance =   charge  -  ( received + commission )

    private Integer totNights = Integer.valueOf(0);
    private Integer totGuests = Integer.valueOf(0);
}

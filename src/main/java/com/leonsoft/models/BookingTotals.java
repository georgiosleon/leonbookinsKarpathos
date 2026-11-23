package com.leonsoft.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingTotals {
    private Double totCharge =  Double.valueOf(0.0);
    private Double totReceived =  Double.valueOf(0.0);
    private Double totCommission =  Double.valueOf(0.0);
    private Double totRentalTax =  Double.valueOf(0.0);
    private Double totBalance = Double.valueOf(0.0);   // balance = charge - (received + commission + rentalTax)

    private Integer totNights = Integer.valueOf(0);
    private Integer totGuests = Integer.valueOf(0);
}

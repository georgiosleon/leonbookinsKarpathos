package com.leonsoft.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Entity
@Data
@EqualsAndHashCode

public class Booking {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "start_date")
    private String startDate; // "2023-03-06",   iso format  for saving  and EU for frontend  dd/MM/yyyy  06/03/2023

    @Column(name = "end_date")
    private String endDate; // iso format  for saving  and EU for frontend

    @Column(name = "num_of_nights")
    private Integer numOfNights;

    @Column(name = "charge")
    private Double charge;

    @Column(name = "received")
    private Double received;

    @Column(name = "commission")
    private Double commission;

    @Column(name = "balance")
    private Double balance;  // balance =   charge  -  ( received + commission )

    @Column(name = "name")
    private String name;

    @Column(name = "agency")
    private String agency;

    @Column(name = "room")
    private String room;

    @Column(name = "num_of_guests")
    private Integer numOfGuests;

    @Column(name = "country")
    private String country;

    @Column(name = "voucher")
    private String voucher;

    @Column(name = "identification")
    private String identification;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "tax_ref_number")
    private String taxRefNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "afm")
    private String afm;

    @Column(name = "tel")
    private String tel;

    @Column(name = "extra_info")
    private String extraInfo;

    @Column(name = "status")
    private String status;


}

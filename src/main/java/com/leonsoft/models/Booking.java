package com.leonsoft.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "user")
@NoArgsConstructor
@Entity
public class Booking {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Column(name = "num_of_nights")
    private Integer numOfNights;

    @Convert(converter = MoneyCentsConverter.class)
    @Column(name = "charge", columnDefinition = "integer")
    private java.math.BigDecimal charge;

    @Convert(converter = MoneyCentsConverter.class)
    @Column(name = "received", columnDefinition = "integer")
    private java.math.BigDecimal received;

    @Convert(converter = MoneyCentsConverter.class)
    @Column(name = "commission", columnDefinition = "integer")
    private java.math.BigDecimal commission;

    @Convert(converter = MoneyCentsConverter.class)
    @Column(name = "rental_tax", columnDefinition = "integer")
    private java.math.BigDecimal rentalTax;

    @Convert(converter = MoneyCentsConverter.class)
    @Column(name = "balance", columnDefinition = "integer")
    private java.math.BigDecimal balance;  // balance = charge - (received + commission + rentalTax)

    @Column(name = "name")
    private String name;

    @Column(name = "agency")
    private String agency;

    @Column(name = "resource_id")
    private String resourceId;

    @Transient
    private String resourceName;

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

    @Column(name = "status", nullable = false)
    private BookingStatus status;


    @Column(name = "request_DateTime")
    private LocalDateTime   requestDateTime;

}
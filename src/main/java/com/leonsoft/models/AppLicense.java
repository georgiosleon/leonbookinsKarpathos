package com.leonsoft.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_license")
@Getter
@Setter
@NoArgsConstructor
public class AppLicense {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "install_id", nullable = false)
    private String installId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "signed_license", nullable = false)
    private String signedLicense;

    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;

    @Column(name = "activated_by", nullable = false)
    private String activatedBy;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}

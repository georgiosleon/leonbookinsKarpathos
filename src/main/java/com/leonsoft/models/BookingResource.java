package com.leonsoft.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_resources")
@Getter
@Setter
@NoArgsConstructor
public class BookingResource {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "resource_type", nullable = false)
    private String type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public BookingResource(String id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.active = true;
    }
}

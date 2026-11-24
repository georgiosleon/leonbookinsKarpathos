package com.leonsoft.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_installation")
@Getter
@Setter
@NoArgsConstructor
public class AppInstallation {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "install_id", nullable = false, unique = true)
    private String installId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

package com.leonsoft.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "passwordHash")
public class User implements UserDetails {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "role", nullable = false)
    private String role; // canonical name: 'ADMIN', 'OPERATOR' or 'GUEST'

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Canonical role name ('ADMIN', 'OPERATOR' or 'GUEST').
     */
    public String getRole() {
        return Role.from(role).name();
    }

    public void setRole(String role) {
        this.role = Role.from(role).name();
    }

    /** True when the user's role is ADMIN. */
    public boolean isAdmin() {
        return Role.ADMIN.name().equals(getRole());
    }

    /** True when the user's role is OPERATOR. */
    public boolean isOperator() {
        return Role.OPERATOR.name().equals(getRole());
    }

    /** True when the user's role is GUEST. */
    public boolean isGuest() {
        return Role.GUEST.name().equals(getRole());
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_" + getRole());
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return active;
    }
}

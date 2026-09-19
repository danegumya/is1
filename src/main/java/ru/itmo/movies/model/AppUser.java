package ru.itmo.movies.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "ml_app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    public Integer id;

    @NotNull
    @Pattern(regexp = "[a-zA-Z0-9_]{3,40}")
    @Column(nullable = false, unique = true, length = 40)
    public String name;

    @NotNull
    @Column(nullable = false, length = 100)
    public String salt;

    @NotNull
    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
}

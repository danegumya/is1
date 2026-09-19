package ru.itmo.movies.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "ml_person")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    public Integer id;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    public String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "eye_color", nullable = false)
    public Color eyeColor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "hair_color", nullable = false)
    public Color hairColor;

    @ManyToOne
    public Location location;

    @Positive
    @DecimalMax("1.7976931348623157E308")
    public Double height;

    @Enumerated(EnumType.STRING)
    public Country nationality;
}

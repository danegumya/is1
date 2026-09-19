package ru.itmo.movies.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "ml_location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    public Integer id;

    @NotNull
    @DecimalMin("-3.4028235E38")
    @DecimalMax("3.4028235E38")
    @Column(nullable = false)
    public Float x;

    @NotNull
    @DecimalMin("-3.4028235E38")
    @DecimalMax("3.4028235E38")
    @Column(nullable = false)
    public Float y;

    @DecimalMin("-1.7976931348623157E308")
    @DecimalMax("1.7976931348623157E308")
    @Column(nullable = false)
    public double z;
}

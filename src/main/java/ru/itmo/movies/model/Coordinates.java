package ru.itmo.movies.model;

import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "ml_coordinates")
public class Coordinates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    public Integer id;

    @Min(-298)
    @Column(nullable = false)
    public int x;

    @NotNull
    @Max(40)
    @Column(nullable = false)
    @JsonbTypeAdapter(LongText.class)
    public Long y;
}

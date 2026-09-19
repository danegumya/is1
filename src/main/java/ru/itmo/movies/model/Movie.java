package ru.itmo.movies.model;

import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity
@Table(name = "ml_movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    public Integer id;

    @Version
    public long version;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    public String name;

    @NotNull
    @ManyToOne(optional = false)
    public Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false, insertable = false, updatable = false)
    public ZonedDateTime creationDate = ZonedDateTime.now(ZoneOffset.UTC);

    @Positive
    @Column(name = "oscars_count")
    @JsonbTypeAdapter(LongText.class)
    public Long oscarsCount;

    @Positive
    @Column(nullable = false)
    public int budget;

    @NotNull
    @Positive
    @Column(name = "total_box_office", nullable = false)
    public Integer totalBoxOffice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mpaa_rating", nullable = false)
    public MpaaRating mpaaRating;

    @ManyToOne
    public Person director;

    @ManyToOne
    public Person screenwriter;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    public Person operator;

    @NotNull
    @Positive
    @Column(nullable = false)
    @JsonbTypeAdapter(LongText.class)
    public Long length;

    @Positive
    @Column(name = "golden_palm_count")
    @JsonbTypeAdapter(LongText.class)
    public Long goldenPalmCount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MovieGenre genre;
}

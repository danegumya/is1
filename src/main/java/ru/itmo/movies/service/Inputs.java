package ru.itmo.movies.service;

import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.BadRequestException;
import java.util.stream.Collectors;
import ru.itmo.movies.model.*;

public class Inputs {

    public static void check(Validator validator, Object input) {
        if (input == null) {
            throw new BadRequestException("Нет данных запроса");
        }
        var errors = validator.validate(input);
        if (!errors.isEmpty()) {
            throw new BadRequestException(
                errors
                    .stream()
                    .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "))
            );
        }
    }

    public static class MovieData {

        @NotBlank
        public String name;

        @NotNull
        @Positive
        public Integer coordinates;

        @Positive
        @JsonbTypeAdapter(LongText.class)
        public Long oscarsCount;

        @NotNull
        @Positive
        public Integer budget;

        @NotNull
        @Positive
        public Integer totalBoxOffice;

        @NotNull
        public MpaaRating mpaaRating;

        @Positive
        public Integer director;

        @Positive
        public Integer screenwriter;

        @NotNull
        @Positive
        public Integer operator;

        @NotNull
        @Positive
        @JsonbTypeAdapter(LongText.class)
        public Long length;

        @Positive
        @JsonbTypeAdapter(LongText.class)
        public Long goldenPalmCount;

        @NotNull
        public MovieGenre genre;

        @PositiveOrZero
        public Long version;
    }

    public static class PersonData {

        @NotBlank
        public String name;

        @NotNull
        public Color eyeColor;

        @NotNull
        public Color hairColor;

        @Positive
        public Integer location;

        @Positive
        @DecimalMax("1.7976931348623157E308")
        public Double height;

        public Country nationality;
    }

    public static class CoordinatesData {

        @NotNull
        @Min(-298)
        public Integer x;

        @NotNull
        @Max(40)
        @JsonbTypeAdapter(LongText.class)
        public Long y;
    }

    public static class LocationData {

        @NotNull
        @DecimalMin("-3.4028235E38")
        @DecimalMax("3.4028235E38")
        public Float x;

        @NotNull
        @DecimalMin("-3.4028235E38")
        @DecimalMax("3.4028235E38")
        public Float y;

        @NotNull
        @DecimalMin("-1.7976931348623157E308")
        @DecimalMax("1.7976931348623157E308")
        public Double z;
    }

    public static class Credentials {

        @NotNull
        @Pattern(regexp = "[a-zA-Z0-9_]{3,40}")
        public String name;

        @NotNull
        @Size(min = 10, max = 128)
        public String password;
    }

    public static class Award {

        @NotNull
        @PositiveOrZero
        @JsonbTypeAdapter(LongText.class)
        public Long length;

        @NotNull
        @Positive
        @JsonbTypeAdapter(LongText.class)
        public Long amount;
    }
}

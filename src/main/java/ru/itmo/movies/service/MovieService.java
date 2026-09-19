package ru.itmo.movies.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import java.util.*;
import ru.itmo.movies.model.*;
import ru.itmo.movies.service.Inputs.*;

@ApplicationScoped
@Transactional
public class MovieService {

    @PersistenceContext
    EntityManager em;

    @Inject
    Validator validator;

    private static final Map<String, String> FIELDS = Map.of(
        "name",
        "m.name",
        "mpaaRating",
        "m.mpaaRating",
        "genre",
        "m.genre",
        "director",
        "d.name",
        "screenwriter",
        "s.name",
        "operator",
        "o.name"
    );

    public <T> T find(Class<T> type, Integer id) {
        if (id == null) {
            return null;
        }
        T value = em.find(type, id);
        if (value == null) {
            throw new NotFoundException("Связанный объект или фильм не найден: " + id);
        }
        return value;
    }

    public Map<String, Object> list(
        int page,
        int size,
        String field,
        String value,
        String sort,
        String order
    ) {
        if (page < 0 || page > 1000000 || size < 1 || size > 100) {
            throw new BadRequestException("Неверная страница");
        }
        if (
            !FIELDS.containsKey(field) ||
            !(sort.equals("id") || FIELDS.containsKey(sort)) ||
            !(order.equals("asc") || order.equals("desc"))
        ) {
            throw new BadRequestException("Неверное поле сортировки");
        }
        String from =
            " from Movie m left join m.director d left join m.screenwriter s join m.operator o";
        Object param = value;
        if (value != null) {
            if (field.equals("genre")) {
                param = MovieGenre.valueOf(value);
            }
            if (field.equals("mpaaRating")) {
                param = MpaaRating.valueOf(value);
            }
            from += " where " + FIELDS.get(field) + " = :value";
        }
        var query = em.createQuery(
            "select m" +
                from +
                " order by " +
                (sort.equals("id") ? "m.id" : FIELDS.get(sort)) +
                " " +
                order +
                ", m.id",
            Movie.class
        );
        var count = em.createQuery("select count(m)" + from, Long.class);
        if (value != null) {
            query.setParameter("value", param);
            count.setParameter("value", param);
        }
        return Map.of(
            "items",
            query
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList(),
            "total",
            count.getSingleResult()
        );
    }

    public Movie save(Integer id, MovieData input) {
        Inputs.check(validator, input);
        Movie movie = id == null ? new Movie() : find(Movie.class, id);
        if (id != null && (input.version == null || movie.version != input.version)) {
            throw new ClientErrorException("Фильм уже изменён. Откройте форму заново", 409);
        }
        movie.name = input.name.trim();
        movie.coordinates = find(Coordinates.class, input.coordinates);
        movie.oscarsCount = input.oscarsCount;
        movie.budget = input.budget;
        movie.totalBoxOffice = input.totalBoxOffice;
        movie.mpaaRating = input.mpaaRating;
        movie.director = find(Person.class, input.director);
        movie.screenwriter = find(Person.class, input.screenwriter);
        movie.operator = find(Person.class, input.operator);
        movie.length = input.length;
        movie.goldenPalmCount = input.goldenPalmCount;
        movie.genre = input.genre;
        if (id == null) {
            em.persist(movie);
        }
        em.flush();
        em.refresh(movie);
        return movie;
    }

    public void delete(int id, long version) {
        Movie movie = find(Movie.class, id);
        if (movie.version != version) {
            throw new ClientErrorException("Фильм уже изменён. Обновите список", 409);
        }
        int coords = movie.coordinates.id;
        Set<Integer> people = new HashSet<>();
        for (Person person : new Person[] { movie.director, movie.screenwriter, movie.operator }) {
            if (person != null) {
                people.add(person.id);
            }
        }
        em.remove(movie);
        em.flush();
        em.createNativeQuery(
            "delete from s468127.ml_coordinates c where c.id = :id and not exists " +
                "(select 1 from s468127.ml_movie m where m.coordinates_id = c.id)"
        )
            .setParameter("id", coords)
            .executeUpdate();
        for (int person : people) {
            Person value = find(Person.class, person);
            Integer loc = value.location == null ? null : value.location.id;
            em.createNativeQuery(
                "delete from s468127.ml_person p where p.id = :id and not exists " +
                    "(select 1 from s468127.ml_movie m where m.director_id = p.id or m.screenwriter_id = p.id or m.operator_id = p.id)"
            )
                .setParameter("id", person)
                .executeUpdate();
            if (loc != null) {
                em.createNativeQuery(
                    "delete from s468127.ml_location l where l.id = :id and not exists " +
                        "(select 1 from s468127.ml_person p where p.location_id = l.id)"
                )
                    .setParameter("id", loc)
                    .executeUpdate();
            }
        }
        em.clear();
    }

    public Map<String, Object> references() {
        return Map.of(
            "coordinates",
            em
                .createQuery("select v from Coordinates v order by v.id", Coordinates.class)
                .getResultList(),
            "people",
            em.createQuery("select v from Person v order by v.id", Person.class).getResultList(),
            "locations",
            em.createQuery("select v from Location v order by v.id", Location.class).getResultList()
        );
    }

    public Person person(PersonData input) {
        Inputs.check(validator, input);
        Person value = new Person();
        value.name = input.name.trim();
        value.eyeColor = input.eyeColor;
        value.hairColor = input.hairColor;
        value.location = find(Location.class, input.location);
        value.height = input.height;
        value.nationality = input.nationality;
        em.persist(value);
        return value;
    }

    public Coordinates coordinates(CoordinatesData input) {
        Inputs.check(validator, input);
        Coordinates value = new Coordinates();
        value.x = input.x;
        value.y = input.y;
        em.persist(value);
        return value;
    }

    public Location location(LocationData input) {
        Inputs.check(validator, input);
        Location value = new Location();
        value.x = input.x;
        value.y = input.y;
        value.z = input.z;
        em.persist(value);
        return value;
    }

    public Object special(String name, String genre) {
        return switch (name) {
            case "average" -> Collections.singletonMap(
                "average",
                em.createNativeQuery("select s468127.ml_average_palms()").getSingleResult()
            );
            case "minimum" -> em.createNativeQuery(
                "select * from s468127.ml_minimum_movie()",
                Movie.class
            ).getResultList();
            case "without-oscars" -> em.createNativeQuery(
                "select * from s468127.ml_without_oscars()",
                Movie.class
            ).getResultList();
            case "genres" -> em.createNativeQuery(
                "select * from s468127.ml_genres_before(:genre)",
                Movie.class
            )
                .setParameter("genre", MovieGenre.valueOf(genre).name())
                .getResultList();
            default -> throw new NotFoundException();
        };
    }

    public Object award(Award input) {
        Inputs.check(validator, input);
        return em
            .createNativeQuery("select s468127.ml_award_oscars(:length, :amount)")
            .setParameter("length", input.length)
            .setParameter("amount", input.amount)
            .getSingleResult();
    }
}

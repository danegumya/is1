package ru.itmo.movies.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import ru.itmo.movies.model.Movie;
import ru.itmo.movies.service.Inputs.*;
import ru.itmo.movies.service.MovieService;

@Path("/")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MoviesResource {

    @Inject
    MovieService service;

    @GET
    @Path("movies")
    public Object list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("10") int size,
        @QueryParam("field") @DefaultValue("name") String field,
        @QueryParam("value") String value,
        @QueryParam("sort") @DefaultValue("id") String sort,
        @QueryParam("order") @DefaultValue("asc") String order
    ) {
        return service.list(page, size, field, value, sort, order);
    }

    @GET
    @Path("movies/{id}")
    public Movie get(@PathParam("id") int id) {
        return service.find(Movie.class, id);
    }

    @POST
    @Path("movies")
    public Movie create(MovieData input) {
        return service.save(null, input);
    }

    @PUT
    @Path("movies/{id}")
    public Movie update(@PathParam("id") int id, MovieData input) {
        return service.save(id, input);
    }

    @DELETE
    @Path("movies/{id}")
    public void delete(@PathParam("id") int id, @QueryParam("version") Long version) {
        if (version == null || version < 0) {
            throw new BadRequestException("Укажите версию фильма");
        }
        service.delete(id, version);
    }

    @GET
    @Path("references")
    public Object references() {
        return service.references();
    }

    @POST
    @Path("people")
    public Object person(PersonData input) {
        return service.person(input);
    }

    @POST
    @Path("coordinates")
    public Object coordinates(CoordinatesData input) {
        return service.coordinates(input);
    }

    @POST
    @Path("locations")
    public Object location(LocationData input) {
        return service.location(input);
    }

    @GET
    @Path("special/{name}")
    public Object special(@PathParam("name") String name, @QueryParam("genre") String genre) {
        return service.special(name, genre);
    }

    @POST
    @Path("special/award")
    public Object award(Award input) {
        return Map.of("updated", service.award(input));
    }
}

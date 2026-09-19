package ru.itmo.movies.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
import ru.itmo.movies.service.AuthService;
import ru.itmo.movies.service.Inputs.Credentials;

@Path("session")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SessionResource {

    @Context
    HttpServletRequest request;

    @Inject
    AuthService auth;

    @GET
    public Object state() {
        var session = request.getSession();
        if (session.getAttribute("csrf") == null) {
            session.setAttribute("csrf", UUID.randomUUID().toString());
        }
        return Map.of(
            "csrf",
            session.getAttribute("csrf"),
            "user",
            session.getAttribute("user") == null ? "" : session.getAttribute("user")
        );
    }

    private Object enter(Credentials input, boolean register) {
        String name = auth.authenticate(input, register);
        request.getSession().invalidate();
        request.getSession(true).setAttribute("user", name);
        return state();
    }

    @POST
    @Path("login")
    public Object login(Credentials input) {
        return enter(input, false);
    }

    @POST
    @Path("register")
    public Object register(Credentials input) {
        return enter(input, true);
    }

    @DELETE
    public void logout() {
        request.getSession().invalidate();
    }
}

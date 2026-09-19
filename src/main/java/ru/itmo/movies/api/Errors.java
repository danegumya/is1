package ru.itmo.movies.api;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Provider
public class Errors implements ExceptionMapper<Exception> {

    public Response toResponse(Exception error) {
        int status = 500;
        String message = "Не удалось выполнить запрос";
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException invalid) {
                status = 400;
                message = invalid
                    .getConstraintViolations()
                    .stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
                break;
            }
            if (cause instanceof OptimisticLockException) {
                status = 409;
                message = "Объект уже изменён. Откройте его заново";
                break;
            }
            if (cause instanceof SQLException sql) {
                String state = sql.getSQLState();
                if ("23505".equals(state)) {
                    status = 409;
                    message = "Такая запись уже существует";
                } else if ("23503".equals(state)) {
                    status = 409;
                    message = "Связи изменились. Обновите страницу";
                } else if (state != null && (state.startsWith("22") || state.startsWith("23"))) {
                    status = 400;
                    message = "Значение нарушает ограничения БД или выходит за допустимый диапазон";
                }
                break;
            }
            if (cause instanceof WebApplicationException web) {
                status = web.getResponse().getStatus();
                message = web.getMessage();
                break;
            }
            if (cause instanceof IllegalArgumentException) {
                status = 400;
                message = "Неверное значение поля";
                break;
            }
        }
        if (status == 500) {
            Logger.getLogger(Errors.class.getName()).log(Level.SEVERE, "Request failed", error);
        }
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of("error", message == null ? "Ошибка запроса" : message))
            .build();
    }
}

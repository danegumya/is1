package ru.itmo.movies.api;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@WebFilter("/*")
public class SecurityFilter implements Filter {

    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    private synchronized boolean allowed(String address) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> now - entry.getValue()[0] > 60000);
        long[] state = attempts.computeIfAbsent(address, key -> new long[] { now, 0 });
        return ++state[1] <= 20;
    }

    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
        throws IOException, ServletException {
        var request = (HttpServletRequest) input;
        var response = (HttpServletResponse) output;
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"
        );
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Cache-Control", "no-store");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/")) {
            var session = request.getSession(false);
            if (
                !path.startsWith("/api/session") &&
                (session == null || session.getAttribute("user") == null)
            ) {
                fail(response, 401, "Войдите в систему");
                return;
            }
            if (!request.getMethod().equals("GET")) {
                if (
                    session == null ||
                    session.getAttribute("csrf") == null ||
                    !session.getAttribute("csrf").equals(request.getHeader("X-CSRF-Token"))
                ) {
                    fail(response, 403, "Обновите страницу и повторите запрос");
                    return;
                }
                if (request.getContentLengthLong() > 65536) {
                    fail(response, 413, "Слишком большой запрос");
                    return;
                }
                if (
                    request.getMethod().equals("POST") &&
                    path.startsWith("/api/session/") &&
                    !allowed(request.getRemoteAddr())
                ) {
                    fail(response, 429, "Слишком много попыток входа. Подождите минуту");
                    return;
                }
            }
        }
        chain.doFilter(input, output);
    }

    private void fail(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}

package ru.itmo.movies.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import ru.itmo.movies.model.AppUser;
import ru.itmo.movies.service.Inputs.Credentials;

@ApplicationScoped
@Transactional
public class AuthService {

    @PersistenceContext
    EntityManager em;

    @Inject
    Validator validator;

    private byte[] hash(String password, byte[] salt) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, 210000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException(error);
        } finally {
            spec.clearPassword();
        }
    }

    public String authenticate(Credentials input, boolean register) {
        Inputs.check(validator, input);
        var users = em
            .createQuery("select u from AppUser u where u.name = :name", AppUser.class)
            .setParameter("name", input.name)
            .getResultList();
        if (register) {
            if (!users.isEmpty()) {
                throw new ClientErrorException("Имя уже занято", 409);
            }
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            AppUser user = new AppUser();
            user.name = input.name;
            user.salt = Base64.getEncoder().encodeToString(salt);
            user.passwordHash = Base64.getEncoder().encodeToString(hash(input.password, salt));
            em.persist(user);
            em.flush();
        } else {
            byte[] salt = users.isEmpty()
                ? new byte[16]
                : Base64.getDecoder().decode(users.get(0).salt);
            byte[] actual = hash(input.password, salt);
            if (
                users.isEmpty() ||
                !MessageDigest.isEqual(
                    actual,
                    Base64.getDecoder().decode(users.get(0).passwordHash)
                )
            ) {
                throw new ClientErrorException(
                    "Неверное имя или пароль. Если у вас ещё нет аккаунта, нажмите «Зарегистрироваться».",
                    401
                );
            }
        }
        return input.name;
    }
}

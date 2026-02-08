package ru.netology.homework_61.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;
import ru.netology.homework_61.model.FileData;
import ru.netology.homework_61.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public class CloudServiceRepository {
    private final EntityManager db;

    public CloudServiceRepository(EntityManager db) {
        this.db = db;
    }

    @Transactional
    public long addUser(User user) {
        db.persist(user);

        return user.getId();
    }

    @Transactional
    public Optional<User> findUserByLogin(String login) {
        var query = db.createQuery("select user from User user where user.login = :login", User.class);

        query.setParameter("login", login);

        try {
            var queryRes = query.getSingleResult();

            return Optional.of(queryRes);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void saveUserSession(String accessToken, User user) {
        db.persist(new UserSession(user, accessToken));
    }

    @Transactional
    public void deleteUserSession(String accessToken) {
        var query = db.createQuery("delete UserSession session where session.accessToken = :token");

        query.setParameter("token", accessToken);

        query.executeUpdate();
    }

    @Transactional
    public Optional<User> findUserIdByAccessToken(String token) {
        if (token == null) {
            return Optional.empty();
        }

        var query = db.createQuery(
                """
                        select user
                        from User user
                            join UserSession session
                            on user = session.user
                        where session.accessToken = :token
                        """,
                User.class
        );

        query.setParameter("token", token);

        try {
            var queryRes = query.getSingleResult();

            return Optional.of(queryRes);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void saveFileData(FileData fileData) {
        db.persist(fileData);
    }

    @Transactional
    public boolean deleteFileData(User user, String fileName) {
        var query = db.createQuery("delete FileData fd where fd.user = :user and fd.name = :fileName");

        query.setParameter("user", user);
        query.setParameter("fileName", fileName);

        return query.executeUpdate() > 0;
    }

    @Transactional
    public Optional<FileData> getFileData(User user, String fileName) {
        var query = db.createQuery("select fd from FileData fd where fd.user = :user and fd.name = :name", FileData.class);

        query.setParameter("user", user);
        query.setParameter("name", fileName);

        try {
            var queryRes = query.getSingleResult();

            return Optional.of(queryRes);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public List<FileData> listFilesByUser(User user, int limit) {
        var query = db.createQuery("select fd from FileData fd where fd.user = :user order by fd.name", FileData.class);

        query.setParameter("user", user);

        query.setMaxResults(limit);

        return query.getResultList();
    }
}

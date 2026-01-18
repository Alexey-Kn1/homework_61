package ru.netology.homework_61.repository;

import jakarta.transaction.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.netology.homework_61.model.FileData;
import ru.netology.homework_61.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public class CloudServiceRepository {
    private final NamedParameterJdbcTemplate db;

    public CloudServiceRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public long addUser(User user) {
        var kh = new GeneratedKeyHolder();

        db.update(
                """
                        insert into users (login, password_hash)
                        values (:login, :password_hash)
                        returning id
                        """,
                new MapSqlParameterSource()
                        .addValue("login", user.getLogin())
                        .addValue("password_hash", user.getPasswordHash()),
                kh
        );

        return kh.getKey().longValue();
    }

    public Optional<User> findUserByLogin(String login) {
        try {
            var res = db.queryForObject(
                    """
                            select id, login, password_hash
                            from users
                            where
                                users.login = :login
                            """,
                    new MapSqlParameterSource()
                            .addValue("login", login),
                    (rs, rowNum) -> new User(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("password_hash")
                    )
            );

            return Optional.of(res);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveUserSession(String accessToken, long userId) {
        db.update(
                """
                        insert into user_sessions (user_id, access_token)
                        values (:user_id, :access_token)
                        """,
                new MapSqlParameterSource()
                        .addValue("user_id", userId)
                        .addValue("access_token", accessToken)
        );
    }

    public void deleteUserSession(String accessToken) {
        db.update(
                """
                        delete from user_sessions
                        where access_token = :access_token
                        """,
                new MapSqlParameterSource()
                        .addValue("access_token", accessToken)
        );
    }

    public Optional<User> findUserIdByAccessToken(String token) {
        if (token == null) {
            return Optional.empty();
        }

        try {
            var res = db.queryForObject(
                    """
                            select users.id as id, users.login as login, users.password_hash as password_hash
                            from user_sessions
                                join users on user_sessions.user_id = users.id
                            where
                                user_sessions.access_token = :access_token
                            """,
                    new MapSqlParameterSource()
                            .addValue("access_token", token),
                    (rs, rowNum) -> new User(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("password_hash")
                    )
            );

            return Optional.of(res);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveFileData(FileData fileData) {
        if (fileData.getId() > 0) {
            db.update(
                    """
                            update files_data
                            set user_id = :user_id, name = :name, local_name = :local_name, checksum = :checksum, size = :size
                            where id = :id
                            """,
                    new MapSqlParameterSource()
                            .addValue("user_id", fileData.getUserId())
                            .addValue("name", fileData.getName())
                            .addValue("local_name", fileData.getLocalName())
                            .addValue("checksum", fileData.getChecksum())
                            .addValue("size", fileData.getSize())
                            .addValue("id", fileData.getId())
            );
        } else {
            var kh = new GeneratedKeyHolder();

            db.update(
                    """
                            insert into files_data (user_id, name, local_name, checksum, size)
                            values (:user_id, :name, :local_name, :checksum, :size)
                            returning id
                            """,
                    new MapSqlParameterSource()
                            .addValue("user_id", fileData.getUserId())
                            .addValue("name", fileData.getName())
                            .addValue("local_name", fileData.getLocalName())
                            .addValue("checksum", fileData.getChecksum())
                            .addValue("size", fileData.getSize()),
                    kh
            );

            fileData.setId(kh.getKey().longValue());
        }
    }

    public boolean deleteFileData(long userId, String fileName) {
        var rowsAffected = db.update(
                """
                        delete from files_data
                        where user_id = :user_id and name = :name
                        """,
                new MapSqlParameterSource()
                        .addValue("user_id", userId)
                        .addValue("name", fileName)
        );

        return rowsAffected > 0;
    }

    public Optional<FileData> getFileData(long userId, String fileName) {
        try {
            var res = db.queryForObject(
                    """
                            select id, user_id, name, local_name, checksum, size
                            from files_data
                            where user_id = :user_id and name = :file_name
                            """,
                    new MapSqlParameterSource()
                            .addValue("user_id", userId)
                            .addValue("file_name", fileName),
                    (rs, rowNum) -> new FileData(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getString("name"),
                            rs.getString("local_name"),
                            rs.getString("checksum"),
                            rs.getLong("size")
                    )
            );

            return Optional.of(res);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<FileData> listFilesByUser(long userId, int limit) {
        return db.query(
                """
                        select user_id, name, local_name, checksum, size
                        from files_data
                        where user_id = :user_id
                        limit :limit
                        """,
                new MapSqlParameterSource()
                        .addValue("user_id", userId)
                        .addValue("limit", limit),
                (rs, rowNum) -> new FileData(
                        rs.getLong("user_id"),
                        rs.getString("name"),
                        rs.getString("local_name"),
                        rs.getString("checksum"),
                        rs.getLong("size")
                )
        );
    }
}

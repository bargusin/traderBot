package ru.rapidcoder.trader.core.database.repository;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.core.database.DatabaseManager;
import ru.rapidcoder.trader.core.database.entity.User;

import java.util.Optional;

public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public User save(User user) {
        return databaseManager.executeInTransaction(session -> {
            User savedUser = session.merge(user);
            session.flush();
            return savedUser;
        });
    }

    public Optional<User> findByChatId(Long chatId) {
        try {
            return databaseManager.executeInTransaction(session -> {
                String hql = "FROM User u WHERE u.chatId = :chatId";
                Query<User> query = session.createQuery(hql, User.class);
                query.setParameter("chatId", chatId);
                User user = query.uniqueResult();
                return Optional.ofNullable(user);

            });
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя chatId={}", chatId, e);
            return Optional.empty();
        }
    }

    public boolean existsByChatId(Long chatId) {
        try {
            return databaseManager.executeInTransaction(session -> {
                String hql = "SELECT count(u) FROM User u WHERE u.chatId = :chatId";
                Query<Long> query = session.createQuery(hql, Long.class);
                query.setParameter("chatId", chatId);
                Long count = query.uniqueResult();
                return count != null && count > 0;
            });
        } catch (Exception e) {
            logger.error("Ошибка при проверке существования chatId={}", chatId, e);
            return false;
        }
    }
}

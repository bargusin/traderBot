package ru.rapidcoder.trader.core.database.repository;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.core.TradingMode;
import ru.rapidcoder.trader.core.database.DatabaseManager;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.entity.UserSetting;

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
                String hql = "SELECT u FROM User u LEFT JOIN FETCH u.settings WHERE u.chatId = :chatId";
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

    public void updateToken(Long chatId, TradingMode mode, String newToken) {
        databaseManager.executeInTransaction(session -> {
            String hql = "SELECT u FROM User u LEFT JOIN FETCH u.settings WHERE u.chatId = :chatId";
            User user = session.createQuery(hql, User.class)
                    .setParameter("chatId", chatId)
                    .uniqueResult();
            if (user != null) {
                Optional<UserSetting> existingSetting = user.getSettings()
                        .stream()
                        .filter(s -> s.getMode()
                                .equals(mode))
                        .findFirst();

                if (existingSetting.isPresent()) {
                    existingSetting.get()
                            .setEncryptedToken(newToken);
                    logger.info("Обновлен токен для пользователя {} в режиме {}", chatId, mode);
                } else {
                    user.addSetting(mode, newToken);
                    logger.info("Создана новая настройка токена для пользователя {} в режиме {}", chatId, mode);
                }
                session.merge(user);
                session.flush();
            } else {
                logger.warn("Попытка обновить токен для несуществующего пользователя: {}", chatId);
                throw new RuntimeException("Попытка обновить токен для несуществующего пользователя: " + chatId);
            }
            return null;
        });
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

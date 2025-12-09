package ru.rapidcoder.trader.core.database.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.core.database.model.User;

import java.util.Optional;

public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final SessionFactory sessionFactory;

    public UserRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public User save(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User savedUser = session.merge(user);
            transaction.commit();
            return savedUser;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Ошибка при сохранении пользователя chatId={}", user.getChatId(), e);
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        }
    }

    public Optional<User> findByChatId(Long chatId) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            String hql = "FROM User u WHERE u.chatId = :chatId";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("chatId", chatId);
            transaction.commit();
            return Optional.ofNullable(query.uniqueResult());
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя chatId={}", chatId, e);
            return Optional.empty();
        }
    }

    public boolean existsByChatId(Long chatId) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT count(u) FROM User u WHERE u.chatId = :chatId";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("chatId", chatId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Ошибка при проверке существования chatId={}", chatId, e);
            return false;
        }
    }
}

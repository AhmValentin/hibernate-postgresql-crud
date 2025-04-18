package dao;

import jakarta.persistence.EntityNotFoundException;
import model.User;
import org.hibernate.Session;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.SessionFactory;


public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);
    private final SessionFactory sessionFactory;

    public UserDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(User user) {
        validateUser(user);
        executeInTransaction(session -> {
            if (emailExists(session, user.getEmail())) {
                logger.error("Email {} уже сеществует", user.getEmail());
                throw new DuplicateEmailException("Email уже существует");
            }
            session.persist(user);
            logger.info("Пользователь {} сохранён", user.getEmail());
        });
    }

    @Override
    public User findById(Long id) {
        if (id == null ) {
            logger.error("Передан null ID");
            throw new IllegalArgumentException("ID не может быть null");
        }
        if (id <= 0) {
            logger.error("Передан невалидный ID: {}", id);
            throw new IllegalArgumentException("ID не может быть отрицательным");
        }
        try (Session session = sessionFactory.openSession()) {
            logger.info("Найден пользователь с ID: {}", id);
            return session.get(User.class, id);
        } catch (Exception e) {
            logger.error("Пользователь с ID {} не найден", id);
            throw new DaoException("Пользователь с ID " + id + " не найден", e);
        }
    }


    @Override
    public List<User> getAllUsers() {
        try (Session session = sessionFactory.openSession()) {
            session.setDefaultReadOnly(true);
            return session.createQuery("FROM User", User.class).getResultList();
        }
    }

    @Override
    public void update(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        executeInTransaction(session -> {
            User existing = session.get(User.class, user.getId());
            if (existing == null) {
                logger.error("Пользователь не найден");
                throw new EntityNotFoundException("Пользователь не найден");
            }
            session.merge(user);
            logger.info("Пользователь обновлен");
        });
    }

    @Override
    public void delete(User user) {
        validateUser(user);
        executeInTransaction(session -> {
            if (!existsInSession(session, user)) {
                throw new EntityNotFoundException("Пользователь с ID " + user.getId() + " не найден");
            }
            session.remove(session.contains(user) ? user : session.merge(user));
            logger.info("Пользователь {} удален", user.getEmail());
        });
    }

    private void executeInTransaction(Consumer<Session> action) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            try {
                action.accept(session);
                session.getTransaction().commit();
            } catch (Exception e) {
                logger.error("Ошибка операции");
                session.getTransaction().rollback();
                throw new DaoException("Ошибка операции", e);
            }
        }
    }

    private void validateUser(User user) {
        if (user == null) {
            logger.error("Пользователь не может быть null");
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.error("Email не может быть пустым");
            throw new IllegalArgumentException("Email не может быть пустым");
        }
    }

    private boolean existsInSession(Session session, User user) {
        return session.createQuery(
                        "SELECT 1 FROM User u WHERE u.id = :id", Integer.class)
                .setParameter("id", user.getId())
                .setMaxResults(1)
                .uniqueResult() != null;
    }

    private boolean emailExists(Session session, String email) {
        return !session.createQuery(
                        "SELECT 1 FROM User u WHERE u.email = :email", Integer.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    public static class DaoException extends RuntimeException {
        public DaoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DuplicateEmailException extends DaoException {
        public DuplicateEmailException(String message) {
            super(message, null);
        }
    }
}

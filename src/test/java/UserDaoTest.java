import dao.UserDaoImpl;
import model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class UserDaoTest {
    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private Query<User> query;

    @Mock
    private Query<Integer> query1;


    @InjectMocks
    private UserDaoImpl userDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.getTransaction()).thenReturn(transaction);
        when(session.createQuery(anyString(), eq(Integer.class))).thenReturn( query1);
        when(query1.setParameter(anyString(), anyString())).thenReturn(query1);
        when(query1.setMaxResults(anyInt())).thenReturn(query1);
        when(query1.getResultList()).thenReturn(Collections.emptyList());
        when(query1.setParameter(anyString(), any())).thenReturn(query1);
        userDao = new UserDaoImpl(sessionFactory);
    }
    @Test
    void save_ShouldPersistUser() {
        User user = new User("Test", "test@example.com", 30);
        userDao.save(user);
        verify(session).persist(user);
        verify(transaction).commit();
    }

    @Test
    void save_ShouldRollbackOnException() {
        User user = new User("Test", "test@example.com", 30);
        user.setAge(30);
        doThrow(new RuntimeException("DB error")).when(session).persist(user);

        assertThrows(RuntimeException.class, () -> userDao.save(user));
        verify(transaction).rollback();
    }

    @Test
    void findById_ShouldReturnUser() {
        Long id = 1L;
        User expected = new User("Test", "test@example.com", 30);
        when(session.get(User.class, id)).thenReturn(expected);

        User result = userDao.findById(id);

        assertEquals(expected, result);
    }

    @Test
    void findById_ShouldThrowOnInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> userDao.findById(null));
        assertThrows(IllegalArgumentException.class, () -> userDao.findById(-1L));
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList() {
        when(sessionFactory.openSession()).thenReturn(session);
        doNothing().when(session).setDefaultReadOnly(true);
        when(session.createQuery("FROM User", User.class)).thenReturn(query);
        when(query1.getResultList()).thenReturn(Collections.emptyList());

        List<User> result = userDao.getAllUsers();

        assertTrue(result.isEmpty());
        verify(session).close();
    }

    @Test
    void getAllUsers_ShouldReturnUsers() {
        List<User> users = List.of(new User("Test1", "test1@example.com", 30),
                new User("Test2", "test2@example.com", 25));

        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(users);

        List<User> result = userDao.getAllUsers();

        assertEquals(2, result.size());
    }

    @Test
    void update_ShouldMergeUser() {
        User user = new User("Test", "test@example.com", 30);
        user.setId(1L);

        when(session.get(User.class, user.getId())).thenReturn(user);

        userDao.update(user);

        verify(session).merge(user);
        verify(transaction).commit();
    }

    @Test
    void update_ShouldThrowOnNullUser() {
        assertThrows(IllegalArgumentException.class, () -> userDao.update(null));
    }

    @Test
    void delete_ShouldRemoveUser() {
        User user = new User("Test", "test@example.com", 30);
        user.setId(1L);

        when(session.contains(user)).thenReturn(true);
        when(query1.uniqueResult()).thenReturn(1);

        userDao.delete(user);

        verify(session).remove(user);
        verify(transaction).commit();
    }

    @Test
    void delete_ShouldMergeAndRemoveDetachedUser() {
        User user = new User("Test", "test@example.com", 30);
        user.setId(1L);
        User managedUser = new User("Test", "test@example.com", 30);
        when(query1.uniqueResult()).thenReturn(1);
        when(session.contains(user)).thenReturn(false);
        when(session.merge(user)).thenReturn(managedUser);

        userDao.delete(user);

        verify(session).remove(managedUser);
    }
}

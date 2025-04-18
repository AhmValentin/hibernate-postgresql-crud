package dao;

import model.User;
import java.util.List;

public interface UserDao {
    void save(User user);
    User findById(Long id);
    List<User> getAllUsers();
    void update(User user);
    void delete(User user);
}

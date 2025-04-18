import dao.UserDao;
import dao.UserDaoImpl;
import jakarta.persistence.EntityNotFoundException;
import model.User;
import util.HibernateUtil;

import java.util.List;
import java.util.Scanner;

public class MainApp {
    private static final UserDao userDao = new UserDaoImpl(HibernateUtil.getSessionFactory());
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> createUser();
                case 2 -> findUser();
                case 3 -> updateUser();
                case 4 -> deleteUser();
                case 5 -> getAllUsers();
                case 0 -> running = false;
                default -> System.out.println("Неверное значение!");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n1. Создание нового пользователя");
        System.out.println("2. Поиск пользователя по ID");
        System.out.println("3. Обновление пользователя");
        System.out.println("4. Удаление пользователя");
        System.out.println("5. Вывод всех пользователей");
        System.out.println("0. Выход");
        System.out.print("Введите ваш выбор: ");
    }

    private static void createUser() {
        System.out.println("\n--- Создание нового пользователя ---");

        try {
            System.out.print("Введите имя: ");
            String name = scanner.nextLine();

            System.out.print("Введите email: ");
            String email = scanner.nextLine();

            System.out.print("Введите возраст: ");
            int age = Integer.parseInt(scanner.nextLine());

            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setAge(age);
            UserDao userDao = new UserDaoImpl(HibernateUtil.getSessionFactory());
            userDao.save(newUser);

            System.out.println("Пользователь успешно создан! ID: " + newUser.getId());

        } catch (NumberFormatException e) {
            System.err.println("Ошибка: возраст должен быть числом!");
        } catch (javax.validation.ConstraintViolationException e) {
            System.err.println("Ошибка валидации: " + e.getMessage());
        } catch (org.hibernate.exception.ConstraintViolationException e) {
            System.err.println("Ошибка: email уже существует!");
        } catch (Exception e) {
            System.err.println("Ошибка при создании пользователя: " + e.getMessage());
       }
    }

    private static void findUser(){
        System.out.println("\n--- Поиск пользователя по ID ---");
        try {
            System.out.print("Введите ID: ");
            long id = Long.parseLong(scanner.nextLine());

            User user = new UserDaoImpl(HibernateUtil.getSessionFactory()).findById(id);

            if (user == null) {
                System.out.println("Пользователь с ID " + id + " не найден");
                return;
            }

            printUserDetails(user);

        } catch (NumberFormatException e) {
            System.err.println("Ошибка: ID должен быть числом!");
        } catch (UserDaoImpl.DaoException e) {
            System.err.println("Ошибка при поиске: " + e.getCause().getMessage());
        }
    }

    private static void updateUser(){
        System.out.println("\n--- Обновление пользователя ---");
        try {
            System.out.print("Введите ID: ");
            long id = Long.parseLong(scanner.nextLine());
            User user = new UserDaoImpl(HibernateUtil.getSessionFactory()).findById(id);
            if (user == null) {
                System.out.println("Пользователь с ID " + id + " не найден");
                return;
            }
            System.out.println("\nТекущие данные:");
            printUserDetails(user);
            System.out.println("\nВведите новые данные (оставьте пустым для сохранения текущего значения):");
            System.out.print("Введите новое имя: ");
            String name = scanner.nextLine();
            if (!name.isEmpty()) {
                user.setName(name);
            }
            System.out.print("Введите новый email: ");
            String email = scanner.nextLine();
            if (!email.isEmpty()) {
                user.setEmail(email);
            }
            System.out.print("Введите новый возраст: ");
            String age = scanner.nextLine();
            if (!age.isEmpty()) {
                user.setAge(Integer.parseInt(age));
            }
            System.out.print("Подтвердите обновление (y/n): ");
            String confirmation = scanner.nextLine();
            if (confirmation.equalsIgnoreCase("y")) {
                userDao.update(user);
                System.out.println("Данные пользователя успешно обновлены");
            } else {
                System.out.println("Обновление отменено");
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: ID и/или возраст должен быть числом!");
        } catch (javax.validation.ConstraintViolationException e) {
            System.err.println("Ошибка валидации: " + e.getMessage());
        } catch (UserDaoImpl.DaoException e){
            System.err.println("Ошибка при изменение пользователя: " + e.getCause().getMessage());
        }
    }

    private static void deleteUser(){
        System.out.println("\n--- Удаление пользователя ---");
        try{
            System.out.print("Введите ID: ");
            long id = Long.parseLong(scanner.nextLine());
            User user = new UserDaoImpl(HibernateUtil.getSessionFactory()).findById(id);
            if (user == null) {
                System.out.println("Пользователь с ID " + id + " не найден");
                return;
            }
            userDao.delete(user);
            System.out.print("Пользователь с ID " + id + " удален");
        } catch (UserDaoImpl.DaoException e){
            System.err.println("Ошибка при удалении: " + e.getCause().getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: ID должен быть числом!");
        } catch (EntityNotFoundException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Системная ошибка при удалении: " + e.getMessage());
        }
    }

    private static void getAllUsers(){
        System.out.println("\n--- Вывод всех пользователей ---");
        try {
            List<User> users = userDao.getAllUsers();

            if (users.isEmpty()) {
                System.out.println("Пользователи не найдены");
                return;
            }

            users.forEach(MainApp::printUserDetails);

        } catch (UserDaoImpl.DaoException e) {
            System.err.println("Не удалось получить пользователя: " + e.getMessage());
        }
    }

    private static void printUserDetails(User user) {
        System.out.println("\nНайден пользователь:");
        System.out.println("ID: " + user.getId());
        System.out.println("Имя: " + user.getName());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Возраст: " + user.getAge());
        System.out.println("Дата создания: " + user.getCreatedAt());
    }
}

package ru.rapidcoder.trader.core.database.repository.test;

import org.junit.jupiter.api.*;
import ru.rapidcoder.trader.core.TradingMode;
import ru.rapidcoder.trader.core.database.DatabaseManager;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserRepositoryTest {

    private DatabaseManager databaseManager;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.getInstance(":memory:");
        userRepository = new UserRepository(databaseManager);
    }

    @AfterEach
    void cleanup() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    @DisplayName("Интеграционный тест: Полный цикл жизни пользователя (Save -> Find -> Update)")
    void testPersistAndRetrieveUser() {
        User newUser = new User();
        newUser.setChatId(100L);
        newUser.setEncryptedToken("encrypted_token");
        newUser.setUserName("userName");
        newUser.setMode(TradingMode.SANDBOX);

        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());

        Optional<User> foundOpt = userRepository.findByChatId(100L);

        assertTrue(foundOpt.isPresent(), "Пользователь должен быть найден");
        User foundUser = foundOpt.get();

        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals(savedUser.getChatId(), foundUser.getChatId());
        assertEquals(savedUser.getUserName(), foundUser.getUserName());
    }
}

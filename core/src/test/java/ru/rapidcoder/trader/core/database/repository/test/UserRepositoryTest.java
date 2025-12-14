package ru.rapidcoder.trader.core.database.repository.test;

import org.junit.jupiter.api.*;
import ru.rapidcoder.trader.core.TradingMode;
import ru.rapidcoder.trader.core.database.DatabaseManager;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.entity.UserSetting;
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
        newUser.setUserName("Trader");
        newUser.addSetting(TradingMode.SANDBOX, "sandbox_token_value");
        newUser.addSetting(TradingMode.PRODUCTION, "prod_token_value");

        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser);
        assertNotNull(savedUser.getChatId());

        Optional<User> foundOpt = userRepository.findByChatId(100L);

        assertTrue(foundOpt.isPresent(), "Пользователь должен быть найден");
        User foundUser = foundOpt.get();

        assertEquals(savedUser.getChatId(), foundUser.getChatId());
        assertEquals(savedUser.getUserName(), foundUser.getUserName());
        assertEquals(savedUser.getCreatedAt(), foundUser.getCreatedAt());

        Optional<UserSetting> sandboxSetting = foundUser.getSettingByMode(TradingMode.SANDBOX);
        assertTrue(sandboxSetting.isPresent());
        assertEquals("sandbox_token_value", sandboxSetting.get()
                .getEncryptedToken());

        Optional<UserSetting> productionSetting = foundUser.getSettingByMode(TradingMode.PRODUCTION);
        assertTrue(productionSetting.isPresent());
        assertEquals("prod_token_value", productionSetting.get()
                .getEncryptedToken());

        userRepository.updateToken(100L, TradingMode.SANDBOX, "sandbox_token_value");
        Optional<User> refreshedOpt = userRepository.findByChatId(100L);
        assertTrue(refreshedOpt.isPresent());
        User refreshedUser = refreshedOpt.get();
        sandboxSetting = refreshedUser.getSettingByMode(TradingMode.SANDBOX);
        assertTrue(sandboxSetting.isPresent());
        assertEquals("sandbox_token_value", sandboxSetting.get()
                .getEncryptedToken());
    }

    @Test
    @DisplayName("Интеграционный тест: Изменение настроек пользователя (Find -> Update -> Find)")
    void testUpdateTradingMode() {
        User newUser = new User();
        newUser.setChatId(100L);
        newUser.setUserName("Trader");

        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser);
        assertNotNull(savedUser.getChatId());

        Optional<User> foundOpt = userRepository.findByChatId(100L);

        assertTrue(foundOpt.isPresent(), "Пользователь должен быть найден");
        User foundUser = foundOpt.get();

        Optional<UserSetting> sandboxSetting = foundUser.getSettingByMode(TradingMode.SANDBOX);
        assertFalse(sandboxSetting.isPresent());
        Optional<UserSetting> productionSetting = foundUser.getSettingByMode(TradingMode.PRODUCTION);
        assertFalse(productionSetting.isPresent());

        userRepository.updateToken(100L, TradingMode.SANDBOX, "sandbox_token_value");
        Optional<User> refreshedOpt = userRepository.findByChatId(100L);
        assertTrue(refreshedOpt.isPresent());
        User refreshedUser = refreshedOpt.get();
        sandboxSetting = refreshedUser.getSettingByMode(TradingMode.SANDBOX);
        assertTrue(sandboxSetting.isPresent());
        assertEquals("sandbox_token_value", sandboxSetting.get()
                .getEncryptedToken());

        productionSetting = refreshedUser.getSettingByMode(TradingMode.PRODUCTION);
        assertFalse(productionSetting.isPresent());
    }
}

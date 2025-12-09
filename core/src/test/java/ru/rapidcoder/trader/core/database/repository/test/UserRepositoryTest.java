package ru.rapidcoder.trader.core.database.repository.test;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import ru.rapidcoder.trader.core.TradingMode;
import ru.rapidcoder.trader.core.database.DatabaseManager;
import ru.rapidcoder.trader.core.database.model.User;
import ru.rapidcoder.trader.core.database.repository.UserRepository;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserRepositoryTest {

    private static final String TEST_DB_PATH = "test_trader_bot.db";

    private SessionFactory sessionFactory;

    private DatabaseManager databaseManager;

    private UserRepository userRepository;

    @AfterAll
    static void tearDownAll() {
//        File dbFile = new File(TEST_DB_PATH);
//        if (dbFile.exists()) {
//            dbFile.delete();
//        }
    }

    @BeforeAll
    void setUpAll() throws Exception {
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        resetSingleton();
    }

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.getInstance(TEST_DB_PATH);
        sessionFactory = databaseManager.getSessionFactory();
        userRepository = new UserRepository(sessionFactory);
    }

    @AfterEach
    void cleanup() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void resetSingleton() throws Exception {
        Field instanceField = DatabaseManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    @DisplayName("Интеграционный тест: Полный цикл жизни пользователя (Save -> Find -> Update)")
    void testPersistAndRetrieveUser() {
        User newUser = new User();
        newUser.setChatId(100L);
        newUser.setEncryptedToken("enc_token_123");
        newUser.setUserName("userName");
        newUser.setMode(TradingMode.SANDBOX);

        User savedUser = userRepository.save(newUser);
        assertNotNull(savedUser.getId());

        assertNotNull(savedUser.getId());

        sessionFactory.getCache()
                .evictAllRegions();

        Optional<User> foundOpt = userRepository.findByChatId(100L);

        assertTrue(foundOpt.isPresent());
        User fromDb = foundOpt.get();
        assertEquals(100L, fromDb.getChatId());
        assertEquals("enc_token_123", fromDb.getEncryptedToken());
        assertEquals("userName", fromDb.getUserName());
        assertEquals(fromDb.getMode(), TradingMode.SANDBOX);

        fromDb.setMode(TradingMode.PRODUCTION);
        userRepository.save(fromDb);

        // Проверяем обновление
        User updatedDb = userRepository.findByChatId(100L)
                .get();
        assertEquals(updatedDb.getMode(), TradingMode.PRODUCTION);
    }
}

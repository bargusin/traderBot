package ru.rapidcoder.trader.core.database.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import ru.rapidcoder.trader.core.database.DatabaseManager;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseManagerTest {

    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        databaseManager = DatabaseManager.getInstance(":memory:");
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
    void testDatabaseCreationAndMigration() {
        assertNotNull(databaseManager.getSessionFactory(), "SessionFactory должен быть создан после миграций");
        assertNotNull(databaseManager.getDataSource(), "DataSource должен быть создан");
    }

    @Test
    void testSessionFactoryCanOpenSession() {
        try (var session = databaseManager.getSessionFactory()
                .openSession()) {
            assertTrue(session.isConnected(), "Сессия должна быть подключена к БД");

            var connection = session.doReturningWork(conn -> conn);
            assertFalse(connection.isClosed(), "Соединение не должно быть закрыто");
        } catch (Exception e) {
            fail("Не удалось открыть сессию: " + e.getMessage());
        }
    }

    @Test
    void testSessionFactoryIsSingleton() {
        var sessionFactory1 = databaseManager.getSessionFactory();
        var sessionFactory2 = databaseManager.getSessionFactory();

        assertSame(sessionFactory1, sessionFactory2, "SessionFactory должен быть singleton в рамках DatabaseManager");
    }

    @Test
    void testCloseMultipleTimesIsSafe() {
        databaseManager.close();
        assertDoesNotThrow(() -> databaseManager.close(), "Повторный вызов close() не должен вызывать исключений");
    }

    @Test
    @DisplayName("clearCaches: не должен выбрасывать исключений")
    void testClearCaches() {
        assertDoesNotThrow(databaseManager::clearCaches);
    }

    @Test
    @DisplayName("isDatabaseAvailable: должен возвращать true при живом подключении и false при закрытом")
    void testIsDatabaseAvailable() {
        assertTrue(databaseManager.isDatabaseAvailable(), "БД должна быть доступна после инициализации");
        if (databaseManager.getDataSource() instanceof HikariDataSource) {
            ((HikariDataSource) databaseManager.getDataSource()).close();
        }
        assertFalse(databaseManager.isDatabaseAvailable(), "БД не должна быть доступна после закрытия пула");
    }

    @Test
    @DisplayName("Конструктор: должен выбрасывать RuntimeException при ошибке инициализации")
    void testConstructorFailure() throws Exception {
        String tempDirPath = System.getProperty("java.io.tmpdir");
        File directory = Paths.get(tempDirPath)
                .resolve("existing_folder")
                .toFile();
        directory.mkdir();
        String badPath = directory.getAbsolutePath();

        resetSingleton();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            DatabaseManager.getInstance(badPath);
        });

        assertEquals("Не удалось запустить DatabaseManager", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}

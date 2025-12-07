package ru.rapidcoder.trader.core.database.test;

import org.junit.jupiter.api.*;
import ru.rapidcoder.trader.core.database.DatabaseManager;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseManagerTest {

    private static final String TEST_DB_PATH = "test_trader_bot.db";
    private DatabaseManager databaseManager;

    @AfterAll
    static void tearDownAll() {
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
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
}

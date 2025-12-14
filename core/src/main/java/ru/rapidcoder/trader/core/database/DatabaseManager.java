package ru.rapidcoder.trader.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.entity.UserSetting;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private final HikariDataSource dataSource;
    private final SessionFactory sessionFactory;

    private DatabaseManager(String storageFile) {
        try {
            // 1. Инициализация DataSource (HikariCP)
            this.dataSource = createDataSource(storageFile);

            // 2. Запуск миграций (Flyway)
            migrateDatabase(dataSource);

            // 3. Инициализация Hibernate (SessionFactory)
            this.sessionFactory = createSessionFactory(dataSource);
        } catch (Exception e) {
            logger.error("Ошибка инициализации базы данных", e);
            throw new RuntimeException("Не удалось запустить DatabaseManager", e);
        }
    }

    public static synchronized DatabaseManager getInstance(String storageFile) {
        if (instance == null) {
            instance = new DatabaseManager(storageFile);
        }
        return instance;
    }

    private HikariDataSource createDataSource(String storageFile) {
        HikariConfig config = new HikariConfig();
        String jdbcUrl = "jdbc:sqlite:" + storageFile;
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");

        // Настройки пула для SQLite
        config.setMaximumPoolSize(1); // SQLite поддерживает только одно соединение на запись
        config.setMinimumIdle(1);
        config.setPoolName("SQLiteConnectionPool");
        config.setConnectionTimeout(30000); // 30 секунд
        config.setIdleTimeout(60000); // 10 минут
        config.setMaxLifetime(180000); // 30 минут

        // Важные настройки для SQLite
        config.addDataSourceProperty("foreign_keys", "true"); // Включение внешних ключей
        config.addDataSourceProperty("journal_mode", "WAL"); // Режим журналирования
        config.addDataSourceProperty("synchronous", "NORMAL"); // Синхронизация
        config.addDataSourceProperty("busy_timeout", "5000"); // Таймаут ожидания блокировки
        config.addDataSourceProperty("cache_size", "-2000"); // Размер кэша в страницах (2MB)
        config.addDataSourceProperty("temp_store", "MEMORY"); // Временные таблицы в памяти
        config.addDataSourceProperty("locking_mode", "NORMAL");
        config.addDataSourceProperty("auto_vacuum", "INCREMENTAL");

        // Тестовое соединение
        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionInitSql("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;");

        HikariDataSource dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection()) {
            logger.info("Подключение к SQLite успешно: {}", jdbcUrl);
        } catch (Exception e) {
            logger.error("Не удалось подключиться к SQLite", e);
            throw new RuntimeException("Ошибка подключения к базе данных", e);
        }

        return dataSource;
    }

    private void migrateDatabase(DataSource dataSource) {
        logger.info("Запуск миграций Flyway для SQLite...");
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .validateMigrationNaming(true)
                    .sqlMigrationPrefix("V")
                    .sqlMigrationSeparator("__")
                    .sqlMigrationSuffixes(".sql")
                    .load();

            flyway.migrate();
            logger.info("Миграции успешно применены");
        } catch (Exception e) {
            logger.error("Ошибка при применении миграций", e);
            throw new RuntimeException("Не удалось применить миграции базы данных", e);
        }
    }

    private SessionFactory createSessionFactory(DataSource dataSource) {
        Properties settings = new Properties();

        settings.put(JdbcSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
        settings.put(JdbcSettings.DIALECT, "org.hibernate.community.dialect.SQLiteDialect");
        settings.put(JdbcSettings.SHOW_SQL, true);
        settings.put(JdbcSettings.FORMAT_SQL, true);

        settings.put(Environment.HBM2DDL_AUTO, "update"); // validate | update | create | create-drop
        settings.put(Environment.JAKARTA_HBM2DDL_DATABASE_ACTION, "update");
        settings.put(Environment.JAKARTA_HBM2DDL_CREATE_SOURCE, "metadata-then-script");
        settings.put(Environment.JAKARTA_HBM2DDL_DROP_SOURCE, "metadata-then-script");

        // Настройки транзакций
        settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        settings.put(Environment.AUTOCOMMIT, "false");

        // Оптимизации для SQLite
        settings.put(Environment.STATEMENT_BATCH_SIZE, "20");
        settings.put(Environment.ORDER_INSERTS, "true");
        settings.put(Environment.ORDER_UPDATES, "true");

        // Кэширование (отключаем для SQLite или для простоты)
        settings.put(Environment.USE_SECOND_LEVEL_CACHE, "false");
        settings.put(Environment.USE_QUERY_CACHE, "false");
        settings.put(Environment.JAKARTA_SHARED_CACHE_MODE, "NONE");

        // Настройки для работы с SQLite
        settings.put(Environment.NON_CONTEXTUAL_LOB_CREATION, "true");

        // Статистика (можно включить для отладки)
        settings.put(Environment.GENERATE_STATISTICS, "false");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(settings)
                .build();

        MetadataSources sources = new MetadataSources(registry);

        // Регистрация Entity-классов
        sources.addAnnotatedClass(User.class);
        sources.addAnnotatedClass(UserSetting.class);
        // sources.addAnnotatedClass(AuditLog.class);
        //sources.addPackage("ru.rapidcoder.trader.core.database.entity");

        Metadata metadata = sources.getMetadataBuilder()
                .build();

        return metadata.getSessionFactoryBuilder()
                .applyStatisticsSupport(true)
                .build();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Получить новый Session
     */
    public Session openSession() {
        return sessionFactory.openSession();
    }

    /**
     * Получить текущую Session (если используется контекст сессии)
     */
    public Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public <T> T executeInTransaction(Function<Session, T> function) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();
                T result = function.apply(session);
                transaction.commit();
                return result;
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Transaction failed", e);
            }
        }
    }

    public void executeInTransaction(Consumer<Session> consumer) {
        executeInTransaction(session -> {
            consumer.accept(session);
            return null;
        });
    }

    private void closeResources() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            try {
                sessionFactory.close();
                logger.info("SessionFactory закрыта");
            } catch (Exception e) {
                logger.error("Ошибка при закрытии SessionFactory", e);
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                logger.info("DataSource закрыт");
            } catch (Exception e) {
                logger.error("Ошибка при закрытии DataSource", e);
            }
        }
    }

    public void close() {
        closeResources();
        instance = null;
        logger.info("DatabaseManager закрыт");
    }

    public boolean isDatabaseAvailable() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2); // 2 секунды таймаута
        } catch (Exception e) {
            logger.error("Проверка доступности БД не удалась", e);
            return false;
        }
    }

    public void clearCaches() {
        if (sessionFactory != null) {
            sessionFactory.getCache()
                    .evictAllRegions();
            sessionFactory.getCache()
                    .evictAll();
            logger.info("Кэши Hibernate очищены");
        }
    }
}

package ru.rapidcoder.trader.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.core.database.model.User;

import javax.sql.DataSource;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;

    private final HikariDataSource dataSource;
    private final SessionFactory sessionFactory;

    private DatabaseManager() {
        try {
            // 1. Инициализация DataSource (HikariCP)
            this.dataSource = createDataSource();

            // 2. Запуск миграций (Flyway)
            migrateDatabase(dataSource);

            // 3. Инициализация Hibernate (SessionFactory)
            this.sessionFactory = createSessionFactory(dataSource);

        } catch (Exception e) {
            logger.error("Ошибка инициализации базы данных", e);
            throw new RuntimeException("Не удалось запустить DatabaseManager", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        // Указываем путь к файлу БД. Файл создастся автоматически.
        config.setJdbcUrl("jdbc:sqlite:trader_bot.db");
        config.setDriverClassName("org.sqlite.JDBC");

        // Настройки пула
        config.setMaximumPoolSize(5); // Для SQLite (файловая БД) много потоков не нужно
        config.setPoolName("SQLiteConnectionPool");

        // ВАЖНО для SQLite: Включение внешних ключей (по умолчанию выключены)
        config.addDataSourceProperty("foreign_keys", "true");

        return new HikariDataSource(config);
    }

    private void migrateDatabase(DataSource dataSource) {
        logger.info("Запуск миграций Flyway...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        logger.info("Миграции успешно применены.");
    }

    private SessionFactory createSessionFactory(DataSource dataSource) {
        Properties settings = new Properties();

        // Используем Datasource, созданный ранее
        settings.put(Environment.DATASOURCE, dataSource);

        // Диалект для SQLite (из пакета hibernate-community-dialects)
        settings.put(Environment.DIALECT, "org.hibernate.community.dialect.SQLiteDialect");

        settings.put(Environment.SHOW_SQL, "true");
        settings.put(Environment.HBM2DDL_AUTO, "validate"); // Flyway управляет схемой, Hibernate только проверяет

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(settings)
                .build();

        MetadataSources sources = new MetadataSources(registry);

        // ЗДЕСЬ регистрируем ваши Entity классы
        sources.addAnnotatedClass(User.class);
        // sources.addAnnotatedClass(AuditLog.class);

        Metadata metadata = sources.getMetadataBuilder()
                .build();
        return metadata.getSessionFactoryBuilder()
                .build();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (sessionFactory != null)
            sessionFactory.close();
        if (dataSource != null)
            dataSource.close();
    }
}

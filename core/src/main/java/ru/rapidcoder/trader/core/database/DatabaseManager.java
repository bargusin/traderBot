package ru.rapidcoder.trader.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.boot.spi.JpaSettings;
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
        config.setJdbcUrl("jdbc:sqlite:" + storageFile);
        config.setDriverClassName("org.sqlite.JDBC");

        // Настройки пула
        config.setMaximumPoolSize(1);
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
        logger.info("Миграции успешно применены");
    }

    private SessionFactory createSessionFactory(DataSource dataSource) {
        Properties settings = new Properties();

        settings.put(JdbcSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
        settings.put(JdbcSettings.DIALECT, "org.hibernate.community.dialect.SQLiteDialect");
        settings.put(JdbcSettings.SHOW_SQL, true);
        settings.put(JdbcSettings.FORMAT_SQL, true);

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(settings)
                .build();

        MetadataSources sources = new MetadataSources(registry);

        // Регистрация Entity-классов
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

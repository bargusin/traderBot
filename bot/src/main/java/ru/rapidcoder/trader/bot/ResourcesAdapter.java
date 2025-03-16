package ru.rapidcoder.trader.bot;

import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;

public class ResourcesAdapter {

    protected static Properties getProperties() {
        Properties prop = new Properties();
        try (FileInputStream inputStream = new FileInputStream(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                .getResource("bot.properties")).getPath())) {
            prop.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return prop;
    }

}

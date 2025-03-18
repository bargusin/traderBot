package ru.rapidcoder.trader.core;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ResourcesAdapter {

    public static Properties getProperties(String fileProperties) {
        Properties prop = new Properties();
        try (InputStream inputStream = Objects.requireNonNull(ResourcesAdapter.class.getClassLoader()
                .getResourceAsStream(fileProperties))) {
            prop.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return prop;
    }

}

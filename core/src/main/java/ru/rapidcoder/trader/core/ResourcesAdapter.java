package ru.rapidcoder.trader.core;

import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;

public class ResourcesAdapter {

    public static Properties getProperties(String fileProperties) {
        Properties prop = new Properties();
        try (FileInputStream inputStream = new FileInputStream(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                .getResource(fileProperties)).getPath())) {
            prop.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return prop;
    }

}

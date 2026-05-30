package com.notesapp.config;

import java.io.FileInputStream;
import java.util.Properties;

// reads config.properties file to get base urls, login details etc
public class ConfigReader {

    private static Properties prop = new Properties();

    static {
        try {
            // using project directory path to find config file
            String path = System.getProperty("user.dir")
                    + "/src/test/resources/config.properties";
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
        } catch (Exception e) {
            System.out.println("Could not load config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return prop.getProperty(key);
    }
}

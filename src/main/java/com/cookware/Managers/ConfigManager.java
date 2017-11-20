package com.cookware.Managers;

import com.cookware.DataTypes.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Kody on 30/10/2017.
 */
public class ConfigManager {
//    private static final Logger log = Logger.getLogger(ConfigManager.class); // Cannot be used until logs are instanciated
    Config config = new Config();

    public ConfigManager(String path){
        Properties properties = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(path);
            properties.load(input);

            config.logPropertiesPath = properties.getProperty("logProperties").replaceAll("\\s","");
            config.logsPath = properties.getProperty("logs").replaceAll("\\s","");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Config getConfig(){
        return this.config;
    }
}

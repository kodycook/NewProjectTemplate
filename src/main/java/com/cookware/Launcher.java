package com.cookware;

import com.cookware.DataTypes.Config;
import com.cookware.Managers.ConfigManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;



public class Launcher {
    private static final Logger log = Logger.getLogger(Launcher.class);

    public static void main(String[] args) {
        String configPath;

        if(args.length == 0)
        {
            configPath = "config/config.properties";
        }
        else{
            configPath = args[0];
        }

        Config config = (new ConfigManager(configPath)).getConfig();
        System.out.println(config.logsPath);
        System.setProperty("logfilename", config.logsPath);
        DOMConfigurator.configure(config.logPropertiesPath);

        log.info("Starting Launcher");



    }
}

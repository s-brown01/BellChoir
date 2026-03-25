package main.bellchoir;

import java.io.IOException;
import java.util.logging.*;

public class BellChoirLogger {
    
    public static Logger createLogger(String name) {
        Logger logger = Logger.getLogger(name);
        // prevents duplicate logging
        logger.setUseParentHandlers(false);
        
        // Console handler for the console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        // only WARNING and above for the console
        // the user shouldn't see every message, just the ones that are bad
        consoleHandler.setLevel(Level.WARNING);
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);
        
        // this goes specifically to the logs directory
        try {
            // logs/log-yyyy-MM-dd.log
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            FileHandler fileHandler = new FileHandler("logs/log-" + date + ".log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            // everything goes to the file
            // assuming if the user voluntarily looks at the logs file they want to see all logs
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("An error occurred while trying to create a Logger for " + name);
            System.exit(1);
        }
        
        // Overall logger level
        logger.setLevel(Level.ALL); // send everything to handlers
        return logger;
    }
}

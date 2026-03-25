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
        
        // File handler for logs/log.txt
        try {
            // this goes specifically to the logs directory
            FileHandler fileHandler = new FileHandler("logs/log.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            // everything goes to the file
            // assuming if the user voluntarily looks at the logs file they want to see all logs
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Logger could not open file, terminating program");
            System.exit(1);
        }
        
        // Overall logger level
        logger.setLevel(Level.ALL); // send everything to handlers
        return logger;
    }
}

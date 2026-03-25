package main.bellchoir;

import java.io.IOException;
import java.util.logging.*;

public class BellChoirLogger {
    
    public static Logger createLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false); // prevent console output
        
        try {
            // logs/log-yyyy-MM-dd.log
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            FileHandler fh = new FileHandler("logs/log-" + date + ".log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("An error occurred while trying to create a Logger for " + name);
            System.exit(1);
        }
        
        return logger;
    }
}

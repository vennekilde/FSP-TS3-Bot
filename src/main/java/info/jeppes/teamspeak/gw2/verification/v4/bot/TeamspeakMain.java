/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.sun.net.httpserver.HttpServer;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.security.auth.DestroyFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class TeamspeakMain {

    private static final Logger logger = LoggerFactory.getLogger(TeamspeakMain.class);
    public static TeamspeakBot bot;
    public static ResourceBundle config;

    public TeamspeakMain() {
    }

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws javax.security.auth.DestroyFailedException
     */
    public static void main(String[] args) throws IOException, DestroyFailedException {
        String configPathEnv = System.getenv("BOT_CONFIG_PATH");
        if (configPathEnv != null) {
            try (FileInputStream fis = new FileInputStream(configPathEnv)) {
                config = new PropertyResourceBundle(fis);
            }
        } else {
            config = ResourceBundle.getBundle("config");
        }
        //Initiate bot instance
        createTempeakBot();

        //HTTP Server
        logger.info("Starting HTTP Server");
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/teamspeak", new RESTService());
        server.setExecutor(null); // creates a default executor
        server.start();
        logger.info("Started HTTP Server");
    }

    public static TeamspeakBot getBot() {
        return bot;
    }

    public static void destroyTeamspeakBot() throws DestroyFailedException {
        if (bot != null) {
            logger.info("Destroying Teamspeak bot instance");
            bot.destroy();
            bot = null;
        }
    }

    public static void createTempeakBot() throws DestroyFailedException {
        if (bot != null) {
            destroyTeamspeakBot();
        }
        logger.info("Initiating Teamspeak bot instance");
        bot = new TeamspeakBot(config);
        bot.init();
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.security.auth.DestroyFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class RESTService implements HttpHandler {

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(2);

    private final Logger LOGGER = LoggerFactory.getLogger(RESTService.class);

    private final String[] allowedIPs = new String[]{"80.198.76.139", "212.113.133.235", "178.62.152.183", "localhost", "0:0:0:0:0:0:0:1"};

    @Override
    public void handle(HttpExchange request) {
        try {
            String requesterIP = request.getRemoteAddress().getAddress().getHostAddress();
            Map<String, String> queryMap = queryToMap(request.getRequestURI().getQuery());
            String accessToken = TeamspeakMain.getConfig().getString("rest_access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                LOGGER.warn("REST Endpoint disabled. No rest_access_token set in config");
                return;
            }
            if (!queryMap.get("access-token").equals(accessToken)) {
                LOGGER.warn("Rejected ip: " + requesterIP);
                return;
            }

            handleAsync(request, queryMap);

            String response = "1";
            request.sendResponseHeaders(200, response.length());
            OutputStream os = request.getResponseBody();
            os.write(response.getBytes());
            os.close();
            LOGGER.info("Handled request");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAsync(final HttpExchange request, final Map<String, String> queryMap) {
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.info("Recieved request: " + request.getRequestURI().getQuery());
                    if (request.getRequestURI().getQuery() != null) {
                        for (Map.Entry<String, String> param : queryMap.entrySet()) {
                            switch (param.getKey()) {
                                //Refresh user access
                                case "ra":
                                    try {
                                        Integer tsDbid = Integer.parseInt(param.getValue());
                                        TeamspeakMain.getBot().updateUserRoles(tsDbid);
                                    } catch (NumberFormatException e) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                    break;

                                //Soft application restart
                                case "srs":
                                    softRestartApplication();
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
    }

    public Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    public void hardRestartApplication() {
        LOGGER.info("Restart Command Received");
        StringBuilder cmd = new StringBuilder();
        cmd.append("\"" + System.getProperty("java.home") + File.separator + "bin" + File.separator + "java\" ");
        for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            cmd.append(jvmArg + " ");
        }
        cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
        cmd.append(TeamspeakMain.class.getName()).append(" ");

        try {
            LOGGER.info(cmd.toString());
            Runtime.getRuntime().exec(cmd.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void softRestartApplication() throws DestroyFailedException {
        LOGGER.info("Soft restart Command Received");
        ResourceBundle.clearCache();
        TeamspeakMain.destroyTeamspeakBot();
        TeamspeakMain.createTempeakBot();
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class HTTPSJsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(HTTPSJsonUtils.class);

    public static JSONObject getJSONObjectFromHTTPSConnection(String urlString) {
        JSONObject json = null;
        try {
            URL url = new URL(urlString);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            if (con != null) {
                
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    
                    String input;
                    StringBuilder content = new StringBuilder();
                    
                    while ((input = br.readLine()) != null) {
                        content.append(input).append("\n");
                    }
                    
                    json = new JSONObject(content.toString());
                    
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return json;
    }
}

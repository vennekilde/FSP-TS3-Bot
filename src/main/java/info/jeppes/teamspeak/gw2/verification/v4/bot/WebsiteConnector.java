/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class WebsiteConnector {
    private static final Logger logger = LoggerFactory.getLogger(TeamspeakBotMessages.class);
    private static final String STATUS_BASE_URL = "Modules/Verification/REST/Restricted/VerificationStatus.php?service-id=1";
    private static final String NEW_SESSION_BASE_URL = "REST/Restricted/User/GenerateServiceSession.php?service-id=1";
    private static final String GRANT_TEMPORARY_ACCESS_BASE_URL = "Modules/Verification/REST/Restricted/GrantTemporaryAccess.php?service-id=1";
    private static final String SET_USER_SERVICE_LINK_ATTRIBUTE_BASE_URL = "REST/Restricted/User/SetUserServiceLinkAttribute.php?service-id=1";
    private final String baseRESTURL;
    
    public WebsiteConnector(String baseRESTURL){
        this.baseRESTURL = baseRESTURL;
    }
    
    public Map<String, AccessStatusData> getAccessStatusForUsers(SimpleEntry<String,String>... userIds){
        Map<String, AccessStatusData> results = new HashMap();
        try {
            StringBuilder userIdsString = new StringBuilder();
            for(SimpleEntry<String,String> entry : userIds){
                userIdsString.append(entry.getKey());
                if(entry.getValue() != null){
                    userIdsString.append(":").append(entry.getValue());
                }
            }
            String url = baseRESTURL + STATUS_BASE_URL + "&enum-ids&user-ids=" + URLEncoder.encode(userIdsString.toString(), "UTF-8");
            String content = IOUtils.toString(new URL(url), Charset.forName("UTF-8"));
            logger.info("Recieved JSON Response: "+content);
            JSONObject jsonObject = new JSONObject(content);
            for (String key : jsonObject.keySet()) {
                int tsDbid = Integer.parseInt(key);
                JSONObject userVerificationStatusJson = jsonObject.getJSONObject(key);
                
                //Access status
                int accessStatusOrdinal = userVerificationStatusJson.getInt("status");
                AccessStatusData accessStatusData = new AccessStatusData(AccessStatus.values()[accessStatusOrdinal]);
                
                //Check if access is temporary and when it expires
                if(userVerificationStatusJson.has("expires-in")){
                    int expiresIn = userVerificationStatusJson.getInt("expires-in");
                    accessStatusData.setExpires(expiresIn);
                }
                
                //Check for ban reason
                if(accessStatusData.getAccessStatus() == AccessStatus.ACCESS_DENIED_BANNED){
                    String banReason = userVerificationStatusJson.getString("ban-reason");
                    accessStatusData.setBanReason(banReason);
                }
                
                //Check if the access granted is to a music bot
                if(userVerificationStatusJson.has("mirror-link-owner")){
                    JSONObject mirrorLink = userVerificationStatusJson.getJSONObject("mirror-link-owner");
                    int primaryUserId = mirrorLink.getInt("mirror_owner_user_id");
                    logger.info("Found primary tsDbid: "+primaryUserId);
                    if(primaryUserId != tsDbid){
                        accessStatusData.setMusicBotOwner(primaryUserId);
                    } 
                }
                
                //Check if the user service link used has any attributes
                if(userVerificationStatusJson.has("attributes")){
                    String attributesString = userVerificationStatusJson.getString("attributes");
                    JSONObject attributes = new JSONObject(attributesString);
                    accessStatusData.setAttributes(attributes);
                }
                
                results.put(key, accessStatusData);
            }
        } catch (IOException ex) {
            logger.error("Could not connect to website REST API", ex);
            for (SimpleEntry<String,String> entry : userIds) {
                results.put(entry.getKey().toString(), new AccessStatusData(AccessStatus.COULD_NOT_CONNECT));
            }
        }
        return results;
    }
    
    public String createSession(int tsDbid, String ip, String displayName){
        return createSession(tsDbid, ip, displayName, true);
    }
    public String createSession(int tsDbid, String ip, String displayName, boolean isPrimary){
        String session = null;
        try {
            String url = baseRESTURL + NEW_SESSION_BASE_URL + "&user-id=" + tsDbid + "&ip="+ip+"&displayname="+URLEncoder.encode(displayName, "UTF-8")+"&is-primary="+isPrimary;
            JSONObject jsonObject = new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));
            if(jsonObject.has("error")){
                logger.info("Could not create new ts session for tsDbid: "+tsDbid, jsonObject.get("error"));
            } else {
                session = jsonObject.getString("session");
            }
        } catch (IOException ex) { 
            logger.info("Could not create new ts session", ex);
        }
        return session;
    }
    
    public boolean grantTemporaryAccess(int tsDbid, String nickname, AccessType accessType){
        boolean success = false;
        String response = null;
        try {
            String params = "&user-id=" + tsDbid + "&displayname="+URLEncoder.encode(nickname.replace(",", " "), "UTF-8")+"&accesstype="+accessType.name();
            String url = baseRESTURL + GRANT_TEMPORARY_ACCESS_BASE_URL + params;
            response = IOUtils.toString(new URL(url), Charset.forName("UTF-8"));
            JSONObject jsonObject = new JSONObject(response);
            if(jsonObject.has("error")){
                logger.info("Could not grant temporary access to tsDbid: "+tsDbid, jsonObject.get("error"));
            } else {
                success = jsonObject.getString("result").equals("granted");
            }
        } catch (IOException | JSONException ex) { 
            logger.info("Could not grant temporary access. Response: "+response, ex);
        }
        return success;
    }
    
    public boolean SetUserServiceLinkAttribute(int tsDbid, String name, String value){
        boolean success = false;
        String response = null;
        try {
            String params = "&user-id=" + tsDbid + "&name="+name+"&value="+value;
            String url = baseRESTURL + SET_USER_SERVICE_LINK_ATTRIBUTE_BASE_URL + params;
            response = IOUtils.toString(new URL(url), Charset.forName("UTF-8"));
            JSONObject jsonObject = new JSONObject(response);
            if(jsonObject.has("error")){
                logger.info("Could not set attribute for tsDbid: "+tsDbid, jsonObject.get("error"));
            } else {
                success = jsonObject.getString("result").equals("success");
            }
        } catch (IOException | JSONException ex) { 
            logger.info("Could not set attribute . Response: "+response, ex);
        }
        return success;
    }
    
    public enum AccessType {
        HOME_SERVER,
        LINKED_SERVER
    }
}

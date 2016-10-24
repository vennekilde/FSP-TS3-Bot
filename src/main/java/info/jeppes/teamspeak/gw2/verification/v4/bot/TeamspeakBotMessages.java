/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import info.jeppes.teamspeak.gw2.verification.v4.bot.utils.TimeUtils;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class TeamspeakBotMessages extends TeamspeakEventListener{

    private final Logger logger = LoggerFactory.getLogger(TeamspeakBotMessages.class);
    
    public TeamspeakBotMessages(ResourceBundle config, boolean shadowMode) {
        super(config, shadowMode);
    }
    
    public void sendVerifyMessageCheckAccess(int clientId, int tsDbid, String displayName){
        sendVerifyMessageCheckAccess(clientId, tsDbid, displayName, true);
    }
    
    public void sendVerifyMessageCheckAccess(int clientId, int tsDbid, String displayName, boolean hideIfVerified){
        SimpleEntry<String,String> entry = new SimpleEntry(String.valueOf(tsDbid), null);
        Map<String, AccessStatusData> accessStatuses = getWebsiteConnector().getAccessStatusForUsers(entry);
        
        AccessStatusData accessStatus = accessStatuses.get(entry.getKey());
        
        sendVerifyMessageCheckAccess(clientId, tsDbid, displayName, hideIfVerified, accessStatus);
    }
    
    public void sendVerifyMessageCheckAccess(int clientId, int tsDbid, String displayName, boolean hideIfVerified, AccessStatusData accessStatusData){
        switch(accessStatusData.getAccessStatus()){
            case ACCESS_GRANTED_HOME_WORLD:
            case ACCESS_GRANTED_LINKED_WORLD:
                if(!hideIfVerified){
                    sendAlreadyVerifiedMessage(tsDbid, clientId);
                }
                break;
            case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
            case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                if(!hideIfVerified){
                    sendCurrentAccessTypeMessage(clientId, tsDbid, accessStatusData);
                    sendVerifyMessage(clientId, tsDbid, displayName);
                }
                break;
            case ACCESS_DENIED_ACCOUNT_NOT_LINKED:
            case ACCESS_DENIED_EXPIRED:
            case ACCESS_DENIED_INVALID_WORLD:
                sendVerifyMessage(clientId, tsDbid, displayName);
                break;
            case COULD_NOT_CONNECT:
                sendUnableToConnectMessage(tsDbid, clientId);
                break;
            case ACCESS_DENIED_UNKNOWN:
                sendUnknownAccessStatusMessage(tsDbid, clientId);
                break;
            case ACCESS_DENIED_BANNED:
                sendVerificationBannedMessage(tsDbid, clientId, accessStatusData.getBanReason());
                break;
        }
    }
    
    public void sendCurrentAccessTypeMessage(int clientId, int tsDbid, AccessStatusData accessStatusData){
        String message = 
            "\n\n[color=blue][b]Your current access level[/b][/color]\n" +
            "[color=#000000][b]"+accessStatusData.getAccessStatus().name()+"[/b][/color]\n\n";
        
        if(accessStatusData.getExpires() > 0){
            message += "Expires in "+TimeUtils.getTimeWWDDHHMMSSStringShort(accessStatusData.getExpires())+"\n\n";
        }
        if(accessStatusData.getAccessStatus() == AccessStatus.ACCESS_DENIED_BANNED && accessStatusData.getBanReason() != null && !accessStatusData.getBanReason().isEmpty()){
            message += "Ban reason: "+accessStatusData.getBanReason()+"\n";
        }
        if(accessStatusData.isMusicBot()){
            message += "Current user is designated as a Music Bot. Primary database user id: "+accessStatusData.getMusicBotOwner()+"\n\n";
        }
        sendPrivateMessage(tsDbid, clientId, message);
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Sent Current Access Type message to user: "+tsDbid);
    }
    
    public void sendVerifyMessage(int clientId, int tsDbid, String displayName){
        String url = getUniqueLinkURL(tsDbid, clientId, displayName);
        if(url == null){
            return;
        }
        String message = 
            "\n\n[color=blue][b]Welcome to Far Shiverpeaks Community Teamspeak Server![/b][/color]\n" +
            "[color=#000000]If you want access to the restricted channels you have a few options[/color]\n\n" +
                
            "[color=blue][b]1. Temporary Restricted Access[/b][/color]\n"+
            "You can get a temporary 3 week access by simply asking in map chat. If anyone with permissions to give you temporary access is online, they will grant it to you.\n\n" +
            "[i]Copy paste:[/i] Can I get permissions on TS? I am [Insert TS Name], in the lobby from [Insert world name].\n\n"+

            "[color=blue][b]2. Permanent Restricted Access[/b][/color]\n"+
            "If you don't plan on leaving FSP anytime soon, you should consider linking your GuildWars 2 account with the Far Shiverpeaks Community website.\n" +
            "Doing so will allow us to automatically determine which server you are from, thus continuously granting you access each week, until the moment you leave FSP again.\n\n" +
            "You can link your account at [url="+url+"]Authenticate";
        sendPrivateMessage(tsDbid, clientId, message);
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Sent authentication message to user: "+tsDbid);
    }
    public void sendAlreadyVerifiedMessage(int tsDbid, int clientId){
        sendPrivateMessage(tsDbid, clientId, "You already have access to our teamspeak. If this is not the case, please rejoin and you should be assigned to the correct servergroup");
    }
    
    public void sendMusicBotMessage(int clientId){
        ClientInfo clientInfo = getAPI().getClientInfo(clientId);
        sendMusicBotMessage(clientId, clientInfo.getDatabaseId(), clientInfo.getNickname());
    }
    public void sendMusicBotMessage(int clientId, int tsDbid, String name){
        String url = getUniqueMusicBotLinkURL(tsDbid, clientId, name);
        String message = "\n\n----------------------------------------------------------------------------------------------\n" +
            "[color=blue][b]Add Music Bot[/b][/color]\n" +
            "[color=#000000]You can link a ts user to your forum account as a music bot\nPermitting it restricted access[/color]\n" +
            "----------------------------------------------------------------------------------------------\n" +
            "[url="+url+"]Add this client[/url]\n" +
            "----------------------------------------------------------------------------------------------";
        sendPrivateMessage(tsDbid, clientId, message);
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Sent music bot message to user: "+tsDbid);
    }
    
    public void sendUnknownAccessStatusMessage(int tsDbid, int clientId){
        sendPrivateMessage(tsDbid, clientId, 
                "\nFor an unknown reason, your account is not verified\n"
              + "If you just transfered to Far Shiverpeaks, but is still on your old server for the end of the matchup, could be the cause\n"
              + "Please contact an admin if you believe this to be a mistake, or wait a little while until the\n"
              + "API refreshes the persisted data which happens once every 30 minutes and see if that fixed the issue\n");
    }
    public void sendVerificationBannedMessage(int tsDbid, int clientId, String banReason){
        sendPrivateMessage(tsDbid, clientId, 
                "\nYou have been verification banned from Far Shiverpeaks community\n"
              + "Please contact an admin if you believe this to be a mistake\n"
              + "\nBan reason: " + banReason + "\n\n");
    }
    public void sendUnableToConnectMessage(int tsDbid, int clientId){
        sendPrivateMessage(tsDbid, clientId, 
                "Could not connect to verification server\n"
              + "If you need access to the restricted channels\n"
              + "Please reconnect and see if you still receive this message\n"
              + "If you still see this message, please contact and admin to make them aware of the issue\n"
              + "They will also be able to manually verify you\n");
    }
    
    public String getUniqueLinkURL(int tsDbid, int clientId, String displayName){
        String url = null;
        ClientInfo clientInfo = getAPI().getClientInfo(clientId);
        if(clientInfo != null){
            String ip = clientInfo.getIp();
            String generatedSession = getWebsiteConnector().createSession(tsDbid, ip, displayName, true);
            url = "farshiverpeaks.com/index.php?page=gw2integrationv4&ls-sessions="+generatedSession;
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Generated session: \""+generatedSession+"\" for tsdbid: "+tsDbid+" with ip: "+ip);
        } else {
            logger.info("User with tsdbid: "+tsDbid+" & clientid: "+clientId+" cannot be found. Maybe disconnected too quickly?");
        }
        return url;
    }
    
    public String getUniqueMusicBotLinkURL(int tsDbid, int clientId, String displayName){
        String ip = getAPI().getClientInfo(clientId).getIp();
        String generatedSession = getWebsiteConnector().createSession(tsDbid, ip, displayName, false);
        String url = "farshiverpeaks.com/index.php?page=gw2integrationv4&ls-sessions="+generatedSession+"&tab=3";
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Generated session: \""+generatedSession+"\" for tsdbid: "+tsDbid+" with ip: "+ip);
        return url;
    }
}

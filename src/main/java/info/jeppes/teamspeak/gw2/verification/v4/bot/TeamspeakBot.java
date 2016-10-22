/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class TeamspeakBot extends TeamspeakBotMessages{

    private final Logger logger = LoggerFactory.getLogger(TeamspeakBot.class);
    
    public TeamspeakBot(ResourceBundle config) {
        super(config, false);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void onClientMoved(ClientMovedEvent event) {
        int clientId = event.getClientId();
        int tsDbid = getAPI().getClientInfo(clientId).getDatabaseId();
        int targetChannelId = event.getClientTargetId();
        switch(targetChannelId){
            case informationChannel:
                sendVerifyMessageCheckAccess(clientId, tsDbid, getAPI().getClientInfo(clientId).getNickname(), false);
                break;
            case musicBotRegisterChannel:
                sendMusicBotMessage(clientId);
                break;
        }
    }
    
    @Override
    public void onClientJoin(ClientJoinEvent event) {
        if(event.getClientType() == 0){
            int clientId = event.getClientId();
            int tsDbid = event.getClientDatabaseId();
            AbstractMap.SimpleEntry<String,String> entry = new AbstractMap.SimpleEntry(String.valueOf(tsDbid), event.getClientNickname());
            Map<String, AccessStatusData> accessStatuses = getWebsiteConnector().getAccessStatusForUsers(entry);
            
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + accessStatuses.toString());
            AccessStatusData accessStatus = accessStatuses.get(entry.getKey());
            
            boolean givenTempAccess = checkIfReceivedTemporaryAccess(tsDbid, event.getClientNickname(), accessStatus);
            //Refresh data from website
            //TODO Consider not doing this, as the website actually informs the teamspeak
            //bot if a user has recieved temporary access, which causes the bot to
            //refresh the user data twice
            if(givenTempAccess) {
                accessStatuses = getWebsiteConnector().getAccessStatusForUsers(entry);
                logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + accessStatuses.toString());
                accessStatus = accessStatuses.get(entry.getKey());
            }
            updateUserRoles(tsDbid, clientId, accessStatus);
            sendVerifyMessageCheckAccess(clientId, tsDbid, event.getClientNickname() ,true, accessStatus);
        }
    }

    
    public void updateUserRoles(int tsDbid){
        DatabaseClientInfo databaseClientInfo = getAPI().getDatabaseClientInfo(tsDbid);
        if(databaseClientInfo == null){
            //Don't bother, user is not on
            return;
        }
        ClientInfo clientByUId = getAPI().getClientByUId(databaseClientInfo.getUniqueIdentifier());
        int clientId = -1;
        if(clientByUId != null){
            clientId = clientByUId.getId();
        }
        //Access status
        AbstractMap.SimpleEntry<String,String> entry = new AbstractMap.SimpleEntry(String.valueOf(tsDbid), null);
        Map<String, AccessStatusData> accessStatuses = getWebsiteConnector().getAccessStatusForUsers(entry);

        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + accessStatuses.toString());
        AccessStatusData accessStatus = accessStatuses.get(entry.getKey());
        
        updateUserRoles(tsDbid, clientId, accessStatus);
    }
    
    public void updateUserRoles(int tsDbid, int clientId, AccessStatusData accessStatusData){
        switch(accessStatusData.getAccessStatus()){
            case ACCESS_GRANTED_HOME_WORLD:
                if(!accessStatusData.isMusicBot()){
                    //Access is primary and not a music bot
                    this.grantAccess(tsDbid, clientId, fspServerGroupId, accessStatusData);
                } else {
                    //Access is granted trough another user and is a music bot
                    this.grantAccess(tsDbid, clientId, musicBotServerGroupId, accessStatusData);
                }
                break;
            case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
                //Don't assign temporary groups, just let mods/commanders deal with them
                //Though temporary access is still registered, so the bot can auto remove it
                /*if(!accessStatus.isMusicBot()){
                    //Access is primary and not a music bot
                    this.grantAccess(tsDbid, clientId, tempFspServerGroupId, accessStatus, hasPublicChannelAccess);
                } else {
                    //Access is granted trough another user and is a music bot
                    this.grantAccess(tsDbid, clientId, musicBotServerGroupId, accessStatus, hasPublicChannelAccess);
                }*/
                break;
            case ACCESS_GRANTED_LINKED_WORLD:
                if(!accessStatusData.isMusicBot()){
                    //Access is primary and not a music bot
                    this.grantAccess(tsDbid, clientId, linkedWorldServerGroupId, accessStatusData);
                } else {
                    //Access is granted trough another user and is a music bot
                    this.grantAccess(tsDbid, clientId, musicBotServerGroupId, accessStatusData);
                }
                break; 
            case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                //Don't assign temporary groups, just let mods/commanders deal with them
                //Though temporary access is still registered, so the bot can auto remove it
                /*if(!accessStatus.isMusicBot()){
                    //Access is primary and not a music bot
                    this.grantAccess(tsDbid, clientId, tempLinkedWorldServerGroupId, accessStatus, hasPublicChannelAccess);
                } else {
                    //Access is granted trough another user and is a music bot
                    this.grantAccess(tsDbid, clientId, musicBotServerGroupId, accessStatus, hasPublicChannelAccess);
                }*/
                break; 
            case ACCESS_DENIED_ACCOUNT_NOT_LINKED:
            case ACCESS_DENIED_BANNED:
            case ACCESS_DENIED_EXPIRED:
            case ACCESS_DENIED_INVALID_WORLD:
            case ACCESS_DENIED_UNKNOWN:
                removeUserFromVerifiedGroups(tsDbid);
                break;
            case COULD_NOT_CONNECT:
                break;
        }
    }
    
    public void grantAccess(int tsDbid, int clientId, int groupId, AccessStatusData accessStatus){
        
        List<ServerGroup> serverGroups = getAPI().getServerGroupsByClientId(tsDbid);
        boolean move = false;
        for(ServerGroup serverGroup : serverGroups){
            int serverGroupId = serverGroup.getId();
            switch(serverGroupId){
                case fspServerGroupId:
                case tempFspServerGroupId:
                case musicBotServerGroupId:
                case commanderServerGroup:
                case linkedWorldServerGroupId:
                case tempLinkedWorldServerGroupId:
                    if(groupId != serverGroupId){
                        removeUserFromGroup(tsDbid, serverGroupId, move);
                    }
                    break;
            }
        }
        
        //Default to true, as this avoid issues with giving wrongful permissions, however might not 
        //give permissions to those who deserve it, if something went wrong
        boolean hasPublicChannelAccess = true;
        try {
            hasPublicChannelAccess = hasRestrictedChannelAccess(tsDbid);
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Public Channel Access for user: "+tsDbid+": "+hasPublicChannelAccess);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        
        boolean shouldAddGroup = true;
        if(accessStatus.isMusicBot()){
            logger.info("Checking primary & secondary user ip's: PRIMARY tsdbid: "+accessStatus.getMusicBotOwner()+" SECONDARY tsdbid: "+tsDbid);
            DatabaseClientInfo ownerDbid = getAPI().getDatabaseClientInfo(accessStatus.getMusicBotOwner());
            DatabaseClientInfo mirrorDbid = getAPI().getDatabaseClientInfo(tsDbid);
            
            logger.info("PRIMARY DatabaseClientInfo: "+ownerDbid);
            logger.info("SECONDARY DatabaseClientInfo: "+mirrorDbid);
            if(ownerDbid == null){
                shouldAddGroup = false;
                
                if(hasPublicChannelAccess){
                    removeUserFromVerifiedGroups(tsDbid);
                }
                sendPrivateMessage(tsDbid, clientId, 
                    "Cannot find IP for your primary TS user\n"
                +   "Please log into your primary TS user to refresh the IP in case you have changed computer since you last logged in\n"
                +   "You are verified as a Music Bot for TS user with tsDbid: "+ownerDbid+" and therefor need to share ip with that user");
            } else if(!ownerDbid.getLastIp().equals(mirrorDbid.getLastIp())){
                shouldAddGroup = false;
                
                if(hasPublicChannelAccess){
                    removeUserFromVerifiedGroups(tsDbid);
                }
                sendPrivateMessage(tsDbid, clientId, 
                    "You do not have the same IP as the user you are linked to\n"
                +   "Please log into your primary TS user to refresh the IP in case you have changed computer since you last logged in\n"
                +   "You are verified as a Music Bot for TS user with tsDbid: "+ownerDbid+" and therefor need to share ip with that user");
            }
        }
        if(shouldAddGroup){
            if(!hasPublicChannelAccess){
                addUserToGroup(tsDbid, groupId);
            }
        }
    }
    
    public boolean checkIfReceivedTemporaryAccess(int tsDbid, String username, AccessStatusData accessStatusData){
        boolean accessGiven = false;
        //Check if user already has been given access
        int expires = accessStatusData.getExpires();
        switch(accessStatusData.getAccessStatus()){
            case ACCESS_GRANTED_HOME_WORLD:
            case ACCESS_GRANTED_HOME_WORLD_TEMPORARY:
            case ACCESS_GRANTED_LINKED_WORLD:
            case ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY:
                return false;
            case ACCESS_DENIED_EXPIRED:
                JSONObject attributes = accessStatusData.getAttributes();
                if(attributes == null || !attributes.has("tempExpired") || attributes.getString("tempExpired").equals("false")){
                    getWebsiteConnector().SetUserServiceLinkAttribute(tsDbid, "tempExpired", "true");
                    return false;
                }
        }
        
        if(expires <= (System.currentTimeMillis() / 1000)){
            List<ServerGroup> serverGroups = getAPI().getServerGroupsByClientId(tsDbid);
            for(ServerGroup serverGroup : serverGroups){
                switch(serverGroup.getId()){
                    case tempFspServerGroupId:
                        //if(!shadowMode){
                            getWebsiteConnector().grantTemporaryAccess(tsDbid, username, WebsiteConnector.AccessType.HOME_SERVER);
                        //}
                        accessGiven = true;
                        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "User "+tsDbid+" has been granted temporary access [Home World]");
                        break;
                    case tempLinkedWorldServerGroupId:
                        //if(!shadowMode){
                            getWebsiteConnector().grantTemporaryAccess(tsDbid, username, WebsiteConnector.AccessType.LINKED_SERVER);
                        //}
                        accessGiven = true;
                        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "User "+tsDbid+" has been granted temporary access [Linked World]");
                        break;
                }
            }
        }
        return accessGiven;
    }

    @Override
    public void onTextMessage(TextMessageEvent e) {
        //logger.info("Recieved message from "+e.getInvokerName()+ " UUID "+e.getInvokerUniqueId()+". TargetMode "+e.getTargetMode().name()+" : "+e.getMessage());
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.github.theholywaffle.teamspeak3.api.wrapper.AdvancedPermission;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import java.util.List;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author venne
 */
public abstract class TSBotHelper extends TSAPIHelper{
    private static final Logger logger = LoggerFactory.getLogger(TSBotHelper.class);
    
    public final int ebChannelID = 8;
    public final int fspServerGroupId = 56;
    public final int linkedWorldServerGroupId = 261;
    public final int musicBotServerGroupId = 32;
    public final int commanderServerGroup = 19;
    public final int tempFspServerGroupId = 246;
    public final int tempLinkedWorldServerGroupId = 247;
    public final int informationChannel = 5057;
    public final int musicBotRegisterChannel = 5358;
    public final String neededJoinPowerPermission = "i_channel_needed_join_power";
    public final String joinPowerPermission = "i_channel_join_power";
    public int requiredJoinPower = 20;
    public int joinPowerPermissionId = -1;
    public int neededJoinPowerPermissionId = -1;
    
    private WebsiteConnector websiteConnector;
    
    /**
     *
     * @param config
     * @param shadowMode
     */
    public TSBotHelper(ResourceBundle config, boolean shadowMode) {
        super(config, shadowMode);
    }

    @Override
    public void init() {
        super.init();
        websiteConnector = new WebsiteConnector(this.getConfig().getString("base_rest_url"));
    }

    public WebsiteConnector getWebsiteConnector() {
        return websiteConnector;
    }
    
    @Override
    public void onConnectionInitialized() {
        getAPI().addTS3Listeners(this);
        getAPI().registerAllEvents();
        
        
        //Determine permission id for join power permission
        joinPowerPermissionId = getAPI().getPermissionIdByName(joinPowerPermission);
        neededJoinPowerPermissionId = getAPI().getPermissionIdByName(neededJoinPowerPermission);
        logger.info("Needed Join Power Permission ID: "+neededJoinPowerPermissionId);
        requiredJoinPower = fetchRequireJoinPowerToJoinChannel(ebChannelID);
        logger.info("Required Join Power: "+requiredJoinPower);
    }
    
    public int fetchRequireJoinPowerToJoinChannel(int channelId){
        int joinPowerRequired = -1;
        //Determine required join power
        List<Permission> channelPermissions = getAPI().getChannelPermissions(channelId);
        for(Permission permission : channelPermissions){
            if(permission.getName().equals(neededJoinPowerPermission)){
                joinPowerRequired = permission.getValue();
                break;
            }
        }
        return joinPowerRequired;
    }
    
    
    
    public boolean removeUserFromVerifiedGroups(int tsDbid){
        List<ServerGroup> serverGroups = getAPI().getServerGroupsByClientId(tsDbid);
        boolean move = true;
        for(ServerGroup serverGroup : serverGroups){
            int groupId = serverGroup.getId();
            switch(groupId){
                case fspServerGroupId:
                case tempFspServerGroupId:
                case musicBotServerGroupId:
                case commanderServerGroup:
                case linkedWorldServerGroupId:
                case tempLinkedWorldServerGroupId:
                    removeUserFromGroup(tsDbid, groupId, move);
                    move = false;
                    break;
            }
        }
        return !move && !isInShadowmodeForUser(tsDbid);
    }
    public boolean removeUserFromGroup(int tsDbid, int tsGroupId, boolean move){
        return removeUserFromGroup(tsDbid, tsGroupId, move, true, -1);
    }
    
    public boolean removeUserFromGroup(int tsDbid, int tsGroupId, boolean move, boolean fetchClientId, int clientId){
        boolean result = false;
        if(!isInShadowmodeForUser(tsDbid)){
            result = getAPI().removeClientFromServerGroup(tsGroupId, tsDbid);
        }
        
        if(move){
            if(fetchClientId){
                moveClient(tsDbid, 1);
            } else {
                moveClient(tsDbid, 1, clientId);
            }
        }
        
        String groupName = getServerGroupName(tsGroupId);
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Removed user "+tsDbid+" from " + groupName + " Server Group. Performed: "+result);
        
        return result;
    }
    
    public boolean addUserToGroup(int tsDbid, int tsGroupId){
        boolean result = false;
        if(!isInShadowmodeForUser(tsDbid)){
            result = getAPI().addClientToServerGroup(tsGroupId, tsDbid);
        }
        String groupName = getServerGroupName(tsGroupId);
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Add user "+tsDbid+" to " + groupName + " Server Group. Performed: "+result);
        
        return result;
    }
    
    public boolean moveClient(int tsDbid, int channelId){
        boolean result = false;
        DatabaseClientInfo databaseClientInfo = getAPI().getDatabaseClientInfo(tsDbid);
        ClientInfo clientByUId = getAPI().getClientByUId(databaseClientInfo.getUniqueIdentifier());
        if(clientByUId != null){
            result = moveClient(tsDbid, channelId, clientByUId.getId());
        } else {
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "User "+tsDbid+" is currently not online, could not move to channel " + channelId);
        }
        return result;
    }
    
    public boolean moveClient(int tsDbid, int channelId, int clientId){
        boolean result = false;
        if(clientId > 0){
            if(!isInShadowmodeForUser(tsDbid)){
                result = getAPI().moveClient(clientId, 1);
            }
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Moved user "+tsDbid+" to channel " + channelId + ". Performed: "+result);
        } else {
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "User "+tsDbid+" is currently not online, could not move to channel " + channelId);
        }
        return result;
    }
    
    public String getServerGroupName(int tsGroupId){
        
        String groupName;
        switch(tsGroupId){
            case fspServerGroupId:
                groupName = "FSP";
                break;
            case tempFspServerGroupId:
                groupName = "Temp FSP";
                break;
            case linkedWorldServerGroupId:
                groupName = "Linked World";
                break;
            case tempLinkedWorldServerGroupId:
                groupName = "Temp Linked World";
                break;
            case musicBotServerGroupId:
                groupName = "Music Bot";
                break;
            case commanderServerGroup:
                groupName = "Commander";
                break;
            default:
                groupName = String.valueOf(tsGroupId);
        }
        return groupName;
    }
    
    /**
     * Determine if a ts user has access to the restricted channels
     * @param tsDbid
     * @return
     * @throws Exception 
     */
    public boolean hasRestrictedChannelAccess(int tsDbid) throws Exception{
        int heighestJoinPower = 0;
        List<AdvancedPermission> permissionOverview = getAPI().getPermissionOverview(ebChannelID, tsDbid);
        if(permissionOverview == null){
            //Try and re-establish connection
            logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "permissionOverview is null, attempting to re-establish connection");
            checkConnection();
            permissionOverview = getAPI().getPermissionOverview(ebChannelID, tsDbid);
            if(permissionOverview == null){
                throw new Exception("timeout");
            }
        }
        for(AdvancedPermission permission : permissionOverview){
            if(permission.getPermissionId() == joinPowerPermissionId){
                if(heighestJoinPower < permission.getPermissionValue()){
                    heighestJoinPower = permission.getPermissionValue();
                }
            }
        }
        logger.info((isInShadowmodeForUser(tsDbid) ? "ShadowMode: " : "") + "Heighest Join Power for tsDbid: "+ tsDbid +": "+heighestJoinPower);
        return heighestJoinPower >= requiredJoinPower;
    }
    
    public void sendPrivateMessage(int tsDbid, int clientId, String message){
        if(!isInShadowmodeForUser(tsDbid)){
            getAPI().sendPrivateMessage(clientId, message);
        }
    }
    
    public void pokeClient(int tsDbid, int clientId, String message){
        if(!isInShadowmodeForUser(tsDbid)){
            getAPI().pokeClient(clientId, message);
        }
    }
    public void pokeClient(int tsDbid, int clientId, String[] messages){
        if(!isInShadowmodeForUser(tsDbid)){
            for(String message : messages){
                getAPI().pokeClient(clientId, message);
            }
        }
    }
}

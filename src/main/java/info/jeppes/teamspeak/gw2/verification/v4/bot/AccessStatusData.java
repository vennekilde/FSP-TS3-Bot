/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import org.json.JSONObject;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class AccessStatusData {
    
    private final AccessStatus accessStatus;
    private int expires = -1;
    private int musicBotOwner = -1;
    private String banReason = null;
    private JSONObject attributes = null;

    public AccessStatusData(AccessStatus accessStatus){
        this.accessStatus = accessStatus;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }
    
    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public boolean isMusicBot() {
        return musicBotOwner >= 0;
    }

    public int getMusicBotOwner() {
        return musicBotOwner;
    }

    public void setMusicBotOwner(int musicBotOwner) {
        this.musicBotOwner = musicBotOwner;
    }
    
    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public JSONObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JSONObject attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "AccessStatusData{" + "accessStatus=" + accessStatus + ", expires=" + expires + ", musicBotOwner=" + musicBotOwner + ", banReason=" + banReason + ", attributes=" + attributes + '}';
    }
}

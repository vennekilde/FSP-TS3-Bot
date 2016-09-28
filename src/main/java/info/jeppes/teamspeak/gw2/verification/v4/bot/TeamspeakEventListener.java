/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import java.util.ResourceBundle;

/**
 * This class is just to make the implementing class less cluttered, as
 * it does not need to actually implement all these methods, only a few of them
 * @author jeppe
 */
public class TeamspeakEventListener extends TSBotHelper implements TS3Listener{
    
    public TeamspeakEventListener(ResourceBundle config, boolean shadowMode) {
        super(config, shadowMode);
    }
    
    @Override
    public void onTextMessage(TextMessageEvent e) {
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {
    }

    @Override
    public void onClientLeave(ClientLeaveEvent e) {
    }

    @Override
    public void onServerEdit(ServerEditedEvent e) {
    }

    @Override
    public void onChannelEdit(ChannelEditedEvent e) {
    }

    @Override
    public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {
    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent e) {
    }

    @Override
    public void onChannelDeleted(ChannelDeletedEvent e) {
    }

    @Override
    public void onChannelMoved(ChannelMovedEvent e) {
    }

    @Override
    public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {
    }
    
}

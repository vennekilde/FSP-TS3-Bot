/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

import com.github.theholywaffle.teamspeak3.KeepAliveThread;
import com.github.theholywaffle.teamspeak3.SocketReader;
import com.github.theholywaffle.teamspeak3.SocketWriter;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.commands.CQuit;
import com.github.theholywaffle.teamspeak3.commands.Command;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author venne
 */
public abstract class TSAPIHelper implements TS3Listener, Destroyable{
    private final Logger logger = LoggerFactory.getLogger(TSAPIHelper.class);
    
    private final TS3Config ts3Config;
    private TS3Query query;
    private TS3Api api;
    private final ResourceBundle config;
    
    //Stops the bot from making any permanent changes, instead it 
    //just logs what it would have done, without actually doing it.
    private final boolean shadowMode;
    
    public List<Integer> ignoreShadowmodeUsers;
    private Timer connectionKeepAliveTimer;
    
    /**
     *
     * @param shadowMode
     */
    public TSAPIHelper(ResourceBundle config, boolean shadowMode){
        this.config = config;
        this.shadowMode = shadowMode;
        ignoreShadowmodeUsers = new ArrayList();
        ignoreShadowmodeUsers.add(2948);
        ignoreShadowmodeUsers.add(31842);
        ignoreShadowmodeUsers.add(36520);
        ignoreShadowmodeUsers.add(16250);
        
        ts3Config = new TS3Config();
        ts3Config.setHost(config.getString("ts3_address"));
        ts3Config.setQueryPort(Integer.parseInt(config.getString("ts3_query_port")));
        ts3Config.setDebugLevel(Level.SEVERE);
        ts3Config.setLoginCredentials(config.getString("ts3_query_username"), config.getString("ts3_query_password"));
        ts3Config.setFloodRate(TS3Query.FloodRate.UNLIMITED);
    }
    
    
    public void init(){
        establishNewTSConnection();
        
        //Watchdog thread to make sure the connection is reestablished if lost
        connectionKeepAliveTimer = new Timer();
        connectionKeepAliveTimer.schedule(new TimerTask(){
            @Override
            public void run() {
                try{
                    //logger.debug("Checking Connection");
                    checkConnection();
                } catch(Exception e){
                    logger.error("Could not check connection/reconnect to TS server");
                    logger.error(e.getMessage(), e);
                }
            }
        }, 10000, 10000);
        
        //In case of event errors, check who is online every minute and save the ip of those who are authenticated
        /*Timer timer2 = new Timer();
        timer2.schedule(new TimerTask(){
            @Override
            public void run() {
                try{
                    HashMap<Integer, String> authMapClone = (HashMap<Integer, String>) ((HashMap) authenticatedIPAddressOnline).clone();
                    HashMap<Integer, String> mirrorMapClone = (HashMap<Integer, String>) ((HashMap) mirrorAuthenticatedIPAddressOnline).clone();
                    
                    for(Integer clientId : authMapClone.keySet()){
                        try{
                            ClientInfo clientInfo = api.getClientInfo(clientId);
                            if(clientInfo == null){
                                authMapClone.remove(clientId);
                            }
                        } catch(Exception e){
                            authMapClone.remove(clientId);
                        }
                    }
                    for(Integer clientId : mirrorMapClone.keySet()){
                        try{
                            if(!mirrorMapClone.containsKey(clientId)){
                                mirrorMapClone.remove(clientId);
                            }
                        } catch(Exception e){
                            mirrorMapClone.remove(clientId);
                        }
                    }
                    
                    
                } catch(Exception e){
                    logger.error(e.getMessage(), e);
                }
            }
        }, 120000, 120000);*/
        
        //api.addClientToServerGroup(6, testTsDbid);
        //api.removeClientFromServerGroup(6, testTsDbid);
        //api.removeClientFromServerGroup(56, testTsDbid);
        //api.removeClientFromServerGroup(72, testTsDbid);
        //api.addClientToServerGroup(72, testTsDbid);
    }

    public ResourceBundle getConfig() {
        return config;
    }
    
    public synchronized TS3Api getAPI(){
        return api;
    }
    
    public boolean isInShadowmodeForUser(int tsDbid){
        if(ignoreShadowmodeUsers.contains(tsDbid)){
            return false;
        }
        return shadowMode;
    }
    public boolean isInShadowMode(){
        return shadowMode;
    }
    
    public synchronized void establishNewTSConnection(){
        if(!isDestroyed()){
            try{
                logger.info("Establishing new TS connection");
                query = new TS3Query(ts3Config);
                query.connect();
                api = query.getApi();
                initTSConnection();
            } catch(Exception e){
                exitQuery();
                throw e;
            }
        } else {
            logger.warn("Attempted to establish new connection when bot instance is destroyed");
        }
    }
    
    public synchronized void checkConnection(){
        boolean reconnect = false;
        if(
                query == null 
             || query.getSocket() == null 
             || !query.getSocket().isConnected() 
             || query.getCommandList() == null 
        ){
            reconnect = true;
        }
        if(!reconnect){
            ServerQueryInfo whoAmI = api.whoAmI();
            reconnect = whoAmI == null;
        }
        if(reconnect){
            try{
                exitQuery();
            } catch(Exception e){}
            establishNewTSConnection();
        }
    }
    
    private void exitQuery(){
        try{
            query.exit();
        } catch(Exception e){
            //Exit wont work on query if not connection was made, leaving some connections open
            
            // Send a quit command synchronously
            // This will guarantee that all previously sent commands have been processed
            try {
                query.doCommand(new CQuit());
            } catch(Exception ignored){}
                
            KeepAliveThread keepAlive = null;
            try {
                Field field = query.getClass().getDeclaredField("keepAlive");
                field.setAccessible(true);
                keepAlive = (KeepAliveThread) field.get(query);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(TSAPIHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            SocketWriter socketWriter = null;
            try {
                Field field = query.getClass().getDeclaredField("socketWriter");
                field.setAccessible(true);
                socketWriter = (SocketWriter) field.get(query);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(TSAPIHelper.class.getName()).log(Level.SEVERE, null, ex);
            }

            SocketReader socketReader = null;
            try {
                Field field = query.getClass().getDeclaredField("socketReader");
                field.setAccessible(true);
                socketReader = (SocketReader) field.get(query);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(TSAPIHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if (keepAlive != null) {
                keepAlive.interrupt();
            }
            if (socketWriter != null) {
                socketWriter.interrupt();
            }
            if (socketReader != null) {
                socketReader.interrupt();
            }

            if (query.getOut() != null) {
                query.getOut().close();
            }
            if (query.getIn() != null) {
                try {
                    query.getIn().close();
                } catch (IOException ignored) {
                }
            }
            if (query.getSocket() != null) {
                try {
                    query.getSocket().close();
                } catch (IOException ignored) {
                }
            }

            try {
                if (keepAlive != null) {
                    keepAlive.join();
                }
                if (socketWriter != null) {
                    socketWriter.join();
                }
                if (socketReader != null) {
                    socketReader.join();
                }
            } catch (final InterruptedException ex) {
                // Restore the interrupt for the caller
                Thread.currentThread().interrupt();
            }

            ConcurrentLinkedQueue<Command> commandList = null;
            try {
                Field field = query.getClass().getDeclaredField("commandList");
                field.setAccessible(true);
                commandList = (ConcurrentLinkedQueue<Command>) field.get(query);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(TSAPIHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            commandList.clear();
            commandList = null;
            
            
            java.util.logging.Logger queryLogger = null;
            try {
                Field field = query.getClass().getDeclaredField("log");
                field.setAccessible(true);
                queryLogger = (java.util.logging.Logger) field.get(query);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(TSAPIHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (final Handler lh : queryLogger.getHandlers()) {
                queryLogger.removeHandler(lh);
            }
        }
    }
    
    private void initTSConnection(){
        api.selectVirtualServerById(Integer.parseInt(config.getString("ts3_virtual_server_id")));
        String baseNickName = "Far Shiverpeaks Bot";
        boolean success = api.setNickname(baseNickName);
        logger.info("setNickname success: "+success);
        if(!success){
            int id = 2;
            while(id < 100){
                String nickname = baseNickName+" ["+id+"]";
                success = api.setNickname(nickname);
                if(success){
                    logger.debug("Nickname "+nickname +" successfull");
                    break;
                }
                id++;
                logger.debug("Nickname "+nickname +" is already taken");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(TSAPIHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            logger.debug("Nickname "+baseNickName +" successfull");
        }
        onConnectionInitialized();
    }
    
    public abstract void onConnectionInitialized();

    @Override
    public void destroy() throws DestroyFailedException {
        connectionKeepAliveTimer.cancel();
        exitQuery();
    }
    
    
}

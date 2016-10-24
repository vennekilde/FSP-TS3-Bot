/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot.utils;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class TimeUtils {
    public static int[] getTimeWWDDHHMMSS(long time) {     
        int weeks = (int) time / 604800;
        int days = (int) (time % 604800) / 86400;
        int hours = (int) ((time % 604800) % 86400) / 3600;
        int minutes = (int) (((time % 604800) % 86400) % 3600) / 60;
        int seconds = (int) (((time % 604800) % 86400) % 3600) % 60;
        return new int[]{weeks,days,hours,minutes,seconds};
    }
    
    public static String getTimeWWDDHHMMSSStringShort(long time){
        int[] timeWWDDHHMMSS = getTimeWWDDHHMMSS(time);
        boolean useWeeks = timeWWDDHHMMSS[0] != 0;
        boolean useDays = timeWWDDHHMMSS[1] != 0;
        boolean useHours = timeWWDDHHMMSS[2] != 0;
        boolean useMinuts = timeWWDDHHMMSS[3] != 0;
        boolean useSeconds = timeWWDDHHMMSS[4] != 0;
        String timeString = (useWeeks ? (timeWWDDHHMMSS[0] + " week"+ (timeWWDDHHMMSS[0] == 1 ? " " : "s ")): "") + 
                (useDays ? (timeWWDDHHMMSS[1] + " day"+ (timeWWDDHHMMSS[1] == 1 ? " " : "s ")): "") + 
                (useHours ? (timeWWDDHHMMSS[2] + " hour"+ (timeWWDDHHMMSS[2] == 1 ? " " : "s ")): "") + 
                (useMinuts ? (timeWWDDHHMMSS[3] + " minute"+ (timeWWDDHHMMSS[3] == 1 ? " " : "s ")): "") + 
                ((useWeeks || useDays || useHours || useMinuts) && useSeconds ? "and " : "")+
                (useSeconds ? (timeWWDDHHMMSS[4] +" second" + (timeWWDDHHMMSS[4] == 1 ? "" : "s")) : "");
        if(timeString.isEmpty()){
            return "0 seconds";
        } 
        return timeString;
    }
}

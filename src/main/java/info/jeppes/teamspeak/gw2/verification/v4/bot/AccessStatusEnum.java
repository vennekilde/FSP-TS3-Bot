/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.teamspeak.gw2.verification.v4.bot;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public enum AccessStatusEnum {
    ACCESS_GRANTED_HOME_WORLD,
    ACCESS_GRANTED_LINKED_WORLD,
    ACCESS_GRANTED_HOME_WORLD_TEMPORARY,
    ACCESS_GRANTED_LIMKED_WORLD_TEMPORARY,
    ACCESS_DENIED_ACCOUNT_NOT_LINKED,
    ACCESS_DENIED_EXPIRED,
    ACCESS_DENIED_INVALID_WORLD,
    ACCESS_DENIED_BANNED,
    ACCESS_DENIED_UNKNOWN,
    COULD_NOT_CONNECT
}

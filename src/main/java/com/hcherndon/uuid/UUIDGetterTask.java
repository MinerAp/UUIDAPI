package com.hcherndon.uuid;

import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileCriteria;

/**
 * Created by hcherndon on 3/9/14.
 */
public class UUIDGetterTask implements Runnable {
    private final String AGENT = "minecraft";
    private String playerName;
    private UUIDAPI uuidapi;
    private CompletionTask onComplete;

    public UUIDGetterTask(String playerName, UUIDAPI uuidapi, CompletionTask onComplete) {
        this.playerName = playerName;
        this.uuidapi = uuidapi;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        for(final Profile profile : uuidapi.getMojangRepository().findProfilesByCriteria(new ProfileCriteria(playerName, AGENT))){
            uuidapi.getStore().get().put(profile.getName(), profile.getId());
            uuidapi.runCompletion(new Runnable() {
                @Override
                public void run() {
                    onComplete.onUUIDGet(profile.getName(), profile.getId());
                }
            });
        }
    }
}

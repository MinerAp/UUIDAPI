package com.hcherndon.uuid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by hcherndon on 3/9/14.
 */
public class FileGetTask implements Runnable {
    private UUIDAPI uuidapi;
    private String player;
    private CompletionTask onCompletion;

    public FileGetTask(UUIDAPI uuidapi, String player, CompletionTask onCompletion) {
        this.uuidapi = uuidapi;
        this.player = player;
        this.onCompletion = onCompletion;
    }

    @Override
    public void run() {
        File save = new File(uuidapi.getDataFolder(), String.format("%s.hch", player));
        if(!save.exists()) {
            uuidapi.runCompletion(new Runnable() {
                @Override
                public void run() {
                    onCompletion.onUUIDGet(null, null);
                }
            });
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(save);
                byte[] in = new byte[fileInputStream.available()];
                fileInputStream.read(in);
                final String[] raw = new String(in).trim().split(":");
                uuidapi.getStore().get().put(raw[0], raw[1]);
                uuidapi.runCompletion(new Runnable() {
                    @Override
                    public void run() {
                        onCompletion.onUUIDGet(raw[0], raw[1]);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

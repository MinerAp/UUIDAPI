package com.hcherndon.uuid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hcherndon on 3/9/14.
 */
public class FileSaveTask implements Runnable {
    private final UUIDAPI uuidapi;
    private final String player;
    private final String uuid;

    public FileSaveTask(UUIDAPI uuidapi, String player, String uuid){
        this.uuidapi = uuidapi;
        this.player = player;
        this.uuid = uuid;
    }

    @Override
    public void run() {
        File saveTo = new File(uuidapi.getDataFolder(), String.format("%s.hch", player));
        try {
            if(saveTo.exists())
                saveTo.delete();
            saveTo.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(saveTo);
            fileOutputStream.write(String.format("%s:%s", player, uuid).getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

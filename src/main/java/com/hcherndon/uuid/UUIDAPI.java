package com.hcherndon.uuid;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.ProfileRepository;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Created by hcherndon on 3/9/14.
 */
public class UUIDAPI extends JavaPlugin implements Listener {
    private final int THREAD_COUNT = 12; //In case anyone wants to change it. Should be a good number at 12 though

    private static UUIDAPI uuidapi;
    public static UUIDAPI getUuidapi() {
        if(uuidapi == null)
            throw new IllegalStateException("UUIDAPI is not loaded yet!");
        return uuidapi;
    }

    //Main Store
    private AtomicReference<Map<String, String>> store = new AtomicReference<Map<String, String>>(new HashMap<String, String>());

    //Services
    private ExecutorService completionThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("UUIDAPI Completor #%d").build());
    private ExecutorService fileThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("UUIDAPI File #%d").build());
    private ExecutorService workerThread = Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactoryBuilder().setNameFormat("UUIDAPI Worker #%d").build());

    //Mojang API
    private final ProfileRepository profileRepository = new HttpProfileRepository();

    @Override
    public void onEnable() {
        uuidapi = this;
        getDataFolder().mkdirs();
        initialLoad();
    }

    @Override
    public void onDisable() {
        workerThread.shutdown();
        fileThread.shutdown();
        completionThread.shutdown();
    }

    private void initialLoad(){
        if(getDataFolder().listFiles().length == 0){
            getLogger().log(Level.INFO, "This looks to be your first time loading UUIDAPI, we will go through all players that have ever joined your server and get their UUID's!");
            getLogger().log(Level.INFO, "We will be going though {0} record!", new Object[]{getServer().getOfflinePlayers().length});
            getLogger().log(Level.INFO, "This is all handled Asynchronously from the main thread, you should not experience any performance issues!");
            getLogger().log(Level.INFO, "Unless you only have like 2 cores.... Then you have an issue.");
            workerThread.execute(new Runnable() {
                @Override
                public void run() {
                    for(OfflinePlayer offlinePlayer : getServer().getOfflinePlayers()){
                        getUUID(offlinePlayer.getName(), new CompletionTask() {
                            @Override
                            public void onUUIDGet(String playerName, String UUID) {
                                saveInFile(playerName, UUID);
                            }
                        });
                    }
                }
            });
        } else {
            getLogger().log(Level.INFO, "Loading, Caching, and Saving {0} UUIDs!", new Object[]{getDataFolder().listFiles().length});
            fileThread.execute(new Runnable() {
                @Override
                public void run() {
                    for (File file : getDataFolder().listFiles()) {
                        getFromFile(file.getName().replaceAll(".hch", ""), new CompletionTask() {
                            @Override
                            public void onUUIDGet(String playerName, String UUID) {
                                //
                            }
                        });
                    }
                }
            });
        }
    }

    @Deprecated
    @EventHandler
    public void onJoin(final PlayerJoinEvent event){
        if(!store.get().containsKey(event.getPlayer().getName()))
            getUUID(event.getPlayer().getName(), new CompletionTask() {
                @Override
                public void onUUIDGet(String playerName, String UUID) {
                    if(playerName == null && UUID == null){
                        getLogger().log(Level.SEVERE, "A UUID for {0} was not found in the Mojang database! Is the account not premium?", new Object[]{event.getPlayer().getName()});
                    }
                    getLogger().log(Level.INFO, "Got, Cached, and Saved {0}'s UUID as '{1}'!", new Object[]{playerName, UUID});
                    saveInFile(playerName, UUID);
                }
            });
    }

    public ProfileRepository getMojangRepository(){
        return profileRepository;
    }

    public void getUUID(String playerName, CompletionTask onGet){
        if(!store.get().containsKey(playerName))
            runTask(new UUIDGetterTask(playerName, this, onGet));
        else
            onGet.onUUIDGet(playerName, store.get().get(playerName));
    }

    public Collection<Map.Entry<String, String>> getAllStoredUUIDS() {
        return Collections.unmodifiableCollection(store.get().entrySet());
    }

    public void getPlayerName(final String uuid, final CompletionTask onGet){
        workerThread.execute(new Runnable() {
            @Override
            public void run() {
                Collection<Map.Entry<String, String>> matches = new HashSet<>();
                for(final Map.Entry<String, String> entry : getAllStoredUUIDS()){
                    if(entry.getValue().equals(uuid)){
                        matches.add(entry);
                    }
                }
                if(matches.size() == 0) {
                    runCompletion(new Runnable() {
                        @Override
                        public void run() {
                            onGet.onUUIDGet(null, null);
                        }
                    });
                } else {
                    for(final Map.Entry<String, String> entry : matches)
                        runCompletion(new Runnable() {
                            @Override
                            public void run() {
                                onGet.onUUIDGet(entry.getKey(), entry.getValue());
                            }
                        });
                }
            }
        });
    }

    protected AtomicReference<Map<String, String>> getStore() {
        return store;
    }

    protected void saveInFile(String player, String uuid){
        runFileSaveTask(new FileSaveTask(this, player, uuid));
    }

    protected void getFromFile(String player, CompletionTask onCompletion){
        runFileGetTask(new FileGetTask(uuidapi, player, onCompletion));
    }

    protected void runFileSaveTask(Runnable runnable){
        fileThread.execute(runnable);
    }

    protected void runFileGetTask(Runnable runnable){
        workerThread.execute(runnable);
    }

    protected void runTask(Runnable runnable){
        workerThread.execute(runnable);
    }

    protected void runCompletion(Runnable runnable){
        completionThread.execute(runnable);
    }
}

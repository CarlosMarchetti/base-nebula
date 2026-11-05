package dev.nebula.client;
import dev.nebula.client.runtime.ClientContext;
import dev.nebula.client.gui.GuiRenderer;
import dev.nebula.client.gui.settings.SettingsManager;
import dev.nebula.client.managers.AttackStatsManager;
import dev.nebula.client.managers.ClanManager;
import dev.nebula.client.modules.ModuleManager;
import dev.nebula.client.utils.SaveLoad;
import dev.nebula.client.utils.Log;
public final class Main {
    private static volatile ClientContext context;
    public void init(){
        ClientContext next=new ClientContext();context=next;
        try {next.initialize();Log.info("Nebula initialized: "+next.modules().getAll().size()+" modules");}
        catch(Exception e){throw new IllegalStateException("Required client initialization failed",e);}
    }
    public static ClientContext getContext(){return context;}
    public static void onClientTick(){
        ClientContext active=context;if(active==null)return;
        try {active.tick();}catch(RuntimeException e){Log.error("Client tick failed: "+e);}
    }
    public static void executeOnClientThread(Runnable task){
        ClientContext active=context;
        if(active==null)throw new IllegalStateException("Client context not initialized");
        active.dispatcher().execute(task);
    }
    public static ModuleManager getModuleManager(){return context==null?null:context.modules();}
    public static SettingsManager getSettingsManager(){return context==null?null:context.settings();}
    public static GuiRenderer getGuiRenderer(){return context==null?null:context.gui();}
    public static SaveLoad getSaveLoad(){return context==null?null:context.persistence();}
    public static ClanManager getClanManager(){return context==null?null:context.clans();}
    public static AttackStatsManager getAttackStatsManager(){return context==null?null:context.stats();}
}

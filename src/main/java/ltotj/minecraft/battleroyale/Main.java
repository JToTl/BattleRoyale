package ltotj.minecraft.battleroyale;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public static CustomConfig participants;

    @Override
    public void onEnable(){
        CustomConfig config=new CustomConfig(this);
        config.saveDefaultConfig();
        participants=new CustomConfig(this,"participants");
        participants.saveDefaultConfig();
        new EventList(this);
        getCommand("battleroyale").setExecutor(new Commands());
        getLogger().info("バトロワプラグインが有効になりました");
    }

    @Override
    public void onDisable(){
        getLogger().info("バトロワプラグインが無効になりました");
    }
}
package ltotj.minecraft.battleroyale;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable(){
        CustomConfig config=new CustomConfig(this);
        config.saveDefaultConfig();
        new EventList(this);
        getCommand("battleroyale").setExecutor(new Commands());
        getLogger().info("バトロワプラグインが有効になりました");
    }

    @Override
    public void onDisable(){
        getLogger().info("バトロワプラグインが無効になりました");
    }
}
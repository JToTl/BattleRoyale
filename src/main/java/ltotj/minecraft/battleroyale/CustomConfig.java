package ltotj.minecraft.battleroyale;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class CustomConfig {

    private FileConfiguration config=null;
    private final File configFile;
    private final String file;
    private final Plugin plugin;

    public CustomConfig(Plugin plugin){
        this(plugin,"config.yml");
    }

    public boolean canGetString(String string){
        try {
            return config.getString(string)!=null;
        }catch(Exception exception){
            return false;
        }
    }

    public boolean canGetList(String string){
        try{
            config.getConfigurationSection(string).getKeys(false);
        }
        catch(Exception exception){
            return false;
        }
        return true;
    }

    public boolean canGetInt(String string){
        try {
            return config.getInt(string)!=0;
        }catch(Exception exception){
            return false;
        }
    }

    public boolean canGetDouble(String string){
        try {
            return config.getDouble(string)!=0;
        }catch(Exception exception){
            return false;
        }
    }

    public boolean canGetBoolean(String string){
        try {
            config.getBoolean(string);
        }catch(Exception exception){
            return false;
        }
        return true;
    }

    public CustomConfig(Plugin plugin,String fileName){
        this.plugin=plugin;
        this.file=fileName;
        configFile=new File(plugin.getDataFolder(),file);
    }

    public void saveDefaultConfig(){
        if(!configFile.exists()){
            plugin.saveResource(file,false);
        }
    }

    public void reloadConfig(){
        config= YamlConfiguration.loadConfiguration(configFile);

        final InputStream defConfigStream=plugin.getResource(file);
        if(defConfigStream==null){
            return;
        }
        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
    }

    public FileConfiguration getConfig(){
        if(config==null){
            reloadConfig();
        }
        return config;
    }

    public void saveConfig(){
        if(config==null){
            return;
        }
        try{
            getConfig().save(configFile);
        }catch(IOException ex){
            plugin.getLogger().log(Level.SEVERE,"コンフィグをセーブできませんでした");

        }
    }

}

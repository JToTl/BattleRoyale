package battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;


import java.util.Arrays;
import java.util.Objects;

public class EventList implements Listener {

    public EventList(Plugin plugin){
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
    }

    private void generatePlayersChest(Player player){
        int count=0,i=0;
        ItemStack[] itemStacks=player.getInventory().getContents();
        Location[] locations={player.getLocation(),player.getLocation().add(0,1,0)};
        locations[0].getBlock().setType(Material.CHEST);
        locations[1].getBlock().setType(Material.CHEST);
        Chest[] chests={(Chest) locations[0].getBlock().getState(),(Chest) locations[1].getBlock().getState()};
        for(ItemStack itemStack:itemStacks){
            if(itemStack!=null) {
                chests[i].getInventory().addItem(itemStack);
                count = count + 1;
                if (count == 27) i = 1;
            }
        }
        GlobalClass.runningGame.playGround.playersChestLocation.addAll(Arrays.asList(locations));
    }

    private String getDisplayName(ItemStack itemStack) {
        if (itemStack!=null&&itemStack.getItemMeta() != null) {
            return itemStack.getItemMeta().getDisplayName();
        }
        return "";
    }

    @EventHandler
    public void DropFromDropShip(final EntityDismountEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player=(Player)e.getEntity();
            if (getDisplayName(player.getInventory().getChestplate()).equals("降下用エリトラ")) {
                Location location=e.getDismounted().getLocation();
                location.add(0,-4,0);
                player.teleport(location);
                player.setGliding(true);
            }
        }
    }

    @EventHandler
    public void CancelDamageByElytra(final EntityDamageEvent e){
        if(e.getCause().equals(EntityDamageEvent.DamageCause.FLY_INTO_WALL))e.setCancelled(true);
    }

    @EventHandler
    public void CancelDamageByPlayer(EntityDamageByEntityEvent e){
        if(e.getDamager().getType().equals(EntityType.PLAYER)){
            Player player=(Player) e.getDamager();
            if(getDisplayName(player.getInventory().getChestplate()).equals("降下用エリトラ"))e.setCancelled(true);
        }
    }

    @EventHandler
    public void ClickElytra(InventoryClickEvent e){
        if(getDisplayName(e.getCurrentItem()).equals("降下用エリトラ")){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerLogout(final PlayerQuitEvent e){
        if(GlobalClass.runningGame==null)return;
        if(GlobalClass.runningGame.playerList.containsKey(e.getPlayer().getUniqueId())) {
            if (!GlobalClass.runningGame.isRunning) {
                GlobalClass.runningGame.playerList.remove(e.getPlayer().getUniqueId());
            } else if (GlobalClass.runningGame.deadPlayerList.contains(e.getPlayer().getUniqueId())) {
                generatePlayersChest(e.getPlayer());
                GlobalClass.runningGame.deadPlayerList.add(e.getPlayer().getUniqueId());
                e.getPlayer().setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    @EventHandler
    public void GenerateCarePackage(EntityExplodeEvent e){
        if(GlobalClass.runningGame==null||!e.getEntity().getType().equals(EntityType.WITHER_SKULL))return;
        if(Objects.equals(e.getEntity().getCustomName(), "carepackage")) {
            e.setCancelled(true);
            e.getEntity().eject();
            GlobalClass.runningGame.playGround.generateCarePackage(e.getLocation());
            GlobalClass.runningGame.playGround.carePackageLocation.add(e.getLocation());
        }
        else if(Objects.equals(e.getEntity().getCustomName(), "dropship")){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void DeleteLootChest(final BlockBreakEvent e){
        if(GlobalClass.editedConfig==null)return;
        Player p=e.getPlayer();
        Location location = e.getBlock().getLocation();
        String displayName=getDisplayName(p.getInventory().getItemInMainHand()),locationkey = location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ();
        if(displayName.equals("§c削除")&&e.getBlock().getType().equals(Material.CHEST)){
            GlobalClass.editedConfig.getConfig().set("chestPosition."+locationkey,null);
            GlobalClass.editedConfig.saveConfig();
            p.sendMessage("X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ() + "のチェストを削除しました");
        }
    }


    @EventHandler
    public void SettingLootChest(final BlockPlaceEvent e){
        if(GlobalClass.editedConfig==null)return;
        Player p=e.getPlayer();
        Location location = e.getBlock().getLocation();
        String displayName=getDisplayName(p.getInventory().getItemInMainHand()),locationkey = location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ();
        if(displayName.length()>6&& displayName.startsWith("§aTier")) {
            GlobalClass.editedConfig.getConfig().set("chestPosition." + locationkey + ".X", location.getBlockX());
            GlobalClass.editedConfig.getConfig().set("chestPosition." + locationkey + ".Y", location.getBlockY());
            GlobalClass.editedConfig.getConfig().set("chestPosition." + locationkey + ".Z", location.getBlockZ());
            GlobalClass.editedConfig.getConfig().set("chestPosition." + locationkey + ".Tier", Integer.parseInt(displayName.substring(6)));
            GlobalClass.editedConfig.saveConfig();
            e.setCancelled(true);
            p.sendMessage("X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ() + "に" + displayName.substring(2) + "チェストを設置しました");
        }
    }

    @EventHandler
    public void RightClickEvent(final PlayerInteractEvent e){
        Player p=e.getPlayer();
        if(GlobalClass.runningGame==null)return;
        if(!e.getAction().equals(Action.RIGHT_CLICK_AIR)&&!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))return;
        if(!p.getInventory().getItemInMainHand().getType().equals(Material.COMPASS))return;
        Location location=new Location(GlobalClass.runningGame.world,GlobalClass.runningGame.playGround.nextCenter[0],100,GlobalClass.runningGame.playGround.nextCenter[1]),
        plocation=p.getLocation();
        p.setCompassTarget(location);
        double d,l=Math.round(10.0*Math.sqrt(Math.pow(plocation.getX()-location.getX(),2)+Math.pow(plocation.getZ()-location.getZ(),2)))*0.1;
        d = Math.max(Math.abs(plocation.getX() - location.getX()), Math.abs(plocation.getZ() - location.getZ()));

        String string;
        if(d<=GlobalClass.runningGame.playGround.nextwidth)string="§aエリア内に滞在中";
        else string="§aエリア内まであと§b"+Math.round(10*l*(d-GlobalClass.runningGame.playGround.nextwidth)/d)/10+"M";

        ItemStack item=p.getInventory().getItemInMainHand();
        ItemMeta meta=item.getItemMeta();
        meta.setDisplayName("§a中心座標まであと§b"+l+"M "+string);
        item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);
    }

    @EventHandler//プレイヤーが死んだら生存人数のやつとかなんとかをいじって、終了ならスレッド止めてなんやかんやしましょう
    public void PlayerKilledEvent(final PlayerDeathEvent e){
        if(GlobalClass.runningGame!=null&&GlobalClass.runningGame.isRunning&&GlobalClass.runningGame.playerList.containsKey(e.getEntity().getPlayer().getUniqueId())) {
            GlobalClass.runningGame.deadPlayerList.add(e.getEntity().getPlayer().getUniqueId());
            generatePlayersChest(e.getEntity());
            e.getEntity().getPlayer().setGameMode(GameMode.SPECTATOR);
            if (e.getEntity().getKiller() != null) {
                Player killer = e.getEntity().getKiller();
                if (GlobalClass.runningGame.playerList.containsKey(killer.getUniqueId())) {
                    GlobalClass.runningGame.playerList.get(killer.getUniqueId()).killCount = GlobalClass.runningGame.playerList.get(killer.getUniqueId()).killCount + 1;
                }
            }
            if (GlobalClass.runningGame.playerList.size() <= GlobalClass.runningGame.deadPlayerList.size() + 1) {
                GlobalClass.runningGame.endGame();
            }
        }
    }

    @EventHandler
    public void DeleteElytra(final EntityToggleGlideEvent e){
        if(GlobalClass.runningGame!=null&&e.getEntity().getType().equals(EntityType.PLAYER)){
            Player player=(Player) e.getEntity();
            if(player.isGliding()&&GlobalClass.runningGame.isRunning&&getDisplayName(player.getInventory().getChestplate()).equals("降下用エリトラ")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1, 200));
                    player.getInventory().getChestplate().setType(Material.AIR);
            }
        }
    }
}

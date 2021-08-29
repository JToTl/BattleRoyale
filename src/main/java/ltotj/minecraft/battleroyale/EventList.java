package ltotj.minecraft.battleroyale;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;


import java.util.*;

public class EventList implements Listener {

    public EventList(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean matchName(ItemStack item,String name){
        if(item!=null&&item.getItemMeta()!=null&&item.getItemMeta().displayName()!=null)
        return item.getItemMeta().displayName().equals(Component.text(name));
        return false;
    }

    private String getDisplayName(ItemStack itemStack) {//要書き換え
        if (itemStack!=null&&itemStack.getItemMeta() != null) {
            return itemStack.getItemMeta().getDisplayName();
        }
        return "";
    }

    @EventHandler
    public void DropFromDropShip(final EntityDismountEvent e) {
        if (e.getEntity() instanceof Player&&GlobalClass.runningGame!=null) {
            Player player=(Player)e.getEntity();
            if (matchName(player.getInventory().getChestplate(),"降下用エリトラ")) {
                e.getEntity().getLocation().setY(e.getEntity().getLocation().getY()-4);
                player.setGliding(true);
                e.getEntity().eject();
            }
        }
    }

    @EventHandler
    public void CancelDamageByElytra(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player&&GlobalClass.runningGame!=null&&GlobalClass.runningGame.isRunning) {
            Player player=(Player)e.getEntity();
            if (e.getCause().equals(EntityDamageEvent.DamageCause.FLY_INTO_WALL) && GlobalClass.runningGame.playerList.containsKey(player.getUniqueId()))
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void CancelDamageByPlayer(EntityDamageByEntityEvent e){
        if(e.getDamager().getType().equals(EntityType.PLAYER)){
            Player player=(Player) e.getDamager();
            if(matchName(player.getInventory().getChestplate(),"降下用エリトラ"))e.setCancelled(true);
        }
    }

    @EventHandler//あるやんけ
    public void PlayerMoveWorldEvent(PlayerChangedWorldEvent e){

    }

    //////////////////////////////
    ///銃の所持数制限のための臨時措置
    @EventHandler
    public void DropItemEvent(PlayerDropItemEvent e){
        if(e.getItemDrop().getItemStack().getType().equals(Material.IRON_HOE)&&GlobalClass.runningGame!=null&&GlobalClass.runningGame.playerList.containsKey(e.getPlayer().getUniqueId())&&!(e.isCancelled())){
            GlobalClass.runningGame.playerList.get(e.getPlayer().getUniqueId()).guns-=1;
        }
    }

    @EventHandler
    public void PickUpItemEvent(EntityPickupItemEvent e){
        if(!(e.getEntity() instanceof Player))return;
        Player player=(Player)e.getEntity();
        if(e.getItem().getItemStack().getType().equals(Material.IRON_HOE)&&GlobalClass.runningGame!=null&&GlobalClass.runningGame.playerList.containsKey(player.getUniqueId())){
            if(GlobalClass.runningGame.playerList.get(player.getUniqueId()).guns>GlobalClass.runningGame.maxGuns-1){
                e.setCancelled(true);
            }
            else{
                GlobalClass.runningGame.playerList.get(player.getUniqueId()).guns+=1;
            }
        }
    }

    @EventHandler
    public void InvCloseEvent(InventoryCloseEvent e){
        if(GlobalClass.runningGame!=null&&GlobalClass.runningGame.playerList.containsKey(e.getPlayer().getUniqueId())&&!(e.getInventory().getType().equals(InventoryType.PLAYER))&&
        GlobalClass.runningGame.playerList.get(e.getPlayer().getUniqueId()).clickGun) {
            ItemStack[] items = e.getPlayer().getInventory().getContents();
            List<ItemStack> guns=new ArrayList<>();
            int count=0;
            for(ItemStack item:items){
                if(item!=null&&item.getType().equals(Material.IRON_HOE)){
                    guns.add(item);
                    count+=1;
                }
            }
            if(count<GlobalClass.runningGame.maxGuns+1){
                GlobalClass.runningGame.playerList.get(e.getPlayer().getUniqueId()).clickGun=false;
                GlobalClass.runningGame.playerList.get(e.getPlayer().getUniqueId()).guns=count;
            }
            else {

                Location location=e.getPlayer().getLocation();
                if(e.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.IRON_HOE))e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.STONE,0));
                e.getPlayer().getInventory().remove(Material.IRON_HOE);
                for(ItemStack item:guns)location.getWorld().dropItem(location, item);
                e.getPlayer().sendMessage(GlobalClass.runningGame.maxGuns+"個以上の銃は持てません");
                GlobalClass.runningGame.playerList.get(e.getPlayer().getUniqueId()).guns=0;
            }
        }
    }
    ////////////////////////////////

    @EventHandler
    public void InvClickEvent(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player))return;
        Player player=(Player)e.getWhoClicked();
        ItemStack clickedItem=e.getCurrentItem();
        if(clickedItem!=null) {
            if (matchName(clickedItem, "降下用エリトラ")) {
                e.setCancelled(true);
            } else if (GlobalClass.runningGame != null && !GlobalClass.runningGame.isRunning && GlobalClass.runningGame.playerList.containsKey(player.getUniqueId())) {
                e.setCancelled(true);
            }
            //臨時
            else if(clickedItem.getType().equals(Material.IRON_HOE)&&GlobalClass.runningGame!=null&&GlobalClass.runningGame.playerList.containsKey(player.getUniqueId())){
                GlobalClass.runningGame.playerList.get(player.getUniqueId()).clickGun=true;
            }
        }
    }

    @EventHandler
    public void PlayerLogout(final PlayerQuitEvent e){
        if(GlobalClass.runningGame==null)return;
        if(GlobalClass.runningGame.playerList.containsKey(e.getPlayer().getUniqueId())) {
            if (!GlobalClass.runningGame.isRunning) {
                GlobalClass.runningGame.playerList.remove(e.getPlayer().getUniqueId());
            } else if (!GlobalClass.runningGame.deadPlayerList.contains(e.getPlayer().getUniqueId())) {
                GlobalClass.runningGame.playerList.get(e.getPlayer().getUniqueId()).generatePlayersChest(e.getPlayer());
                e.getPlayer().getInventory().clear();
                GlobalClass.runningGame.deadPlayerList.add(e.getPlayer().getUniqueId());
                e.getPlayer().setGameMode(GameMode.SPECTATOR);
                e.getPlayer().getServer().broadcast(e.getPlayer().getName() + "はログアウトしたため死亡扱いとなります", Server.BROADCAST_CHANNEL_USERS);
                if (GlobalClass.runningGame.playerList.size() <= GlobalClass.runningGame.deadPlayerList.size() + 1) {
                    GlobalClass.runningGame.endGame();
                }
            }
        }
    }

    @EventHandler
    public void GenerateCarePackage(EntityRemoveFromWorldEvent e){
        if(GlobalClass.runningGame!=null&&GlobalClass.runningGame.isRunning&&Objects.equals(e.getEntity().getCustomName(), "carepackage")) {
            GlobalClass.runningGame.playGround.generateCarePackage(e.getEntity().getLocation().add(0,1,0));
            GlobalClass.runningGame.playGround.carePackageLocation.add(e.getEntity().getLocation().add(0,1,0));
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
    public void RightClickEvent(final PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (GlobalClass.runningGame == null) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) && !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if(e.getClickedBlock()!=null) {
            if (e.getClickedBlock().getState() instanceof Chest) {
                if (((Chest) e.getClickedBlock().getState()).getCustomName() != null) {
                    if (GlobalClass.runningGame.playerList.containsKey(UUID.fromString(((Chest) e.getClickedBlock().getState()).getCustomName()))) {
                        e.getPlayer().openInventory(GlobalClass.runningGame.playerList.get(UUID.fromString(((Chest) e.getClickedBlock().getState()).getCustomName())).inv);
                        e.setCancelled(true);
                    }
                }
            }
        }
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
        meta.displayName(Component.text("§a中心座標まであと§b"+Math.round(l)+"M "+string));
        item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);
    }

    @EventHandler//プレイヤーが死んだら生存人数のやつとかなんとかをいじって、終了ならスレッド止めてなんやかんやしましょう
    public void PlayerKilledEvent(PlayerDeathEvent e) {
        if (GlobalClass.runningGame != null && GlobalClass.runningGame.isRunning && GlobalClass.runningGame.playerList.containsKey(e.getEntity().getPlayer().getUniqueId())) {
            GlobalClass.runningGame.deadPlayerList.add(e.getEntity().getPlayer().getUniqueId());
            GlobalClass.runningGame.playerList.get(e.getEntity().getPlayer().getUniqueId()).generatePlayersChest(e.getEntity().getPlayer());
            e.getEntity().getInventory().clear();
            e.getEntity().getPlayer().setGameMode(GameMode.SPECTATOR);
            if (e.getEntity().getKiller() != null) {
                Player killer = e.getEntity().getKiller();
                if (GlobalClass.runningGame.playerList.containsKey(killer.getUniqueId())) {
                    GlobalClass.runningGame.playerList.get(killer.getUniqueId()).killCount = GlobalClass.runningGame.playerList.get(killer.getUniqueId()).killCount + 1;
                    GlobalClass.runningGame.spectatorList.put(e.getEntity().getUniqueId(), killer.getUniqueId());
                    GlobalClass.runningGame.playerList.get(killer.getUniqueId()).watchedList.add(e.getEntity().getUniqueId());
                    for (UUID uuid : GlobalClass.runningGame.playerList.get(e.getEntity().getPlayer().getUniqueId()).watchedList) {
                        GlobalClass.runningGame.playerList.get(killer.getUniqueId()).watchedList.add(uuid);
                        GlobalClass.runningGame.spectatorList.put(uuid, killer.getUniqueId());
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.getGameMode() == GameMode.SPECTATOR) {
                            player.setSpectatorTarget(killer);
                        }
                    }
                }
            } else {
                Player killer = null;
                for (UUID uuid : GlobalClass.runningGame.spectatorList.keySet()) {
                    if (uuid != e.getEntity().getUniqueId() && !GlobalClass.runningGame.deadPlayerList.contains(uuid)) {
                        killer = Bukkit.getPlayer(uuid);
                        break;
                    }
                }
                GlobalClass.runningGame.spectatorList.put(e.getEntity().getUniqueId(), killer.getUniqueId());
                GlobalClass.runningGame.playerList.get(killer.getUniqueId()).watchedList.add(e.getEntity().getUniqueId());
                for (UUID uuid : GlobalClass.runningGame.playerList.get(e.getEntity().getPlayer().getUniqueId()).watchedList) {
                    GlobalClass.runningGame.playerList.get(killer.getUniqueId()).watchedList.add(uuid);
                    GlobalClass.runningGame.spectatorList.put(uuid, killer.getUniqueId());
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.getGameMode() == GameMode.SPECTATOR) {
                        player.setSpectatorTarget(killer);
                    }
                }
            }
            if (GlobalClass.runningGame.playerList.size() <= GlobalClass.runningGame.deadPlayerList.size() + 1) {
                GlobalClass.runningGame.endGame();
            }
        }
    }

    @EventHandler
    public void ProtectDropShip(ProjectileCollideEvent e){
        if(e.getEntity().getCustomName()!=null&&e.getEntity().getCustomName().equals("dropship")){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerRespawn(PlayerRespawnEvent e){
        if(GlobalClass.runningGame!=null&&GlobalClass.runningGame.deadPlayerList.contains(e.getPlayer().getUniqueId())){
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
                e.getPlayer().setSpectatorTarget(Bukkit.getEntity(GlobalClass.runningGame.spectatorList.get(e.getPlayer().getUniqueId())));

        }
    }

    @EventHandler
    public void DeleteElytra(final EntityToggleGlideEvent e){
        if(GlobalClass.runningGame!=null&&e.getEntity().getType().equals(EntityType.PLAYER)){
            Player player=(Player) e.getEntity();
            if(player.isGliding()) {
                if (GlobalClass.runningGame.isRunning) {
                    if (getDisplayName(player.getInventory().getChestplate()).equals("降下用エリトラ")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1, 200));
                        player.getInventory().clear();
                        player.getInventory().addItem(new ItemStack(Material.COMPASS,1));
                        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD,1));
                    }
                }
            }
        }
    }

    @EventHandler
    public void runchange(PlayerToggleSprintEvent e){
        if (GlobalClass.runningGame!=null && GlobalClass.runningGame.isRunning){
            if (e.getPlayer().isSneaking() && e.isSprinting()){
                e.getPlayer().setSneaking(false);
            }
            if (!e.getPlayer().isSneaking() && !e.isSprinting()){
                e.getPlayer().setSneaking(true);
            }
        }
    }

    @EventHandler
    public void sneak(PlayerToggleSneakEvent e){
        if (GlobalClass.runningGame!=null && GlobalClass.runningGame.isRunning){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void breakcrops(PlayerInteractEvent e){
        if (GlobalClass.runningGame!=null && GlobalClass.runningGame.isRunning){
            if (e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType().equals(Material.FARMLAND)){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void spectatingEvent(PlayerStopSpectatingEntityEvent e){
        if(GlobalClass.runningGame!=null&&GlobalClass.runningGame.isRunning&&GlobalClass.runningGame.deadPlayerList.contains(e.getPlayer().getUniqueId())){
            e.setCancelled(true);
        }
    }

    
}
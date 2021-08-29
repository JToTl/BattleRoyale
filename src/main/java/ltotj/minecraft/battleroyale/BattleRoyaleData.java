package ltotj.minecraft.battleroyale;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTTileEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.*;


import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleRoyaleData{

    HashMap<UUID,PlayerData> playerList=new HashMap<>();
    List<UUID> deadPlayerList=new ArrayList<>();//死んだプレイヤーのリスト
    FileConfiguration fieldConfig;//フィールドの設定書いたconfig
    PlayGround playGround;//フィールドの処理が色々書いてあるクラス
    RunBattleRoyale runBattleRoyale=new RunBattleRoyale();//バトロワ実行スレッド
    boolean isRunning=false,isEnd=false,isGenerated=false;
    double probability;//チェストの生成率
    private final Plugin instance =Main.getPlugin(Main.class);//こんふぃぐよう
    World world;//バトロワが行われるワールド
    String itemfilename,mode="";//使うルートテーブルとモードの指定
    Random random=new Random();//ランダム用
    int sumRandomWeight=0,maxTier=0,maxGuns=2,reductionTimes;
    BossBar bossBar=Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID, BarFlag.CREATE_FOG);
    UUID winner;
    HashMap<UUID,UUID> spectatorList=new HashMap<>();

    class PlayerData {
        UUID uuid;
        String name;
        int killCount = 0,guns=0;
        boolean clickGun=false;
        Location[] deadLocations = new Location[2];
        Material[] deadLocationMaterials=new Material[2];
        BlockData[] deadLocationBlockData=new BlockData[2];
        Inventory inv;
        List<UUID> watchedList=new ArrayList<>();

        public void generatePlayersChest(Player player){
            deadLocations[0]=player.getLocation();
            deadLocations[1]=player.getLocation().add(0,1,0);
            deadLocationBlockData[0]=deadLocations[0].getBlock().getBlockData();
            deadLocationBlockData[1]=deadLocations[1].getBlock().getBlockData();
            inv.setContents(player.getInventory().getContents());
            deadLocations[0].getBlock().setType(Material.CHEST);
            Chest chest=(Chest) deadLocations[0].getBlock().getState();
            chest.setCustomName(String.valueOf(uuid));
            chest.update();
            deadLocations[1].getBlock().setType(Material.PLAYER_HEAD);
            Block skullBlock =deadLocations[1].getBlock();
            skullBlock.setType(Material.PLAYER_HEAD);
            BlockState state = skullBlock.getState();
            Skull skull = (Skull) state;
            skull.setOwningPlayer(player);
            skull.update();
        }

        private void resetBlock(){
            if(deadLocations[0]!=null){
                deadLocations[0].getBlock().setBlockData(deadLocationBlockData[0]);
                deadLocations[1].getBlock().setBlockData(deadLocationBlockData[1]);
            }
        }

        PlayerData(Player player){
            uuid=player.getUniqueId();
            name=player.getName();
            inv= Bukkit.createInventory(null, 45, Component.text(player.getName() + "のインベントリ"));
        }
    }

    class PlayGround {//これクラスとして作らなくてよくな〜い？
        double currentwidth,nextwidth;
        double[] currentCenter=new double[2],nextCenter=new double[2];
        boolean isNarrowingArea=false,spawnableCarePackage=false;
        List<Location> carePackageLocation=new ArrayList<>(),playersChestLocation=new ArrayList<>();
        List<UUID> ejectEntityList=new ArrayList<>();
        NarrowArea narrowArea;
        CarePackageThread carePackageThread=new CarePackageThread();

        PlayGround(){
            currentCenter[0]= fieldConfig.getDouble("firstCenter.X");
            nextCenter[0]=currentCenter[0];
            currentCenter[1]=fieldConfig.getDouble("firstCenter.Z");
            nextCenter[1]=currentCenter[1];
            currentwidth=fieldConfig.getDouble("firstWidth");
            nextwidth=currentwidth;
        }

        private void removeEntities() {
            for(UUID uuid:ejectEntityList){
                Entity entity=Bukkit.getEntity(uuid);
                if(entity!=null)entity.remove();
            }
        }

        private void removeCarePackage(){
            for(Location location:carePackageLocation){
                if(location.getBlock().getState() instanceof Chest){
                    Chest chest= (Chest) location.getBlock().getState();
                    chest.getInventory().clear();
                    location.getBlock().setType(Material.AIR);
                }
            }
        }

        private void removeLootChest(){
            for(String str:fieldConfig.getConfigurationSection("chestPosition").getKeys(false)){
                Location removelocation=new Location(world, fieldConfig.getDouble("chestPosition." + str + ".X"), fieldConfig.getDouble("chestPosition." + str + ".Y"), fieldConfig.getDouble("chestPosition." + str + ".Z"));
                if(removelocation.getBlock().getState() instanceof Chest){
                    Chest chest= (Chest) removelocation.getBlock().getState();
                    chest.getInventory().clear();
                    removelocation.getBlock().setType(Material.AIR);
                }
            }
        }

        private void removePlayersChest(){
            for(PlayerData playerData:playerList.values()){
                if(deadPlayerList.contains(playerData.uuid)){
                    playerData.resetBlock();
                }
            }
        }



        private void removeWorldBorder(){
            world.getWorldBorder().setSize(1000000);
        }

        private Location generateShipRoute(){//現在のエリアの辺上のうちの一点から中心に向かって１進めた場所をlocationとして返す
            Location rlocation=new Location(world,currentCenter[0],fieldConfig.getDouble("dropShipAltitude"),currentCenter[1]);
            Double l=nextwidth*(((Math.pow(2,0.5))-1)*random.nextDouble() +1);
            int ponx,ponz=1;
            int randomint=random.nextInt(8);
            ponx= (int) Math.pow(-1,randomint);
            if(randomint<3.5)ponz=-1;
            if((randomint-1)%4<2.5) rlocation.add(ponx*nextwidth,0,ponz*Math.sqrt(l*l-nextwidth*nextwidth));
            else rlocation.add(ponx*Math.sqrt(l*l-nextwidth*nextwidth),0,ponz*nextwidth);
            rlocation.setDirection(new Vector(currentCenter[0]-rlocation.getX(),0,currentCenter[1]-rlocation.getZ()));
            rlocation.setDirection(new Vector(rlocation.getDirection().getX()/rlocation.getDirection().length()/2,0,rlocation.getDirection().getZ()/rlocation.getDirection().length()/2));
            rlocation.add(rlocation.getDirection().getX()/rlocation.getDirection().length(),0,rlocation.getDirection().getZ()/rlocation.getDirection().length());
            return rlocation;
        }

        private void setWorldBorder(){//最初につかう
            setWBCenter(currentCenter[0],currentCenter[1]);
            setWBSize(2*currentwidth);
            setWBBuffer(10);
            setWBDamage(0.01);
        }

        private void newCenterPosition(double p){//今の中心座標から、新しい中心座標をランダム生成
            final Random random = new Random();
            nextCenter[0]=currentCenter[0]+(random.nextDouble()-0.5)*currentwidth*(1-p);
            nextCenter[1]=currentCenter[1]+(random.nextDouble()-0.5)*currentwidth*(1-p);
            if(p*currentwidth>1)nextwidth=p*currentwidth;
            else nextwidth=1;
        }

        private void doNarrowArea(int t){
            if(isNarrowingArea)return;
            narrowArea=new NarrowArea(t);
            narrowArea.start();
        }

        public void putLootChest() {//生成率に応じて個別にチェストが生成されるかどうかの判定を行う
            if(isGenerated)return;
            isGenerated=true;
            Set<String> chestPosition = fieldConfig.getConfigurationSection("chestPosition").getKeys(false);
            for (String c : chestPosition) {
                if (random.nextDouble() < probability) {
                    generateLootChestFromDatapack(c);
                }
            }
        }

        private void generateLootChestFromDatapack(String str) {//battleroyaleというデータパックにあるルートテーブルを設定する
            Location location = new Location(world, fieldConfig.getDouble("chestPosition." + str + ".X"), fieldConfig.getDouble("chestPosition." + str + ".Y"), fieldConfig.getDouble("chestPosition." + str + ".Z"));
            int tier = fieldConfig.getInt("chestPosition." + str + ".Tier");
            if (tier == 0 || mode.equals("random")) {
                double decisionTierDouble = random.nextDouble() * sumRandomWeight;
                int decisionTierInt = 0;
                for (int i = 1; i < sumRandomWeight + 1; i++) {
                    decisionTierInt = decisionTierInt + fieldConfig.getInt("randomTierWeight.Tier" + i);
                    if (decisionTierInt >= decisionTierDouble) {
                        tier = i;
                        break;
                    }
                }
            }
            location.getBlock().setType(Material.CHEST);
            NBTTileEntity nbtTileEntity = new NBTTileEntity(location.getBlock().getState());
            nbtTileEntity.mergeCompound(new NBTContainer("{LootTable:\"battleroyalepack:" + itemfilename + "/tier" + tier + "\"}"));
        }

        private void spawnCarePackage(){
            Location location=new Location(world,nextCenter[0]+nextwidth*(0.5-random.nextDouble()),250,nextCenter[1]+nextwidth*(0.5-random.nextDouble()));
            location.setDirection(new Vector(0,-1,0));
            Entity entity=world.spawnEntity(location,EntityType.WITHER_SKULL),carechest=world.spawnEntity(location,EntityType.ARMOR_STAND);
            WitherSkull witherSkull=(WitherSkull)entity;
            witherSkull.setInvulnerable(true);
            carechest.setInvulnerable(true);
            witherSkull.setCharged(true);
            witherSkull.setCustomName("carepackage");
            witherSkull.setVelocity(new Vector(0,-0.03,0));
            carechest.setCustomName("subcarepackage");
            witherSkull.addPassenger(carechest);
            NBTEntity chestEntity=new NBTEntity(carechest);
            chestEntity.mergeCompound(new NBTContainer("{ActiveEffects:[{Id:24,Amplifier:1,Duration:100000000,ShowParticles:0b}]}"));
            ejectEntityList.add(entity.getUniqueId());
            ejectEntityList.add(carechest.getUniqueId());
        }

        public void generateCarePackage(Location location){
            int tier = fieldConfig.getInt("carePackage.Tier");
            if (tier == 0 || mode.equals("random")) {
                double decisionTierDouble = random.nextDouble() * sumRandomWeight;
                int decisionTierInt = 0;
                for (int i = 1; i < sumRandomWeight + 1; i++) {
                    decisionTierInt = decisionTierInt + fieldConfig.getInt("randomTierWeight.Tier" + i);
                    if (decisionTierInt >= decisionTierDouble) {
                        tier = i;
                        break;
                    }
                }
            }
            location.getBlock().setType(Material.CHEST);
            NBTTileEntity nbtTileEntity = new NBTTileEntity(location.getBlock().getState());
            nbtTileEntity.mergeCompound(new NBTContainer("{LootTable:\"battleroyalepack:" + itemfilename + "/tier" + tier + "\"}"));
        }

        public void generateShip(){
            int count=0;
            Location location=generateShipRoute(),tplocation=location.add(0,-3,0);
            Vector vector=location.getDirection().crossProduct(new Vector(0,1,0));
            vector.setX(vector.getX()/vector.length());
            vector.setZ(vector.getZ()/vector.length());
            for(UUID uuid:playerList.keySet()){
                Objects.requireNonNull(Bukkit.getPlayer(uuid)).teleport(tplocation);
                if(count%2==0){
                    location.add(vector);
                    Entity entity=world.spawnEntity(location,EntityType.WITHER_SKULL);
                    entity.setCustomName("dropship");
                    entity.setInvulnerable(true);
                    entity.addPassenger(Objects.requireNonNull(Bukkit.getPlayer(uuid)));
                    ejectEntityList.add(entity.getUniqueId());
                }
                else{
                    location.add(-2*vector.getX(),0,-2*vector.getZ());
                    Entity entity=world.spawnEntity(location,EntityType.WITHER_SKULL);
                    entity.setCustomName("dropship");
                    entity.setInvulnerable(true);
                    entity.addPassenger(Objects.requireNonNull(Bukkit.getPlayer(uuid)));
                    location.add(location.getDirection().getX()/location.getDirection().length(),0,location.getDirection().getZ()/location.getDirection().length());
                    location.add(vector);
                    ejectEntityList.add(entity.getUniqueId());
                }
                count=count+1;
            }
        }

        class CarePackageThread extends BukkitRunnable {

            @Override
            public void run() {//ケアパケ投下用
                if (spawnableCarePackage&&random.nextDouble()<fieldConfig.getDouble("carePackage.rate")) {
                    spawnCarePackage();
                }
            }
        }

        class NarrowArea extends Thread{

            AtomicBoolean flag=new AtomicBoolean(true);
            int time;//秒で指定

            NarrowArea(int t){
                time=t;
            }

            @Override//エリア縮小のためのスレッド
            public void run(){
                if(isNarrowingArea)return;
                isNarrowingArea=true;

                setWBSize(2*nextwidth,time);
                //world.getWorldBorder().setSize(2*nextwidth,time);

                for(int i=0;i<time*20&&flag.get()&&currentwidth>1;i++){
                    if((time*20-i)%20==0){
                        setBossBar(time,time-(i/20),"エリア縮小終了");
                    }
                    if(i%20==0)setWBCenter(currentCenter[0]+i*(nextCenter[0]-currentCenter[0])/time/20,currentCenter[1]+i*(nextCenter[1]-currentCenter[1])/time/20);
                    //world.getWorldBorder().setCenter(currentCenter[0]+i*(nextCenter[0]-currentCenter[0])/time/20,currentCenter[1]+i*(nextCenter[1]-currentCenter[1])/time/20);
                    threadSleep(50);
                    //world.getWorldBorder().setSize(2*currentwidth*(1+(((nextwidth/currentwidth)-1)*i/time/20)));
                }
                if(nextwidth==1)setWBCenter(currentCenter[0]+5000,currentCenter[1]+5000);//world.getWorldBorder().setCenter(currentCenter[0]+5000,currentCenter[1]+5000);
                currentwidth=nextwidth;
                currentCenter[0]=nextCenter[0];
                currentCenter[1]=nextCenter[1];
                isNarrowingArea=false;
            }
        }
    }

    private void setWBBuffer(double buffer) {
        Bukkit.getScheduler().runTask(instance, new Runnable() {
                    public void run() {
                        world.getWorldBorder().setDamageBuffer(buffer);
                    }
                }
        );
    }

   private void setWBDamage(double damage){
        Bukkit.getScheduler().runTask(instance,new Runnable(){
            @Override
            public void run(){
                world.getWorldBorder().setDamageAmount(damage);
            }
        });
   }

    private void setWBCenter(double X,double Z) {
        Bukkit.getScheduler().runTask(instance,new Runnable(){
            @Override
            public void run(){
                world.getWorldBorder().setCenter(X,Z);
            }
        });
    }

    private void setWBSize(double width,long time){//widthは一辺の長さ
        Bukkit.getScheduler().runTask(instance,new Runnable(){
            @Override
            public void run(){
                world.getWorldBorder().setSize(width,time);
            }
        });
    }

    private void setWBSize(double width){//widthは一辺の長さ
        Bukkit.getScheduler().runTask(instance,new Runnable(){
            @Override
            public void run(){
                world.getWorldBorder().setSize(width);
            }
        });
    }

    void scoreboard(World world){
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Bukkit.getScheduler().runTaskTimer(instance, Runnable->{
            if (!isRunning)return;
            for (UUID uuid : playerList.keySet()){
                Player p = Bukkit.getPlayer(uuid);
                Location location=new Location(GlobalClass.runningGame.world,GlobalClass.runningGame.playGround.nextCenter[0],100,GlobalClass.runningGame.playGround.nextCenter[1]),
                        plocation=p.getLocation();
                p.setCompassTarget(location);
                double d,l=Math.round(10.0*Math.sqrt(Math.pow(plocation.getX()-location.getX(),2)+Math.pow(plocation.getZ()-location.getZ(),2)))*0.1;
                d = Math.max(Math.abs(plocation.getX() - location.getX()), Math.abs(plocation.getZ() - location.getZ()));

                String string;
                if(d<=GlobalClass.runningGame.playGround.nextwidth)string="§dエリア内に滞在中";
                else string="§aエリア内まであと§b"+Math.round(10*l*(d-GlobalClass.runningGame.playGround.nextwidth)/d)/10+"M";

                Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
                Objective objective = scoreboard.registerNewObjective("§d§lBattleRoyale","Dummy",Component.text("Battle"));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                objective.getScore("§6§l残り人数 : " + (playerList.size()-deadPlayerList.size()) + "人").setScore(0);
                objective.getScore("§a§l中心座標まであと§b§l"+Math.round(l)+"M").setScore(-1);
                objective.getScore(string).setScore(-2);
                objective.getScore("§b§lマップ : " + world.getName()).setScore(-3);
                p.setScoreboard(scoreboard);
            }

        },0,20);
    }

    class RunBattleRoyale extends Thread{//バトロワ実行の主要スレッド

        AtomicBoolean flag=new AtomicBoolean(true);

        private void joinNarrowArea(){
            try {
                playGround.narrowArea.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run(){
            isRunning=true;
            for(UUID uuid:playerList.keySet()){
                Player player=Bukkit.getPlayer(uuid);
                if(player==null)continue;
                player.getInventory().setArmorContents(new ItemStack[]{null,null,createCustomItem(Material.ELYTRA,"降下用エリトラ","着地すると消滅します"),null});
            }
            playGround.setWorldBorder();
            threadSleep(1000*fieldConfig.getInt("firstAreaWaitTime"));
            for(int i=1;i<reductionTimes+1&&flag.get();i++){
                playGround.spawnableCarePackage=fieldConfig.getBoolean("areaReduction."+i+".spawnableCarePackage");
                setWBBuffer(fieldConfig.getDouble("areaReduction."+i+".areaDamageBuffer"));
                setWBDamage(fieldConfig.getDouble("areaReduction."+i+".areaDamage"));
                //world.getWorldBorder().setDamageBuffer(fieldConfig.getDouble("areaReduction."+i+".areaDamageBuffer"));
                //world.getWorldBorder().setDamageAmount(fieldConfig.getDouble("areaReduction."+i+".areaDamage"));
                playGround.newCenterPosition(fieldConfig.getDouble("areaReduction."+i+".reductionRate"));
                bossBar.setColor(BarColor.GREEN);
                for(int j=fieldConfig.getInt("areaReduction."+i+".waitTime");j>0&&flag.get();j--){
                    setBossBar(fieldConfig.getInt("areaReduction."+i+".waitTime"),j,"エリア縮小");
                    threadSleep(1000);
                }
                if(flag.get()) {
                    bossBar.setColor(BarColor.RED);
                    playGround.doNarrowArea(fieldConfig.getInt("areaReduction." + i + ".executeTime"));
                    joinNarrowArea();
                }
            }
        }
    }

    private void setBossBar(int t,int s,String string){
        bossBar.setTitle(string+"まで残り"+s+"秒");
        bossBar.setProgress((double)s/t);
    }

    private void threadSleep(int t){
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void endGame(){//メインスレッドから呼び出す
        interruptThreads();
        if(playerList.size()==deadPlayerList.size()+1){
            for(UUID uuid:playerList.keySet()){
                if(!deadPlayerList.contains(uuid)){
                    broadcastMessage("§l§6"+Bukkit.getPlayer(uuid).getName()+"がバトルロワイヤルを制しました！");
                    winner=uuid;
                    Bukkit.getPlayer(uuid).getInventory().clear();
                    break;
                }
            }
        }
        bossBar.removeAll();
        playGround.removeWorldBorder();
        playGround.removePlayersChest();
        playGround.removeCarePackage();
        playGround.removeLootChest();
        isRunning=false;
        isEnd=true;
        playGround.removeEntities();
    }

    public void preGameStart() {
        playGround.generateShip();
        setPlayerStatus();
        playGround.putLootChest();
//        for (Player p : Bukkit.getOnlinePlayers()) {
//            if (!playerList.containsKey(p.getUniqueId()) && !p.isOp()) p.setGameMode(GameMode.SPECTATOR);
//        }
        broadcastMessage("§3バトルロワイヤルが始まりました！最後の一人を賭けて戦いましょう！");
        broadcastMessage("§3参加人数" + LivingPlayers() + "人");
    }

    private void interruptThreads(){
        if(playGround.narrowArea!=null){
            playGround.narrowArea.flag.set(false);
        }
        runBattleRoyale.flag.set(false);
        playGround.carePackageThread.cancel();
    }

    public boolean putParticipant (Player p){
        if(playerList.containsKey(p.getUniqueId())){
            return false;
        }
        playerList.put(p.getUniqueId(),new PlayerData(p));
        return true;
    }

    public void setPlayerStatus(){
        for(UUID uuid:playerList.keySet()){
            if(Bukkit.getPlayer(uuid)==null)continue;
            Player p=Bukkit.getPlayer(uuid);
            p.setGameMode(GameMode.ADVENTURE);
            p.setFoodLevel(20);
            bossBar.addPlayer(p);
            //移動させたあとに実行p.getInventory().setArmorContents(new ItemStack[]{null,null,createCustomItem(Material.ELYTRA,"降下用エリトラ","着地すると消滅します"),null});
            //p.setHealth(fieldConfig.getInt("playerHealth")); なしにしましょう
            for(PotionEffect potion:p.getActivePotionEffects()){
                p.removePotionEffect(potion.getType());
                p.getInventory().clear();
                p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL,500,20));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL,500,20));
            }
        }
    }

    public int LivingPlayers(){
        int r=0;
        for(UUID uuid:playerList.keySet()){
            if(!deadPlayerList.contains(uuid))r=r+1;
        }
        return r;
    }

    public ItemStack createCustomItem (final Material material, final String name, final String... lore){//デバッグのためpublic
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name));

        meta.lore(listToComponent(lore));

        item.setItemMeta(meta);

        return item;
    }


    public void removeParticipant(Player p){
        playerList.remove(p.getUniqueId());
    }

    private List<Component> listToComponent(String... lore){
        List<Component> components=new ArrayList<>();
        for (String s : lore) components.add(Component.text(s));
        return components;
    }

    private void broadcastMessage(String str){
        world.sendMessage(Component.text(str));
    }

    public void broadcastRanking(){
        if(winner==null||!isEnd)return;
        broadcastMessage("§c一位：§f"+playerList.get(winner).name);
        if(deadPlayerList.size()<1)return;
        broadcastMessage("§d二位：§f"+playerList.get(deadPlayerList.get(deadPlayerList.size()-1)).name);
        if (deadPlayerList.size()<2)return;
        broadcastMessage("§e三位：§f"+playerList.get(deadPlayerList.get(deadPlayerList.size()-2)).name);
    }

    BattleRoyaleData(String fieldName,String itemsName){
        fieldConfig=new CustomConfig(instance,fieldName).getConfig();
        playGround=new PlayGround();
        itemfilename=itemsName;
        probability=fieldConfig.getDouble("generatingRate");
        reductionTimes=fieldConfig.getConfigurationSection("areaReduction").getKeys(false).size();
        bossBar.removeFlag(BarFlag.CREATE_FOG);
        world=Bukkit.getWorld(fieldConfig.getString("world"));
        for(String string:fieldConfig.getConfigurationSection("randomTierWeight").getKeys(false)){
            sumRandomWeight=sumRandomWeight+fieldConfig.getInt("randomTierWeight."+string);
            maxTier=maxTier+1;
        }
    }
}
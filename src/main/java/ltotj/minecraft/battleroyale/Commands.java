package ltotj.minecraft.battleroyale;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class Commands implements CommandExecutor {

    private final Plugin instance=Main.getPlugin(Main.class);

    private void renamePlayerItem(Player player,String name){
        ItemStack item=player.getInventory().getItemInMainHand();
        ItemMeta meta=item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
    }

    private void broadcastMessage(String message){
        Bukkit.getServer().broadcast(Component.text(message),Server.BROADCAST_CHANNEL_USERS);
    }

    private void checkYml(String str,CommandSender p){
        p.sendMessage("ファイルの構成をチェック中・・・");
        CustomConfig config=new CustomConfig(instance,str);
        config.getConfig();
        if(!config.canGetList("firstCenter")||!config.canGetDouble("firstCenter.X")||!config.canGetDouble("firstCenter.Z")||!config.canGetDouble("firstWidth"))p.sendMessage("初期エリアが指定されていません_※このメッセージは初期エリアの座標に0が含まれる時も表示されます");
        if(!config.canGetString("world"))p.sendMessage("ワールド名が指定されていません");
        else{
            try{Bukkit.getWorld(config.getConfig().getString("world"));}
            catch (Exception exception){
                p.sendMessage("指定されたワールドが存在しません");
            }
        }
        if(!config.canGetDouble("generatingRate")){p.sendMessage("チェスト生成率が指定されていません_※このメッセージは生成率0の時も表示されます");}
        if(!config.canGetList("randomTierWeight")){p.sendMessage("ランダムチェストの重みが指定されていません_※このメッセージは重み0の時も表示されます");}
        else{
            for(int i=1;i<=config.getConfig().getConfigurationSection("randomTierWeight").getKeys(false).size();i++){
                if(!config.canGetInt("randomTierWeight.Tier"+i))p.sendMessage("Tier"+i+"チェストの重みが正しく指定されていません_※このメッセージはTier0の時も表示されます");
            }
        }
        if(!config.canGetList("carePackage"))p.sendMessage("ケアパッケージの設定欄が存在しません");
        else{
          if(!config.canGetInt("carePackage.frequency"))p.sendMessage("ケアパッケージの頻度が指定されていません");
          else if(config.getConfig().getInt("carePackage.frequency")<=5)p.sendMessage("ケアパッケージの頻度が小さすぎて危険です");
          if(!config.canGetInt("carePackage.Tier"))p.sendMessage("ケアパッケージのTierが指定されていません_※このメッセージはTier0の時も表示されます");
          if(!config.canGetDouble("carePackage.rate"))p.sendMessage("ケアパッケージの生成率が指定されていません_※このメッセージは生成率0の時も表示されます");
        }
        if(!config.canGetDouble("dropShipAltitude"))p.sendMessage("ドロップシップの高さが指定されていません");
        if(!config.canGetInt("playerHealth"))p.sendMessage("プレイヤーの初期体力が指定されていません");
        if(!config.canGetInt("firstAreaWaitTime"))p.sendMessage("最初のエリア収縮判定までの時間が指定されていません");
        if(!config.canGetList("areaReduction"))p.sendMessage("エリア収縮の設定欄が存在しません");
        else{
            for(int i=1;i<=config.getConfig().getConfigurationSection("areaReduction").getKeys(false).size();i++){
                if(!config.canGetInt("areaReduction."+i+".waitTime"))p.sendMessage("フェーズ"+i+"のエリア収縮待機時間が指定されていません");
                if(!config.canGetInt("areaReduction."+i+".executeTime"))p.sendMessage("フェーズ"+i+"のエリア収縮実行時間が指定されていません");
                if(!config.canGetDouble("areaReduction."+i+".reductionRate"))p.sendMessage("フェーズ"+i+"のエリア収縮率が指定されていません_※このメッセージは収縮率0の時も表示されます");
                if(!config.canGetBoolean("areaReduction."+i+".spawnableCarePackage"))p.sendMessage("フェーズ"+i+"のケアパケ発生の有無が指定されていません");
                if(!config.canGetDouble("areaReduction."+i+".areaDamage"))p.sendMessage("フェーズ"+i+"のエリアダメージが指定されていません_※このメッセージはダメージ0の時も表示されます");
                if(!config.canGetDouble("areaReduction."+i+".areaDamageBuffer"))p.sendMessage("フェーズ"+i+"のエリアバッファーが指定されていません_※このメッセージはバッファー0の時も表示されます");
            }
        }
        p.sendMessage("チェック完了");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0&&sender.hasPermission("op")) {
            switch (args[0]) {
                case "lootchest":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("プレイヤー以外は実行できません");
                        return true;
                    }
                    Player p=(Player)sender;
                    if (args.length < 2) break;
                    else if (GlobalClass.editedConfig == null) {
                        p.sendMessage("設定するファイルを選択してください /bbattleroyale editfield <ファイル名(.ymlまで)>");
                        return true;
                    }
                    switch (args[1]) {
                        case "delete":
                            if (p.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                                p.sendMessage("なんでもいいんでアイテム持ってください");
                                return true;
                            }
                            renamePlayerItem(p, "§c削除");
                            break;
                        case "get":
                            if (args.length < 3) {
                                p.sendMessage("Tierを設定してください");
                                return true;
                            }
                            if (p.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                                p.sendMessage("なんでもいいんでブロック持ってください");
                                return true;
                            }
                            if (!args[2].matches("[+-]?\\d*(\\.\\d+)?")) {
                                p.sendMessage("整数を指定してください");
                                return true;
                            }
                            renamePlayerItem(p, "§aTier" + args[2]);
                            break;
                        case "visible":
                            if (args.length < 3) {
                                p.sendMessage("Tierを設定してください");
                                return true;
                            }
                            for (String str : GlobalClass.editedConfig.getConfig().getConfigurationSection("chestPosition").getKeys(false)) {
                                if (!args[2].equals("all") && !args[2].equals("" + GlobalClass.editedConfig.getConfig().getInt("chestPosition." + str + ".Tier"))) {
                                    continue;
                                }
                                Location visiblelocation = new Location(p.getWorld(), GlobalClass.editedConfig.getConfig().getDouble("chestPosition." + str + ".X"), GlobalClass.editedConfig.getConfig().getDouble("chestPosition." + str + ".Y"), GlobalClass.editedConfig.getConfig().getDouble("chestPosition." + str + ".Z"));
                                visiblelocation.getBlock().setType(Material.CHEST);
                            }
                            p.sendMessage("Tier:" + args[2] + "をvisibleに設定しました");
                            break;
                        case "invisible":
                            for (String str : GlobalClass.editedConfig.getConfig().getConfigurationSection("chestPosition").getKeys(false)) {
                                Location invisiblelocation = new Location(p.getWorld(), GlobalClass.editedConfig.getConfig().getDouble("chestPosition." + str + ".X"), GlobalClass.editedConfig.getConfig().getDouble("chestPosition." + str + ".Y"), GlobalClass.editedConfig.getConfig().getDouble("chestPosition." + str + ".Z"));
                                invisiblelocation.getBlock().setType(Material.AIR);
                            }
                            break;
                        case "check":
                            if (args.length < 3) {
                                p.sendMessage("Tierを設定してください");
                                return true;
                            }
                            int count=0;
                            for (String str : GlobalClass.editedConfig.getConfig().getConfigurationSection("chestPosition").getKeys(false)) {
                                if (!args[2].equals("all") && !args[2].equals("" + GlobalClass.editedConfig.getConfig().getInt("chestPosition." + str + ".Tier"))) {
                                    continue;
                                }
                                count+=1;
                            }
                            p.sendMessage("Tier:" + args[2] + "のチェストは"+count+"個設定されています");
                    }
                    break;
                case "editfield":
                    if (args.length < 2) {
                        sender.sendMessage("ファイルを指定してください");
                        return true;
                    }
                    try {
                        GlobalClass.editedConfig = new CustomConfig(instance, args[1]);
                        Bukkit.getServer().broadcast(Component.text(args[1] + "をeditfieldとして設定しました"), Server.BROADCAST_CHANNEL_USERS);
                    } catch (NullPointerException e) {
                        sender.sendMessage(args[1] + "は存在しません");
                    }
                    break;
                case "setrate":
                    if (GlobalClass.runningGame == null || GlobalClass.runningGame.isRunning) {
                        sender.sendMessage("準備中のゲームが存在しません");
                        return true;
                    }
                    try {
                        double d = Double.parseDouble(args[1]);
                        if (Math.abs(d - 0.5) > 0.5) sender.sendMessage("0~1の数値を指定してください");
                        else {
                            GlobalClass.runningGame.probability = d;
                            sender.sendMessage("チェストの出現率を" + d + "に設定しました");
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("チェストの出現率は0~1の数値で指定してください");
                    }
                    return true;
                case "cancel":
                    if (GlobalClass.runningGame == null) {
                        sender.sendMessage("ゲームが存在しません");
                    } else if (GlobalClass.runningGame.isRunning) {
                        sender.sendMessage("既にゲームが開始されています 中止したい場合は/battleroyale stop を使用してください");
                        return true;
                    }
                    GlobalClass.runningGame.playGround.removeLootChest();
                    GlobalClass.runningGame = null;
                    Bukkit.getServer().broadcast(Component.text("ゲームが削除されました"),Server.BROADCAST_CHANNEL_USERS);
                    break;
                case "generate":
                    if(GlobalClass.runningGame==null){
                        sender.sendMessage("ゲームが存在しません");
                    }
                    else if(GlobalClass.runningGame.isGenerated){
                        sender.sendMessage("既に生成されています");
                    }
                    else{
                        GlobalClass.runningGame.playGround.putLootChest();
                        GlobalClass.runningGame.playGround.removeItems();
                        sender.sendMessage("チェストを生成しました");
                    }
                    break;
                case "setgame":
                    if (args.length < 2) {
                        sender.sendMessage("/ setgame ステージ アイテムリスト で新規ゲームを開設");
                    } else if (GlobalClass.runningGame != null) {
                        sender.sendMessage("既にゲームが開設されています /battleroyale cancelまたは/battleroyale stopで安全にゲームを閉じてから再設定してください");
                    } else {
                        GlobalClass.runningGame = new BattleRoyaleData(args[1], args[2]);
                        sender.sendMessage("新規ゲーム開設完了");
                        //Bukkit.getServer().broadcast(Component.text("§5バトルロワイヤルが間も無く開催されます！/bat join で参加登録をしましょう！"),Server.BROADCAST_CHANNEL_USERS);
                    }
                    break;
                case "check":
                    if(args.length>1){
                        checkYml(args[1],sender);
                    }
                    break;
                case "guns":
                    if(args.length<3&&GlobalClass.runningGame!=null&&args[1].matches("-?\\\\d+")){
                        GlobalClass.runningGame.maxGuns=Integer.parseInt(args[1]);
                        sender.sendMessage("銃の最大所持数を"+args[1]+"に設定しました");
                    }
                    break;
                case "ban":
                    if (GlobalClass.runningGame != null&&GlobalClass.runningGame.isEnd) {
                        List<String> currentPlayers=Main.participants.getConfig().getStringList("player");
                        for (BattleRoyaleData.PlayerData data : GlobalClass.runningGame.playerList.values()) {
                            if(!Bukkit.getOfflinePlayer(data.uuid).isOp()){
                                Bukkit.getOfflinePlayer(data.uuid).banPlayer("参加済");
                                currentPlayers.add(data.name);
                            }
                        }
                        Main.participants.getConfig().set("player",currentPlayers);
                    }
                    break;
                case "pardon":
                    for(OfflinePlayer player:Bukkit.getBannedPlayers()) {
                        Bukkit.getBanList(BanList.Type.NAME).pardon(player.getName());
                    }
                    break;
                case "start":
                    if (GlobalClass.runningGame == null) {
                        sender.sendMessage("ゲームが設定されていません");
                    } else if (!GlobalClass.runningGame.isRunning) {
                        if(!GlobalClass.runningGame.isGenerated){
                            sender.sendMessage("先に/bat generate を実行してください");
                            return true;
                        }
                        GlobalClass.runningGame.preGameStart();
                        GlobalClass.runningGame.scoreboard();
                        GlobalClass.runningGame.runBattleRoyale.start();
                        GlobalClass.runningGame.scoreboard();
                        GlobalClass.runningGame.playGround.carePackageThread.runTaskTimer(instance, 300, 20 * GlobalClass.runningGame.fieldConfig.getInt("carePackage.frequency"));
                    } else {
                        sender.sendMessage("既にゲームが行われています");
                    }
                    break;
                case "show":
                    if(GlobalClass.runningGame==null){
                        sender.sendMessage("ゲームが設定されていません");
                    }
                    else if(args.length<2){
                        sender.sendMessage("足りないよ！");
                    }
                    else {
                        switch (args[1]) {
                            case "player":
                                sender.sendMessage("参加登録者は以下の通りです");
                                for (BattleRoyaleData.PlayerData playerData : GlobalClass.runningGame.playerList.values()) {
                                    sender.sendMessage(playerData.name);
                                }
                                break;
                            case "ranking":
                                if (GlobalClass.runningGame.isRunning) {
                                    sender.sendMessage("ゲームが終了していません");
                                    break;
                                }
                                GlobalClass.runningGame.broadcastRanking();
                                break;
                            case "killranking":
                                break;
                        }
                    }
                    break;
                case "stop":
                    if (GlobalClass.runningGame == null || !GlobalClass.runningGame.isRunning) {
                        sender.sendMessage("バトルロワイヤルはただいま開催されていません");
                        break;
                    }
                    GlobalClass.runningGame.endGame();
                    GlobalClass.runningGame.playGround.removeLootChest();
                    GlobalClass.runningGame = null;
                    sender.sendMessage("バトルロワイヤルを中断しました");
                    break;
                case "kick":
                    List<String> players=Main.participants.getConfig().getStringList("player");
                    for(Player player:Bukkit.getServer().getOnlinePlayers()){
                        if(players.contains(player.getName())&&!player.isOp()){
                            player.kick(Component.text("既に参加済みです"));
                        }
                    }
                    break;
                case "saveplayer":
                    List<String> currentPlayers=Main.participants.getConfig().getStringList("player");
                    for(BattleRoyaleData.PlayerData playerData:GlobalClass.runningGame.playerList.values()){
                        currentPlayers.add(playerData.name);
                    }
                    Main.participants.getConfig().set("player",currentPlayers);
                    break;
                case "joinalpl":
                    if(GlobalClass.runningGame==null||GlobalClass.runningGame.isRunning||GlobalClass.runningGame.isEnd){
                        sender.sendMessage("ゲームが存在しないか、既に開始されています");
                    }
                    else{
                     for(Player player:Bukkit.getServer().getOnlinePlayers()){
                         if(player.isOp())continue;
//                         Location location=player.getLocation();
//                         if(location.clone().add(0,-1,0).getBlock().getType().equals(Material.DIAMOND_BLOCK)||location.clone().add(0,-2,0).getBlock().getType().equals(Material.DIAMOND_BLOCK)){
                             player.getInventory().clear();
                             GlobalClass.runningGame.putParticipant(player);
                             player.sendMessage("§eゲームへの参加登録が完了しました!");
//                         }
                        }
                    }
                    break;
                case "spec":
                    Bukkit.getPlayer(args[1]).setSpectatorTarget(Bukkit.getPlayer(args[2]));
                    break;
            }
        }
        if (!(sender instanceof Player)) {
            return true;
        }
        Player p=(Player)sender;
        switch (args[0]) {
            case "join":
                if (GlobalClass.runningGame == null) {
                    p.sendMessage("ただいまゲームは開催されていません");
                } else if (GlobalClass.runningGame.isRunning) {
                    p.sendMessage("既にゲームが開始されています");
                }else if (GlobalClass.runningGame.putParticipant(p)) {
                    p.sendMessage("参加者登録完了");
                    p.getInventory().clear();
                    Bukkit.getServer().broadcast(Component.text(p.getName()+"が参加者登録しました"),Server.BROADCAST_CHANNEL_USERS);
                } else {
                    p.sendMessage("既に参加者登録されています");
                }
                break;
            case "leave":
                if (GlobalClass.runningGame != null&&GlobalClass.runningGame.playerList.containsKey(p.getUniqueId())) {
                    GlobalClass.runningGame.removeParticipant(p);
                    p.sendMessage("参加登録を解除しました");
                    Bukkit.getServer().broadcast(Component.text(p.getName()+"が参加者登録を解除しました"),Server.BROADCAST_CHANNEL_USERS);
                }
                break;
            case "survivor":
                if(GlobalClass.runningGame!=null) sender.sendMessage("残り人数"+GlobalClass.runningGame.LivingPlayers()+"人");
                break;
            case "kansen":
                if(GlobalClass.runningGame!=null&&GlobalClass.runningGame.isRunning&& GlobalClass.runningGame.spectatorList.containsKey(((Player) sender).getUniqueId())){
                    Player player=Bukkit.getPlayer(GlobalClass.runningGame.spectatorList.get(((Player) sender).getUniqueId()));
                    if(player!=null){
                        ((Player)sender).setGameMode(GameMode.SPECTATOR);
                        ((Player)sender).setSpectatorTarget(player);
                    }
                    else{
                        sender.sendMessage("えらー");
                    }
                }
        }
        return true;
    }
}
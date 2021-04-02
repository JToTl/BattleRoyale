package battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;


public class Commands implements CommandExecutor {

    private final Plugin instance=Main.getPlugin(Main.class);

    private void renamePlayerItem(Player player,String name){
        ItemStack item=player.getInventory().getItemInMainHand();
        ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("プレイヤー以外は実行できません");
            return true;
        }
        Player p=(Player) sender;
        if (args.length != 0&&p.hasPermission("op")) {
            switch (args[0]) {
                case "lootchest":
                    if (args.length < 2) break;
                    else if (GlobalClass.editedConfig == null) {
                        p.sendMessage("設定するファイルを選択してください /bbattleroyale editfield <ファイル名(.ymlまで)>");
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
                    }
                    break;
                case "editfield":
                    if (args.length < 2) {
                        p.sendMessage("ファイルを指定してください");
                        return true;
                    }
                    try {
                        GlobalClass.editedConfig = new CustomConfig(instance, args[1]);
                        Bukkit.getServer().broadcastMessage(args[1] + "をeditfieldとして設定しました");
                    } catch (NullPointerException e) {
                        p.sendMessage(args[1] + "は存在しません");
                    }
                    break;
                case "setrate":
                    if (GlobalClass.runningGame == null || GlobalClass.runningGame.isRunning) {
                        p.sendMessage("準備中のゲームが存在しません");
                        return true;
                    }
                    try {
                        double d = Double.parseDouble(args[1]);
                        if (Math.abs(d - 0.5) > 0.5) p.sendMessage("0~1の数値を指定してください");
                        else {
                            GlobalClass.runningGame.probability = d;
                            p.sendMessage("チェストの出現率を" + d + "に設定しました");
                        }
                    } catch (NumberFormatException e) {
                        p.sendMessage("チェストの出現率は0~1の数値で指定してください");
                    }
                    return true;
                case "cancel":
                    if (GlobalClass.runningGame == null) {
                        p.sendMessage("ゲームが存在しません");
                    } else if (GlobalClass.runningGame.isRunning) {
                        p.sendMessage("既にゲームが開始されています 中止したい場合は/battleroyale stop を使用してください");
                        return true;
                    }
                    GlobalClass.runningGame = null;
                    Bukkit.getServer().broadcastMessage("ゲームが削除されました");
                    break;
                case "setgame":
                    if (args.length < 2) {
                        p.sendMessage("/ setgame ステージ アイテムリスト で新規ゲームを開設");
                    } else if (GlobalClass.runningGame != null) {
                        p.sendMessage("既にゲームが開設されています /battleroyale cancelまたは/battleroyale stopで安全にゲームを閉じてから再設定してください");
                    } else {
                        GlobalClass.runningGame = new BattleRoyaleData(args[1], args[2]);
                        GlobalClass.runningGame.world = p.getWorld();
                        p.sendMessage("新規ゲーム開設完了");
                        Bukkit.getServer().broadcastMessage("§5バトルロワイヤルが間も無く開催されます！/battleroyale join で参加登録をしましょう！");
                    }
                    break;
                case "start":
                    if (GlobalClass.runningGame == null) {
                        p.sendMessage("ゲームが設定されていません");
                    } else if (!GlobalClass.runningGame.isRunning) {
                        GlobalClass.runningGame.preGameStart();
                        GlobalClass.runningGame.runBattleRoyale.start();
                        GlobalClass.runningGame.playGround.carePackageThread.runTaskTimer(instance, 300, 20 * GlobalClass.runningGame.fieldConfig.getInt("carePackage.frequency"));
                    } else {
                        p.sendMessage("既にゲームが行われています");
                    }
                    break;
                case "stop":
                    if (GlobalClass.runningGame == null || !GlobalClass.runningGame.isRunning) {
                        p.sendMessage("バトルロワイヤルはただいま開催されていません");
                        break;
                    }
                    GlobalClass.runningGame.endGame();
                    GlobalClass.runningGame = null;
                    p.sendMessage("バトルロワイヤルを中断しました");
                    break;
                case "test":
            }
        }
        switch (args[0]) {
            case "join":
                if (GlobalClass.runningGame == null) {
                    p.sendMessage("ただいまゲームは開催されていません");
                } else if (GlobalClass.runningGame.isRunning) {
                    p.sendMessage("既にゲームが開始されています");
                } else if (GlobalClass.runningGame.putParticipant(p)) {
                    p.sendMessage("参加者登録完了");
                    Bukkit.getServer().broadcastMessage(p.getName()+"が参加者登録しました");
                } else {
                    p.sendMessage("既に参加者登録されています");
                }
                break;
            case "leave":
                if (GlobalClass.runningGame != null&&GlobalClass.runningGame.playerList.containsKey(p.getUniqueId())) {
                    GlobalClass.runningGame.removeParticipant(p);
                    p.sendMessage("参加登録を解除しました");
                    Bukkit.getServer().broadcastMessage(p.getName()+"が参加者登録を解除しました");
                }
                break;
        }
    return true;
    }
}

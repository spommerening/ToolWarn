package io.github.toolwarn;

import io.github.toolwarn.config.PluginConfig;
import io.github.toolwarn.listener.EquipmentListener;
import io.github.toolwarn.tracker.DurabilityTracker;
import io.github.toolwarn.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ToolWarnPlugin extends JavaPlugin implements CommandExecutor {

    private PluginConfig pluginConfig;
    private DurabilityTracker tracker;
    private EquipmentListener equipmentListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        tracker = new DurabilityTracker();
        equipmentListener = new EquipmentListener(this, pluginConfig, tracker);

        getServer().getPluginManager().registerEvents(equipmentListener, this);

        var toolwarnCommand = getCommand("toolwarn");
        if (toolwarnCommand != null) {
            toolwarnCommand.setExecutor(this);
        }

        getLogger().info("ToolWarn enabled — monitoring durability thresholds.");
    }

    @Override
    public void onDisable() {
        if (tracker != null) {
            tracker.clearAll();
        }
        getLogger().info("ToolWarn disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("toolwarn")) return false;

        if (!sender.hasPermission("toolwarn.admin")) {
            sender.sendMessage(MessageUtil.colorComponent("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtil.colorComponent("&eUsage: /toolwarn <reload|status>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                pluginConfig.load();
                tracker.clearAll();
                sender.sendMessage(MessageUtil.colorComponent("&aToolWarn config reloaded. All tracker states cleared."));
            }
            case "status" -> {
                int thresholdCount = pluginConfig.getSortedThresholds().size();
                int playerCount = tracker.getTrackedPlayerCount();
                sender.sendMessage(MessageUtil.colorComponent(
                    "&eToolWarn Status: &f" + thresholdCount + " threshold(s) loaded, " +
                    playerCount + " player(s) currently tracked."
                ));
            }
            default -> sender.sendMessage(MessageUtil.colorComponent("&eUsage: /toolwarn <reload|status>"));
        }

        return true;
    }
}

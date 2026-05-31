package io.github.toolwarn.config;

import io.github.toolwarn.ToolWarnPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PluginConfig {

    private final ToolWarnPlugin plugin;

    private List<ThresholdEntry> sortedThresholds = new ArrayList<>();
    private String warningSound = "";
    private boolean sendActionBar = true;

    public PluginConfig(ToolWarnPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        sortedThresholds = new ArrayList<>();

        List<?> rawThresholds = plugin.getConfig().getList("thresholds");
        if (rawThresholds != null) {
            for (Object entry : rawThresholds) {
                if (!(entry instanceof ConfigurationSection section)) {
                    if (entry instanceof java.util.Map<?, ?> map) {
                        parseThresholdMap(map);
                    }
                    continue;
                }
                double pct = section.getDouble("percentage", -1);
                String msg = section.getString("message", "");
                addThreshold(pct, msg);
            }
        }

        // Also try reading via ConfigurationSection list approach
        if (sortedThresholds.isEmpty()) {
            var thresholdsSection = plugin.getConfig().getConfigurationSection("thresholds");
            if (thresholdsSection == null) {
                // Try list of maps via getMapList
                List<java.util.Map<?, ?>> mapList = plugin.getConfig().getMapList("thresholds");
                for (java.util.Map<?, ?> map : mapList) {
                    parseThresholdMap(map);
                }
            }
        }

        sortedThresholds.sort(Comparator.comparingDouble(ThresholdEntry::percentage).reversed());

        warningSound = plugin.getConfig().getString("warning-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        sendActionBar = plugin.getConfig().getBoolean("send-actionbar", true);

        plugin.getLogger().info("Loaded " + sortedThresholds.size() + " threshold(s).");
    }

    private void parseThresholdMap(java.util.Map<?, ?> map) {
        Object pctObj = map.get("percentage");
        Object msgObj = map.get("message");
        if (pctObj == null || msgObj == null) return;
        double pct;
        try {
            pct = Double.parseDouble(pctObj.toString());
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid threshold percentage: " + pctObj);
            return;
        }
        addThreshold(pct, msgObj.toString());
    }

    private void addThreshold(double pct, String msg) {
        if (pct <= 0.0 || pct > 1.0) {
            plugin.getLogger().warning("Skipping threshold with invalid percentage: " + pct + " (must be in (0.0, 1.0])");
            return;
        }
        sortedThresholds.add(new ThresholdEntry(pct, msg));
    }

    public List<ThresholdEntry> getSortedThresholds() {
        return sortedThresholds;
    }

    public String getWarningSound() {
        return warningSound == null ? "" : warningSound;
    }

    public boolean isSendActionBar() {
        return sendActionBar;
    }
}

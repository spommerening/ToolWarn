package io.github.toolwarn.listener;

import io.github.toolwarn.ToolWarnPlugin;
import io.github.toolwarn.config.PluginConfig;
import io.github.toolwarn.config.ThresholdEntry;
import io.github.toolwarn.tracker.DurabilityTracker;
import io.github.toolwarn.util.MessageUtil;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EquipmentListener implements Listener {

    private final ToolWarnPlugin plugin;
    private final PluginConfig config;
    private final DurabilityTracker tracker;

    public EquipmentListener(ToolWarnPlugin plugin, PluginConfig config, DurabilityTracker tracker) {
        this.plugin = plugin;
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        event.getEquipmentChanges().forEach((slot, change) -> {
            tracker.resetSlot(player.getUniqueId(), slot);
            ItemStack newItem = change.newItem();
            if (newItem != null && !newItem.getType().isAir()) {
                checkAndWarn(player, slot, newItem);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Find which slot this item is in, then schedule a 1-tick delayed check
        // after the damage has been applied (EntityEquipmentChangedEvent will also fire,
        // but this acts as a redundancy guard)
        EquipmentSlot slot = findSlotForItem(player, item);
        if (slot == null) return;

        final EquipmentSlot checkedSlot = slot;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            ItemStack current = getItemInSlot(player, checkedSlot);
            if (current != null && !current.getType().isAir()) {
                checkAndWarn(player, checkedSlot, current);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        tracker.removePlayer(event.getPlayer().getUniqueId());
    }

    public void checkAndWarn(Player player, EquipmentSlot slot, ItemStack item) {
        double remaining = getRemainingPercent(item);
        if (remaining >= 1.0) return; // no warnings for full-durability items

        List<ThresholdEntry> thresholds = config.getSortedThresholds();
        Set<Integer> fired = tracker.getFiredSet(player.getUniqueId(), slot);

        for (int i = 0; i < thresholds.size(); i++) {
            ThresholdEntry t = thresholds.get(i);
            if (remaining <= t.percentage() && !fired.contains(i)) {
                fired.add(i);
                sendWarning(player, slot, item, t);
            }
        }
    }

    private void sendWarning(Player player, EquipmentSlot slot, ItemStack item, ThresholdEntry entry) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int max = damageable.getMaxDamage();
        if (max <= 0) return;
        int damage = damageable.hasDamageValue() ? damageable.getDamage() : 0;
        int current = max - damage;
        int percent = (int) Math.round((double) current / max * 100);

        String message = MessageUtil.format(entry.message(), Map.of(
            "{item}",       getItemDisplayName(item),
            "{slot}",       slot.name(),
            "{durability}", String.valueOf(current),
            "{max}",        String.valueOf(max),
            "{percent}",    String.valueOf(percent)
        ));

        player.sendMessage(MessageUtil.colorComponent(message));

        if (config.isSendActionBar()) {
            player.sendActionBar(MessageUtil.colorComponent(message));
        }

        String soundName = config.getWarningSound();
        if (!soundName.isBlank()) {
            try {
                @SuppressWarnings({"deprecation", "removal"})
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid warning-sound in config: '" + soundName + "'");
            }
        }
    }

    private double getRemainingPercent(ItemStack item) {
        if (item == null || item.getType().isAir()) return 1.0;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return 1.0;
        if (!damageable.hasMaxDamage()) return 1.0;
        int max = damageable.getMaxDamage();
        if (max <= 0) return 1.0;
        int damage = damageable.hasDamageValue() ? damageable.getDamage() : 0;
        return (double) (max - damage) / max;
    }

    private String getItemDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // Strip color codes from custom display name for plain text
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.displayName());
        }
        return MessageUtil.toTitleCase(item.getType().name());
    }

    private EquipmentSlot findSlotForItem(Player player, ItemStack item) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack slotItem = getItemInSlot(player, slot);
            if (slotItem != null && slotItem.equals(item)) {
                return slot;
            }
        }
        return null;
    }

    private ItemStack getItemInSlot(Player player, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD      -> player.getInventory().getHelmet();
            case CHEST     -> player.getInventory().getChestplate();
            case LEGS      -> player.getInventory().getLeggings();
            case FEET      -> player.getInventory().getBoots();
            case HAND      -> player.getInventory().getItemInMainHand();
            case OFF_HAND  -> player.getInventory().getItemInOffHand();
            default        -> null;
        };
    }
}

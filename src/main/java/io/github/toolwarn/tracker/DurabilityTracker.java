package io.github.toolwarn.tracker;

import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

public class DurabilityTracker {

    // UUID -> (slot -> set of already-fired threshold indices)
    private final Map<UUID, Map<EquipmentSlot, Set<Integer>>> data = new HashMap<>();

    public Set<Integer> getFiredSet(UUID playerId, EquipmentSlot slot) {
        return data
            .computeIfAbsent(playerId, k -> new EnumMap<>(EquipmentSlot.class))
            .computeIfAbsent(slot, k -> new HashSet<>());
    }

    public void resetSlot(UUID playerId, EquipmentSlot slot) {
        Map<EquipmentSlot, Set<Integer>> playerData = data.get(playerId);
        if (playerData != null) {
            playerData.remove(slot);
        }
    }

    public void removePlayer(UUID playerId) {
        data.remove(playerId);
    }

    public void clearAll() {
        data.clear();
    }

    public int getTrackedPlayerCount() {
        return data.size();
    }
}

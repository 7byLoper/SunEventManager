package ru.loper.suneventmanager.summoner.block.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.manager.SummonBlockManager;
import ru.loper.suneventmanager.summoner.block.model.SummonBlockDefinition;
import ru.loper.suneventmanager.summoner.block.model.SummonLocationMode;

public final class SummonBlockService {
    private final SunEventManager plugin;
    private final EventManager eventManager;
    private final SummonBlockManager manager;
    private final SummonValidator validator;
    private final SummonCooldownService cooldowns;
    private final SummonInventoryService inventory;
    private final SummonEffectsService effects;
    private final SummonMessageService messages;
    private final Map<Integer, PendingSummon> pendingSummons;

    public SummonBlockService(SunEventManager plugin, EventManager eventManager, SummonBlockManager manager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.manager = manager;
        this.validator = new SummonValidator();
        this.cooldowns = new SummonCooldownService();
        this.inventory = new SummonInventoryService(manager);
        this.effects = new SummonEffectsService();
        this.messages = new SummonMessageService(manager);
        this.pendingSummons = new HashMap<>();
    }

    public void use(
            Player player,
            SummonBlockDefinition definition,
            EquipmentSlot hand,
            Location placedLocation,
            Material supportBlock) {
        if (player == null || definition == null || placedLocation == null) {
            return;
        }

        long secondsLeft = cooldowns.getSecondsLeft(player, definition);
        if (secondsLeft > 0L) {
            messages.cooldown(player, definition, secondsLeft);
            effects.playFailed(player, definition);
            return;
        }

        SummonValidator.ValidationResult validation =
                validator.validate(player, placedLocation, supportBlock, definition);
        if (validation != SummonValidator.ValidationResult.OK) {
            messages.validationFailure(player, definition, validation);
            effects.playFailed(player, definition);
            return;
        }

        Location eventLocation = resolveEventLocation(placedLocation, definition);
        if (eventLocation != null) {
            SummonValidator.ValidationResult eventLocationValidation =
                    validator.validateEventLocation(player, eventLocation, definition);
            if (eventLocationValidation != SummonValidator.ValidationResult.OK) {
                messages.validationFailure(player, definition, eventLocationValidation);
                effects.playFailed(player, definition);
                return;
            }
        }

        ItemStack reservedItem =
                (manager.isCancelBlockPlace() && manager.isConsumeItem()) ? inventory.consume(player, hand) : null;

        cooldowns.start(player, definition);
        effects.playSuccess(player, eventLocation == null ? placedLocation : eventLocation, definition);

        if (definition.delayTicks() <= 0L) {
            startEvent(
                    player,
                    player.getUniqueId(),
                    player.getName(),
                    definition,
                    eventLocation,
                    placedLocation,
                    reservedItem);
            return;
        }

        scheduleStart(player, definition, eventLocation, placedLocation, reservedItem);
    }

    public void shutdown() {
        for (PendingSummon pending : pendingSummons.values()) {
            Bukkit.getScheduler().cancelTask(pending.taskId());
            Player player = Bukkit.getPlayer(pending.playerId());
            if (pending.reservedItem() != null) {
                if (player != null) {
                    inventory.refund(player, pending.reservedItem());
                } else {
                    inventory.refund(pending.refundLocation(), pending.reservedItem());
                }
            }
        }
        pendingSummons.clear();
        cooldowns.clear();
    }

    private void scheduleStart(
            Player player,
            SummonBlockDefinition definition,
            Location eventLocation,
            Location refundLocation,
            ItemStack reservedItem) {
        final int[] taskId = {-1};
        BukkitTask task = Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {
                            PendingSummon pending = pendingSummons.remove(taskId[0]);
                            if (pending == null) {
                                return;
                            }

                            Player currentPlayer = Bukkit.getPlayer(pending.playerId());
                            if (currentPlayer != null && pending.eventLocation() != null) {
                                SummonValidator.ValidationResult validation = validator.validateEventLocation(
                                        currentPlayer, pending.eventLocation(), pending.definition());
                                if (validation != SummonValidator.ValidationResult.OK) {
                                    cooldowns.reset(pending.playerId(), pending.definition());
                                    if (pending.reservedItem() != null) {
                                        inventory.refund(currentPlayer, pending.reservedItem());
                                    }
                                    messages.validationFailure(currentPlayer, pending.definition(), validation);
                                    return;
                                }
                            }
                            startEvent(
                                    currentPlayer,
                                    pending.playerId(),
                                    pending.playerName(),
                                    pending.definition(),
                                    pending.eventLocation(),
                                    pending.refundLocation(),
                                    pending.reservedItem());
                        },
                        definition.delayTicks());

        taskId[0] = task.getTaskId();
        pendingSummons.put(
                task.getTaskId(),
                new PendingSummon(
                        task.getTaskId(),
                        player.getUniqueId(),
                        player.getName(),
                        definition,
                        eventLocation == null ? null : eventLocation.clone(),
                        refundLocation == null ? null : refundLocation.clone(),
                        reservedItem));
    }

    private void startEvent(
            Player player,
            UUID playerId,
            String playerName,
            SummonBlockDefinition definition,
            Location eventLocation,
            Location refundLocation,
            ItemStack reservedItem) {
        EventManager.EventStartResult result =
                eventManager.startEvent(definition.eventSpec(), eventLocation, playerName);
        if (!result.success()) {
            cooldowns.reset(playerId, definition);
            if (reservedItem != null) {
                if (player != null) {
                    inventory.refund(player, reservedItem);
                } else {
                    inventory.refund(refundLocation, reservedItem);
                }
            }
            messages.startFailure(player, definition, result);
            effects.playFailed(player, definition);
            return;
        }

        effects.playSpawn(player, definition);
        messages.success(player, definition, result);
    }

    private Location resolveEventLocation(Location placedLocation, SummonBlockDefinition definition) {
        if (definition.locationMode() == SummonLocationMode.RANDOM) {
            return null;
        }
        return placedLocation.clone().add(definition.locationOffset());
    }

    private record PendingSummon(
            int taskId,
            UUID playerId,
            String playerName,
            SummonBlockDefinition definition,
            Location eventLocation,
            Location refundLocation,
            ItemStack reservedItem) {}
}

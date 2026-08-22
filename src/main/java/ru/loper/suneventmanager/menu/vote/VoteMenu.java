package ru.loper.suneventmanager.menu.vote;

import java.util.*;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.menu.button.Button;
import ru.loper.suncore.api.menu.impl.AbstractMenu;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.vote.EventVote;
import ru.loper.suneventmanager.api.models.vote.Vote;
import ru.loper.suneventmanager.utils.TimeUtils;

public class VoteMenu extends AbstractMenu {
    @Getter
    private final Set<Player> voteViewers = new HashSet<>();

    private final CustomConfig config;
    private final Vote vote;

    public VoteMenu(Vote vote) {
        this.vote = vote;
        this.config = vote.getConfigManager().getVoteMenuConfig();
    }

    @Override
    public @Nullable String getTitle() {
        return config.configMessage("title", "Голосование")
                .replace("{time}", TimeUtils.formatSeconds(vote.getVoteTimer()));
    }

    @Override
    public int getSize() {
        int rows = config.getConfig().getInt("rows", 3);
        return rows * 9;
    }

    @Override
    public void getItemsAndButtons() {
        addDecor();
        addEventButtons();
    }

    private void addEventButtons() {
        ConfigurationSection eventsSection = config.getConfig().getConfigurationSection("events");
        ConfigurationSection slotsSection = config.getConfig().getConfigurationSection("slots");

        if (eventsSection == null || slotsSection == null) {
            return;
        }

        List<Map.Entry<Event, EventVote>> eventList =
                new ArrayList<>(vote.getEventVotes().entrySet());
        int eventCount = eventList.size();

        List<Integer> slotList = slotsSection.getIntegerList(String.valueOf(eventCount));
        if (slotList.isEmpty()) {
            return;
        }

        for (int i = 0; i < Math.min(eventCount, slotList.size()); i++) {
            Map.Entry<Event, EventVote> entry = eventList.get(i);
            Event event = entry.getKey();
            EventVote eventVote = entry.getValue();
            int votes = eventVote.getTotalVotes();

            ConfigurationSection eventSection = eventsSection.getConfigurationSection(event.getId());
            if (eventSection == null) {
                continue;
            }

            ItemBuilder builder = ItemBuilder.fromConfig(eventSection);
            builder.name(builder.name()
                    .replace("{votes}", String.valueOf(votes))
                    .replace("{rarity}", event.getEventRarity().getDisplayName()));

            builder.lore(builder.lore().stream()
                    .map(line -> line.replace("{votes}", String.valueOf(votes))
                            .replace("{rarity}", event.getEventRarity().getDisplayName()))
                    .toList());

            int slot = slotList.get(i);
            addEventButton(builder, slot, event);
        }
    }

    private void addEventButton(ItemBuilder builder, int slot, Event event) {
        menuButtons.add(new Button(builder.build(), slot) {
            @Override
            public void onClick(InventoryClickEvent e) {
                if (!(e.getWhoClicked() instanceof Player player)) {
                    return;
                }

                if (vote.hasVoted(player, event)) {
                    return;
                }

                vote.addVote(player, event);
            }
        });
    }

    private void addDecor() {
        ConfigurationSection decorSection = config.getConfig().getConfigurationSection("decor");
        if (decorSection == null) {
            return;
        }

        addDecorFromSection(decorSection);
    }

    public boolean requiresTimerRefresh() {
        String title = config.getConfig().getString("title", "");
        return title.contains("{time}");
    }

    public void refreshViewers() {
        new ArrayList<>(voteViewers).stream().filter(Player::isOnline).forEach(this::show);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        HumanEntity entity = event.getPlayer();
        if (entity instanceof Player player) {
            voteViewers.remove(player);
        }
    }

    @Override
    public void show(@NotNull Player player) {
        super.show(player);
    }

    public void onOpen(@NotNull InventoryOpenEvent event) {
        HumanEntity entity = event.getPlayer();
        if (entity instanceof Player player) {
            voteViewers.add(player);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return menuInventory;
    }
}

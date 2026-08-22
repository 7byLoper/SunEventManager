package ru.loper.suneventmanager;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suneventmanager.api.models.EventChest;
import ru.loper.suneventmanager.commands.event.EventCommand;
import ru.loper.suneventmanager.commands.vote.VoteCommand;
import ru.loper.suneventmanager.config.LootSettingsManager;
import ru.loper.suneventmanager.config.PluginConfigManager;
import ru.loper.suneventmanager.listeners.ChestListener;
import ru.loper.suneventmanager.listeners.InternalListener;
import ru.loper.suneventmanager.listeners.SummonBlockListener;
import ru.loper.suneventmanager.listeners.VoteMenuListener;
import ru.loper.suneventmanager.manager.EventManager;
import ru.loper.suneventmanager.manager.SummonBlockManager;
import ru.loper.suneventmanager.message.MessageService;
import ru.loper.suneventmanager.summoner.Summoner;
import ru.loper.suneventmanager.summoner.block.service.SummonBlockService;

@Getter
public final class SunEventManager extends JavaPlugin {

    @Getter
    private static SunEventManager instance;

    private MessageService messageService;
    private PluginConfigManager configManager;
    private LootSettingsManager lootSettingsManager;
    private Summoner summoner;
    private EventManager eventManager;
    private SummonBlockManager summonBlockManager;
    private SummonBlockService summonBlockService;

    @Override
    public void onEnable() {
        instance = this;

        messageService = new MessageService(this);
        configManager = new PluginConfigManager(this);
        lootSettingsManager = configManager.getLootSettingsManager();
        eventManager = new EventManager(this);
        summoner = new Summoner(configManager, this);
        summonBlockManager = new SummonBlockManager(this, configManager.getSummonBlocksConfig());
        summonBlockService = new SummonBlockService(this, eventManager, summonBlockManager);

        Bukkit.getPluginManager().registerEvents(new ChestListener(), this);
        Bukkit.getPluginManager().registerEvents(new VoteMenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new InternalListener(eventManager), this);
        Bukkit.getPluginManager().registerEvents(new SummonBlockListener(summonBlockManager, summonBlockService), this);

        initCommands();
    }

    @Override
    public void onDisable() {
        if (summoner != null && summoner.getVoteManager() != null) {
            summoner.getVoteManager().clearCurrentVote();
        }

        if (summonBlockService != null) {
            summonBlockService.shutdown();
        }

        if (eventManager != null) {
            eventManager.stopAllActiveEvents();
            eventManager.clearAllRegisteredEvents();
        }

        EventChest.removeAllChests();
        instance = null;
    }

    private void initCommands() {
        new EventCommand(this).registerWrappers();
        new VoteCommand(this).registerWrappers();
    }
}

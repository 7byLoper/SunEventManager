package ru.loper.suneventmanager.bossbar;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.enums.EventState;
import ru.loper.suneventmanager.api.models.Event;
import ru.loper.suneventmanager.api.models.settings.EventBossBarSettings;

public final class EventBossBarController {
    private final Event event;
    private final EventBossBarSettings settings;
    private BossBar bossBar;

    public EventBossBarController(Event event, EventBossBarSettings settings) {
        this.event = event;
        this.settings = settings;
    }

    public void update() {
        if (settings == null || !settings.isEnabled() || event == null || !event.isActivate()) {
            return;
        }

        EventState state = event.getCurrentState();
        if (state != EventState.STARTED && state != EventState.OPENED && state != EventState.STOPPING) {
            hide();
            return;
        }

        String rawTitle = title(state);
        if (rawTitle == null || rawTitle.isBlank()) {
            hide();
            return;
        }

        String title = SunEventManager.getInstance()
                .getMessageService()
                .toLegacy(null, rawTitle, event.getMessagePlaceholders());

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(title, settings.getColor(), settings.getStyle());
        } else {
            bossBar.setTitle(title);
            bossBar.setColor(settings.getColor());
            bossBar.setStyle(settings.getStyle());
        }

        bossBar.setProgress(progress(state));
        syncPlayers();
        bossBar.setVisible(true);
    }

    private void syncPlayers() {
        if (bossBar == null) {
            return;
        }

        double radius = settings.getRadius();
        if (radius <= 0.0D) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!bossBar.getPlayers().contains(player)) {
                    bossBar.addPlayer(player);
                }
            }
            return;
        }

        Location center = event.getEventLocation();
        if (center == null || center.getWorld() == null) {
            bossBar.removeAll();
            return;
        }

        double radiusSquared = radius * radius;

        for (Player player : List.copyOf(bossBar.getPlayers())) {
            if (!isInRange(player, center, radiusSquared)) {
                bossBar.removePlayer(player);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInRange(player, center, radiusSquared)
                    && !bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private boolean isInRange(Player player, Location center, double radiusSquared) {
        if (player == null || !player.isOnline() || player.getWorld() != center.getWorld()) {
            return false;
        }

        return player.getLocation().distanceSquared(center) <= radiusSquared;
    }

    public void hide() {
        if (bossBar == null) {
            return;
        }
        bossBar.removeAll();
        bossBar.setVisible(false);
        bossBar = null;
    }

    private String title(EventState state) {
        return switch (state) {
            case STARTED -> settings.getStartedTitle();
            case OPENED -> settings.getOpenedTitle();
            case STOPPING ->
                event.getConfigManager().isOpenedStageEnabled()
                        ? settings.getStoppingTitle()
                        : settings.getOpenedTitle();
            default -> "";
        };
    }

    private double progress(EventState state) {
        int total =
                switch (state) {
                    case STARTED -> event.getConfigManager().getEventStartTime();
                    case OPENED -> event.getConfigManager().getEventOpenedTime();
                    case STOPPING -> event.getConfigManager().getEventStopTime();
                    default -> 0;
                };

        if (total <= 0) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, event.getEventTimer() / (double) total));
    }
}

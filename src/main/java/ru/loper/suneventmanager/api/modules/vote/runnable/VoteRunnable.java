package ru.loper.suneventmanager.api.modules.vote.runnable;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.modules.vote.Vote;
import ru.loper.suneventmanager.api.modules.vote.VoteManager;

@RequiredArgsConstructor
public class VoteRunnable extends BukkitRunnable {
    private final VoteManager voteManager;

    @Override
    public void run() {
        Vote currentVote = voteManager.getCurrentVote();
        if (currentVote == null) {
            cancel();
            return;
        }

        int tick = currentVote.tick();

        currentVote.getVoteMenu().getVoteViewers()
                .forEach(player -> currentVote.getVoteMenu().show(player));

        if (tick <= 0) {
            currentVote.stopVote();
            voteManager.clearCurrentVote();
            if (SunEventManager.isDebug()) {
                Bukkit.getLogger().info("Голосование остановлено!");
            }
        }
    }
}

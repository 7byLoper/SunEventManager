package ru.loper.suneventmanager.api.modules.summoner.runnable;

import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;
import ru.loper.suneventmanager.api.modules.summoner.Summoner;

@RequiredArgsConstructor
public class SummonerRunnable extends BukkitRunnable {
    private final Summoner summoner;

    @Override
    public void run() {
        if (summoner.isEventTimer()) {
            int eventTimer = summoner.getAndSubtractEventTimer();
            if (eventTimer <= 0) {
                summoner.startRandomEvent();
                summoner.resetEventTimer();
            }
        }

        if (summoner.isVoteTimer()) {
            int voteTimer = summoner.getAndSubtractVoteTimer();
            if (voteTimer <= 0) {
                summoner.getVoteManager().startVote();
                summoner.resetVoteTimer();
            }
        }
    }
}

package ru.loper.suneventmanager.api.runnable;

import lombok.RequiredArgsConstructor;
import ru.loper.suncore.api.scheduler.CoreRunnable;
import ru.loper.suneventmanager.SunEventManager;
import ru.loper.suneventmanager.api.models.vote.Vote;
import ru.loper.suneventmanager.manager.VoteManager;

@RequiredArgsConstructor
public class VoteRunnable extends CoreRunnable {
    private final VoteManager voteManager;

    @Override
    public void run() {
        Vote currentVote = voteManager.getCurrentVote();
        if (currentVote == null) {
            cancel();
            return;
        }

        int secondsLeft = currentVote.tick();

        if (SunEventManager.getInstance().getConfigManager().isUpdateVoteMenu()
                && (currentVote.consumeMenuDirty() || currentVote.getVoteMenu().requiresTimerRefresh())) {
            currentVote.getVoteMenu().refreshViewers();
        }

        if (secondsLeft <= 0) {
            currentVote.stopVote();
            voteManager.clearCurrentVote();
        }
    }
}

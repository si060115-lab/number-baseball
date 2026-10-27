package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GameSession.giveUp의 단위 테스트.
 * 대상: 모드별 기권·시간 초과 처리 (유즈케이스 5)
 * PvE는 실패, 1:1 PvP는 상대 승리, 다인 PvP는 본인 탈락으로 처리되어야 한다.
 */
class GiveUpTest {
    /** PvP 모드에서 유저 순서대로 설정하는 비밀 숫자 */
    private static final String[] SECRETS = {"123", "456", "789"};

    private RecordingListener listener;

    @BeforeEach
    void setUp() {
        listener = new RecordingListener();
    }

    /**
     * 턴을 시작한 세션을 생성한다.
     *
     * @param mode  게임 모드
     * @param count 참가 인원
     * @return 진행 중인 세션
     */
    private GameSession startedSession(GameMode mode, int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new Player("유저" + (i + 1)));
        }
        GameSession s = new GameSession(mode, players, new Random(5));
        s.addListener(listener);
        if (mode != GameMode.PVE) {
            for (int i = 0; i < count; i++) {
                s.setSecret(players.get(i), SECRETS[i]);
            }
        }
        s.startTurns();
        return s;
    }

    /** PvE 모드에서 기권하면 승자 없이 실패로 종료되는지 검증한다. */
    @Test
    @DisplayName("PvE 기권은 실패로 처리한다")
    void pveGiveUpFails() {
        GameSession s = startedSession(GameMode.PVE, 1);
        Player player = s.getCurrentPlayer();

        s.giveUp(player, GameSession.REASON_GIVE_UP);

        assertEquals(GamePhase.FINISHED, s.getPhase());
        assertNull(s.getWinner());
        assertTrue(listener.finished);
        assertSame(player, listener.gaveUp);
        assertEquals("기권", listener.reason);
    }

    /** 1:1 PvP 모드에서 시간이 초과되면 상대가 승리하는지 검증한다. */
    @Test
    @DisplayName("1:1 시간 초과는 상대 승리로 처리한다")
    void duelTimeoutOpponentWins() {
        GameSession s = startedSession(GameMode.DUEL, 2);
        Player current = s.getCurrentPlayer();
        Player opponent = s.getTargets(current).get(0);

        s.giveUp(current, GameSession.REASON_TIMEOUT);

        assertEquals(GamePhase.FINISHED, s.getPhase());
        assertSame(opponent, s.getWinner());
        assertEquals("시간 초과", listener.reason);
    }

    /** 다인 PvP 모드에서 기권하면 본인만 탈락하고 다음 유저에게 턴이 넘어가는지 검증한다. */
    @Test
    @DisplayName("다인 기권은 본인 탈락 후 다음 턴으로 넘어간다")
    void multiGiveUpEliminatesAndPassesTurn() {
        GameSession s = startedSession(GameMode.MULTI, 3);
        List<Player> order = s.getTurnOrder();

        s.giveUp(order.get(0), GameSession.REASON_GIVE_UP);

        assertFalse(order.get(0).isAlive());
        assertEquals(List.of(order.get(0)), listener.eliminated);
        assertEquals(GamePhase.PLAYING, s.getPhase());
        assertSame(order.get(1), s.getCurrentPlayer());
        assertEquals(List.of(order.get(2)), s.getTargets(order.get(1)));
    }

    /** 다인 PvP 모드에서 기권으로 1명만 남으면 그 유저가 우승하는지 검증한다. */
    @Test
    @DisplayName("다인 기권으로 1명이 남으면 우승한다")
    void multiGiveUpLastSurvivorWins() {
        GameSession s = startedSession(GameMode.MULTI, 3);
        List<Player> order = s.getTurnOrder();

        s.giveUp(order.get(0), GameSession.REASON_GIVE_UP);
        s.giveUp(order.get(1), GameSession.REASON_TIMEOUT);

        assertEquals(GamePhase.FINISHED, s.getPhase());
        assertSame(order.get(2), s.getWinner());
    }

    /** 자기 턴이 아닌 유저는 기권할 수 없는지 검증한다. */
    @Test
    @DisplayName("자기 턴이 아니면 기권할 수 없다")
    void giveUpOutOfTurnRejected() {
        GameSession s = startedSession(GameMode.DUEL, 2);
        Player other = s.getTargets(s.getCurrentPlayer()).get(0);

        assertThrows(IllegalStateException.class, () -> s.giveUp(other, GameSession.REASON_GIVE_UP));
        assertEquals(GamePhase.PLAYING, s.getPhase());
        assertNull(listener.gaveUp);
    }

    /** 종료된 게임에서는 기권할 수 없는지 검증한다. */
    @Test
    @DisplayName("종료된 게임에서는 기권할 수 없다")
    void giveUpAfterFinishRejected() {
        GameSession s = startedSession(GameMode.PVE, 1);
        Player player = s.getCurrentPlayer();
        s.giveUp(player, GameSession.REASON_GIVE_UP);

        assertThrows(IllegalStateException.class, () -> s.giveUp(player, GameSession.REASON_GIVE_UP));
    }

    /** 발생한 이벤트를 기록하는 테스트용 리스너 */
    private static class RecordingListener implements GameListener {
        final List<Player> eliminated = new ArrayList<>();
        Player gaveUp;
        String reason;
        boolean finished;

        @Override
        public void onGaveUp(Player player, String reason) {
            this.gaveUp = player;
            this.reason = reason;
        }

        @Override
        public void onEliminated(Player player, String reason) {
            eliminated.add(player);
        }

        @Override
        public void onFinished(Player winner) {
            finished = true;
        }
    }
}

package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GameSession의 다인 PvP 모드 단위 테스트.
 * 대상: getTargets, guess의 탈락·우승 처리 (유즈케이스 2)
 */
class MultiSessionTest {
    /** 어느 유저의 비밀 숫자(123, 456, 789)와도 같지 않아 항상 오답이 되는 추측 숫자 */
    private static final String MISS = "321";

    private GameSession session;
    private RecordingListener listener;
    /** 턴 순서상 첫 번째 유저 */
    private Player first;
    /** 턴 순서상 두 번째 유저 */
    private Player second;
    /** 턴 순서상 세 번째 유저 */
    private Player third;

    /** 테스트마다 3명이 비밀 숫자를 설정하고 턴을 시작한 세션을 준비한다. */
    @BeforeEach
    void setUp() {
        Player a = new Player("A");
        Player b = new Player("B");
        Player c = new Player("C");
        session = new GameSession(GameMode.MULTI, List.of(a, b, c), new Random(7));
        listener = new RecordingListener();
        session.addListener(listener);
        session.setSecret(a, "123");
        session.setSecret(b, "456");
        session.setSecret(c, "789");
        session.startTurns();

        List<Player> order = session.getTurnOrder();
        first = order.get(0);
        second = order.get(1);
        third = order.get(2);
    }

    /** @return 주어진 유저의 비밀 숫자 문자열 */
    private static String answerOf(Player player) {
        return player.getSecret().toString();
    }

    /** @return 이름이 서로 다른 유저 목록 */
    private static List<Player> players(int count) {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Player("유저" + (i + 1)));
        }
        return list;
    }

    /** 참가 인원은 3~5명만 허용하는지 검증한다. */
    @Test
    @DisplayName("다인 PvP 참가 인원은 3~5명이다")
    void playerCountRange() {
        assertThrows(IllegalArgumentException.class, () -> new GameSession(GameMode.MULTI, players(2), new Random()));
        assertThrows(IllegalArgumentException.class, () -> new GameSession(GameMode.MULTI, players(6), new Random()));
        assertDoesNotThrow(() -> new GameSession(GameMode.MULTI, players(3), new Random()));
        assertDoesNotThrow(() -> new GameSession(GameMode.MULTI, players(5), new Random()));
    }

    /** 지목 대상에서 자기 자신이 제외되는지 검증한다. */
    @Test
    @DisplayName("상대 목록에는 자기 자신이 없다")
    void targetsExcludeSelf() {
        List<Player> targets = session.getTargets(first);
        assertEquals(2, targets.size());
        assertFalse(targets.contains(first));
        assertTrue(targets.containsAll(List.of(second, third)));
    }

    /** 상대의 숫자를 맞히면 그 상대만 탈락하고 게임이 계속되는지 검증한다. */
    @Test
    @DisplayName("상대의 숫자를 맞히면 그 상대가 탈락한다")
    void correctGuessEliminatesTarget() {
        session.guess(first, third, answerOf(third));

        assertFalse(third.isAlive());
        assertEquals(GamePhase.PLAYING, session.getPhase());
        assertEquals(List.of(third), listener.eliminated);
        assertSame(second, session.getCurrentPlayer());
        assertEquals(List.of(first), session.getTargets(second));
    }

    /** 탈락한 유저는 지목할 수 없고, 거부되어도 턴이 유지되는지 검증한다. */
    @Test
    @DisplayName("탈락한 유저는 지목할 수 없다")
    void eliminatedCannotBeTargeted() {
        session.guess(first, third, answerOf(third));

        assertThrows(IllegalArgumentException.class, () -> session.guess(second, third, MISS));
        assertSame(second, session.getCurrentPlayer());
    }

    /** 탈락한 유저의 턴은 건너뛰는지 검증한다. */
    @Test
    @DisplayName("탈락한 유저의 턴은 건너뛴다")
    void eliminatedTurnSkipped() {
        session.guess(first, third, answerOf(third));
        session.guess(second, first, MISS);

        assertSame(first, session.getCurrentPlayer());
    }

    /** 마지막까지 남은 한 명이 우승하는지 검증한다. */
    @Test
    @DisplayName("마지막 1명이 남으면 우승한다")
    void lastSurvivorWins() {
        session.guess(first, third, answerOf(third));
        session.guess(second, first, answerOf(first));

        assertEquals(GamePhase.FINISHED, session.getPhase());
        assertSame(second, session.getWinner());
        assertSame(second, listener.winner);
        assertEquals(List.of(third, first), listener.eliminated);
    }

    /** 발생한 이벤트를 기록하는 테스트용 리스너 */
    private static class RecordingListener implements GameListener {
        final List<Player> eliminated = new ArrayList<>();
        Player winner;

        @Override
        public void onEliminated(Player player, String reason) {
            eliminated.add(player);
        }

        @Override
        public void onFinished(Player winner) {
            this.winner = winner;
        }
    }
}

package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GameSession의 1:1 PvP 모드 단위 테스트.
 * 대상: setSecret, startTurns, abort, getTargets, guess (유즈케이스 1 기본 흐름과 대안 흐름)
 */
class DuelSessionTest {
    /** 두 유저의 비밀 숫자(123, 456)와 겹치는 숫자가 없어 항상 오답이 되는 추측 숫자 */
    private static final String MISS = "789";

    private Player hong;
    private Player kim;
    private GameSession session;
    private RecordingListener listener;

    /** 테스트마다 설정 단계의 1:1 세션을 준비한다. */
    @BeforeEach
    void setUp() {
        hong = new Player("홍길동");
        kim = new Player("김철수");
        session = new GameSession(GameMode.DUEL, List.of(hong, kim), new Random(3));
        listener = new RecordingListener();
        session.addListener(listener);
    }

    /** 두 유저의 비밀 숫자를 설정하고 턴을 시작한다. (홍길동: 123, 김철수: 456) */
    private void setSecretsAndStart() {
        session.setSecret(hong, "123");
        session.setSecret(kim, "456");
        session.startTurns();
    }

    /** @return 주어진 유저의 상대 */
    private Player opponentOf(Player player) {
        return (player == hong) ? kim : hong;
    }

    /** 모든 유저가 비밀 숫자를 설정해야 턴을 시작할 수 있는지 검증한다. */
    @Test
    @DisplayName("비밀 숫자를 모두 설정해야 턴을 시작할 수 있다")
    void startRequiresAllSecrets() {
        session.setSecret(hong, "123");
        assertFalse(session.isAllSecretsSet());
        assertThrows(IllegalStateException.class, () -> session.startTurns());

        session.setSecret(kim, "456");
        assertTrue(session.isAllSecretsSet());
        session.startTurns();
        assertEquals(GamePhase.PLAYING, session.getPhase());
        assertEquals(2, listener.order.size());
        assertSame(listener.order.get(0), session.getCurrentPlayer());
    }

    /** 형식 오류인 비밀 숫자는 설정되지 않는지 검증한다. (대안 흐름 3-1) */
    @Test
    @DisplayName("잘못된 비밀 숫자는 설정되지 않는다")
    void invalidSecretNotSet() {
        assertThrows(InvalidInputException.class, () -> session.setSecret(hong, "112"));
        assertFalse(hong.hasSecret());
        assertEquals(GamePhase.SETUP, session.getPhase());
    }

    /** 비밀 숫자 입력 도중 취소하면 무효 처리되는지 검증한다. (대안 흐름 3-2) */
    @Test
    @DisplayName("비밀 숫자 입력을 취소하면 무효 처리된다")
    void cancelAborts() {
        session.setSecret(hong, "123");
        session.abort();
        assertEquals(GamePhase.ABORTED, session.getPhase());
        assertNull(session.getWinner());
        assertEquals(1, listener.abortCount);

        // 이미 무효 처리된 게임은 다시 처리하지 않고, 턴도 시작할 수 없다.
        session.abort();
        assertEquals(1, listener.abortCount);
        assertThrows(IllegalStateException.class, () -> session.startTurns());
    }

    /** 오답이면 턴이 상대에게 넘어가는지 검증한다. (기본 흐름 6) */
    @Test
    @DisplayName("턴은 두 유저가 번갈아 가진다")
    void turnsAlternate() {
        setSecretsAndStart();
        Player first = session.getCurrentPlayer();
        Player second = opponentOf(first);

        session.guess(first, second, MISS);
        assertSame(second, session.getCurrentPlayer());
        session.guess(second, first, MISS);
        assertSame(first, session.getCurrentPlayer());
        assertEquals(3, listener.turnCount);
    }

    /** 상대의 숫자를 맞힌 유저가 승리하는지 검증한다. (기본 흐름 7) */
    @Test
    @DisplayName("상대의 숫자를 맞히면 승리한다")
    void correctGuessWins() {
        setSecretsAndStart();
        Player first = session.getCurrentPlayer();
        Player second = opponentOf(first);

        session.guess(first, second, second.getSecret().toString());
        assertEquals(GamePhase.FINISHED, session.getPhase());
        assertSame(first, session.getWinner());
        assertSame(first, listener.winner);
    }

    /** 자기 턴이 아닌 유저는 추측할 수 없는지 검증한다. */
    @Test
    @DisplayName("자기 턴이 아니면 추측할 수 없다")
    void guessOutOfTurnRejected() {
        setSecretsAndStart();
        Player first = session.getCurrentPlayer();
        Player second = opponentOf(first);

        assertThrows(IllegalStateException.class, () -> session.guess(second, first, MISS));
        assertSame(first, session.getCurrentPlayer());
    }

    /** 지목 대상은 상대 한 명뿐이며 자기 자신은 지목할 수 없는지 검증한다. */
    @Test
    @DisplayName("상대는 자기 자신을 제외한 한 명이다")
    void targetIsOpponent() {
        setSecretsAndStart();
        assertEquals(List.of(kim), session.getTargets(hong));
        assertEquals(List.of(hong), session.getTargets(kim));

        Player first = session.getCurrentPlayer();
        assertThrows(IllegalArgumentException.class, () -> session.guess(first, first, MISS));
    }

    /** 형식 오류 입력은 턴을 넘기지 않는지 검증한다. (대안 흐름 5-1) */
    @Test
    @DisplayName("형식 오류는 턴을 넘기지 않는다")
    void invalidGuessKeepsTurn() {
        setSecretsAndStart();
        Player first = session.getCurrentPlayer();
        Player second = opponentOf(first);

        assertThrows(InvalidInputException.class, () -> session.guess(first, second, "12a"));
        assertSame(first, session.getCurrentPlayer());
        assertEquals(1, listener.turnCount);
    }

       /**
     * 턴 순서가 무작위로 정해지는지 검증한다. (기본 흐름 4)
     * 실제 게임과 같이 하나의 난수 생성기를 이어서 사용하여 50판을 진행하고,
     * 두 유저가 모두 선공을 가진 적이 있는지 확인한다.
     * 시드가 인접한 Random을 매번 새로 생성하면 첫 난수가 서로 비슷하여 결과가 치우치므로 사용하지 않는다.
     */
    @Test
    @DisplayName("턴 순서는 랜덤으로 정해진다")
    void turnOrderRandom() {
        Random rnd = new Random(42);
        Set<String> firstNames = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            Player a = new Player("A");
            Player b = new Player("B");
            GameSession s = new GameSession(GameMode.DUEL, List.of(a, b), rnd);
            s.setSecret(a, "123");
            s.setSecret(b, "456");
            s.startTurns();
            firstNames.add(s.getCurrentPlayer().getName());
        }
        assertEquals(Set.of("A", "B"), firstNames);
    }


    /** 발생한 이벤트를 기록하는 테스트용 리스너 */
    private static class RecordingListener implements GameListener {
        final List<Player> order = new ArrayList<>();
        int turnCount;
        int abortCount;
        Player winner;

        @Override
        public void onTurnOrderDecided(List<Player> order) {
            this.order.addAll(order);
        }

        @Override
        public void onTurnStarted(Player current) {
            turnCount++;
        }

        @Override
        public void onFinished(Player winner) {
            this.winner = winner;
        }

        @Override
        public void onAborted() {
            abortCount++;
        }
    }
}

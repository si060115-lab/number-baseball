package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GameSession의 PvE 모드 단위 테스트.
 * 대상: startTurns, getTargets, guess (유즈케이스 3 기본 흐름과 대안 흐름)
 */
class GameSessionTest {
    private GameSession session;
    private Player user;
    private Player computer;
    private RecordingListener listener;

    /** 테스트마다 턴이 시작된 PvE 세션을 준비한다. */
    @BeforeEach
    void setUp() {
        user = new Player("유저");
        session = new GameSession(GameMode.PVE, List.of(user), new Random(7));
        computer = session.getComputer();
        listener = new RecordingListener();
        session.addListener(listener);
        session.startTurns();
    }

    /** @return 컴퓨터의 비밀 숫자 */
    private String answer() {
        return computer.getSecret().toString();
    }

    /**
     * 정답을 뒤집은 숫자를 반환한다.
     * 중복 없는 3자리 숫자는 첫째 자리와 셋째 자리가 다르므로 뒤집으면 항상 오답이 된다.
     */
    private String wrong() {
        return new StringBuilder(answer()).reverse().toString();
    }

    /** 정답을 맞히면 유저의 승리로 종료되는지 검증한다. */
    @Test
    @DisplayName("정답을 맞히면 승리한다")
    void correctGuessWins() {
        GuessResult r = session.guess(user, computer, answer());
        assertTrue(r.isCorrect());
        assertEquals(GamePhase.FINISHED, session.getPhase());
        assertSame(user, session.getWinner());
        assertTrue(listener.finished);
        assertSame(user, listener.winner);
    }

    /** 10번째 시도까지 틀리면 실패로 종료되는지 검증한다. (대안 흐름 5-1) */
    @Test
    @DisplayName("10번 모두 틀리면 실패한다")
    void failsAfterTenWrongGuesses() {
        for (int i = 0; i < GameRules.PVE_MAX_ATTEMPTS - 1; i++) {
            session.guess(user, computer, wrong());
        }
        assertEquals(GamePhase.PLAYING, session.getPhase());
        assertEquals(1, session.getRemainingAttempts());

        session.guess(user, computer, wrong());
        assertEquals(GamePhase.FINISHED, session.getPhase());
        assertNull(session.getWinner());
        assertTrue(listener.finished);
        assertNull(listener.winner);
    }

    /** 마지막 시도에서 정답을 맞힌 경우 승리로 처리되는지 검증한다. */
    @Test
    @DisplayName("10번째 시도에서 맞히면 승리한다")
    void winsOnTenthAttempt() {
        for (int i = 0; i < GameRules.PVE_MAX_ATTEMPTS - 1; i++) {
            session.guess(user, computer, wrong());
        }
        session.guess(user, computer, answer());
        assertSame(user, session.getWinner());
    }

    /** 형식 오류 입력은 시도 횟수와 진행 상태에 영향을 주지 않는지 검증한다. (대안 흐름 4-1) */
    @Test
    @DisplayName("형식 오류는 시도 횟수에 포함하지 않는다")
    void invalidInputDoesNotCountAttempt() {
        assertThrows(InvalidInputException.class, () -> session.guess(user, computer, "112"));
        assertEquals(0, session.getAttempts());
        assertEquals(GameRules.PVE_MAX_ATTEMPTS, session.getRemainingAttempts());
        assertEquals(GamePhase.PLAYING, session.getPhase());
        assertEquals(0, listener.judgedCount);
    }

    /** 종료된 게임에서는 추측할 수 없는지 검증한다. */
    @Test
    @DisplayName("게임 종료 후에는 추측할 수 없다")
    void guessAfterFinishRejected() {
        session.guess(user, computer, answer());
        assertThrows(IllegalStateException.class, () -> session.guess(user, computer, wrong()));
    }

    /** 턴 시작 전에는 추측할 수 없는지 검증한다. */
    @Test
    @DisplayName("턴 시작 전에는 추측할 수 없다")
    void guessBeforeStartRejected() {
        Player other = new Player("유저2");
        GameSession notStarted = new GameSession(GameMode.PVE, List.of(other), new Random(1));
        assertThrows(IllegalStateException.class,
                () -> notStarted.guess(other, notStarted.getComputer(), "123"));
    }

    /** PvE 모드의 지목 대상은 컴퓨터뿐인지 검증한다. */
    @Test
    @DisplayName("PvE 모드의 상대는 컴퓨터뿐이다")
    void targetsOnlyComputer() {
        assertEquals(List.of(computer), session.getTargets(user));
        assertThrows(IllegalArgumentException.class, () -> session.guess(user, user, wrong()));
    }

    /** 턴 시작 이벤트가 시작 시점과 오답 이후에 발생하는지 검증한다. */
    @Test
    @DisplayName("오답 후 다음 턴이 시작된다")
    void turnEventsFired() {
        assertEquals(1, listener.turnCount);
        session.guess(user, computer, wrong());
        assertEquals(2, listener.turnCount);
        assertEquals(1, listener.judgedCount);
    }

    /** 모드의 허용 인원을 벗어나면 세션을 만들 수 없는지 검증한다. */
    @Test
    @DisplayName("PvE 모드는 1명만 참가할 수 있다")
    void invalidPlayerCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameSession(GameMode.PVE, List.of(new Player("a"), new Player("b")), new Random()));
    }

    /** 발생한 이벤트를 기록하는 테스트용 리스너 */
    private static class RecordingListener implements GameListener {
        int turnCount;
        int judgedCount;
        boolean finished;
        Player winner;

        @Override
        public void onTurnStarted(Player current) {
            turnCount++;
        }

        @Override
        public void onGuessJudged(Player attacker, Player target, String guess, GuessResult result) {
            judgedCount++;
        }

        @Override
        public void onFinished(Player winner) {
            finished = true;
            this.winner = winner;
        }
    }
}

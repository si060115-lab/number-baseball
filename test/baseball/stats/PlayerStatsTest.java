package baseball.stats;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PlayerStats의 단위 테스트.
 * 대상: 결과 반영(addResult), 승률 계산(winRate), 횟수 검사
 */
class PlayerStatsTest {

    /** 결과를 반영하면 새 객체가 반환되고 기존 객체는 바뀌지 않는지 검증한다. */
    @Test
    @DisplayName("결과를 반영하면 플레이 수와 승리 수가 늘어난다")
    void addResultAccumulates() {
        PlayerStats empty = PlayerStats.empty("A");
        PlayerStats afterWin = empty.addResult(true);
        PlayerStats afterLose = afterWin.addResult(false);

        assertEquals(new PlayerStats("A", 1, 1), afterWin);
        assertEquals(new PlayerStats("A", 2, 1), afterLose);
        assertEquals(new PlayerStats("A", 0, 0), empty);
    }

    /** 승률이 백분율로 계산되고, 플레이 수가 0이면 0인지 검증한다. */
    @Test
    @DisplayName("승률은 승리 수 / 플레이 수 × 100이다")
    void winRateCalculated() {
        assertEquals(25.0, new PlayerStats("A", 4, 1).winRate(), 1e-9);
        assertEquals(100.0, new PlayerStats("A", 3, 3).winRate(), 1e-9);
        assertEquals(0.0, PlayerStats.empty("A").winRate(), 1e-9);
    }

    /** 음수이거나 승리 수가 플레이 수보다 크면 거부하는지 검증한다. */
    @Test
    @DisplayName("잘못된 횟수는 예외가 발생한다")
    void invalidCountsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerStats("A", 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStats("A", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStats("A", 1, -1));
    }
}

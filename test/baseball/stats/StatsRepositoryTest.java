package baseball.stats;

import static org.junit.jupiter.api.Assertions.*;

import baseball.model.GameMode;
import baseball.model.Player;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * StatsRepository의 단위 테스트.
 * 대상: recordResult, loadStats (유즈케이스 4)
 * 테스트마다 임시 폴더의 파일을 사용하므로 실제 stats.csv에는 영향을 주지 않는다.
 */
class StatsRepositoryTest {
    /** JUnit이 테스트마다 생성하고 종료 후 삭제하는 임시 폴더 */
    @TempDir
    Path dir;

    private Path file;
    private StatsRepository repository;

    @BeforeEach
    void setUp() {
        file = dir.resolve("stats.csv");
        repository = new StatsRepository(file);
    }

    /**
     * 목록에서 이름이 같은 전적을 찾는다.
     *
     * @param list 전적 목록
     * @param name 찾을 이름
     * @return 찾은 전적
     */
    private static PlayerStats find(List<PlayerStats> list, String name) {
        return list.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
    }

    /** 파일이 없으면 모든 모드가 빈 목록이고 파일을 만들지 않는지 검증한다. */
    @Test
    @DisplayName("파일이 없으면 모든 모드의 전적이 비어 있다")
    void noFileReturnsEmpty() {
        Map<GameMode, List<PlayerStats>> stats = repository.loadStats();

        for (GameMode mode : GameMode.values()) {
            assertTrue(stats.get(mode).isEmpty());
        }
        assertFalse(Files.exists(file));
    }

    /** 1:1 PvP 결과가 두 유저 모두에게 반영되는지 검증한다. */
    @Test
    @DisplayName("1:1 결과는 승자와 패자 모두 플레이 수가 늘어난다")
    void duelRecordsBothPlayers() {
        Player a = new Player("A");
        Player b = new Player("B");
        repository.recordResult(GameMode.DUEL, List.of(a, b), a);

        List<PlayerStats> duel = repository.loadStats().get(GameMode.DUEL);
        assertEquals(new PlayerStats("A", 1, 1), find(duel, "A"));
        assertEquals(new PlayerStats("B", 1, 0), find(duel, "B"));
    }

    /** 다인 PvP는 우승자만 승리로 집계되는지 검증한다. */
    @Test
    @DisplayName("다인은 우승만 승리로 집계한다")
    void multiCountsOnlyWinner() {
        Player a = new Player("A");
        Player b = new Player("B");
        Player c = new Player("C");
        repository.recordResult(GameMode.MULTI, List.of(a, b, c), b);

        List<PlayerStats> multi = repository.loadStats().get(GameMode.MULTI);
        assertEquals(1, find(multi, "A").plays());
        assertEquals(0, find(multi, "A").wins());
        assertEquals(1, find(multi, "B").wins());
        assertEquals(0, find(multi, "C").wins());
    }

    /** PvE 실패는 승리 없이 플레이 수만 늘어나고, 이후 결과가 누적되는지 검증한다. */
    @Test
    @DisplayName("PvE 실패는 플레이 수만 늘어난다")
    void pveFailureAndAccumulation() {
        repository.recordResult(GameMode.PVE, List.of(new Player("A")), null);
        Player again = new Player("A");
        repository.recordResult(GameMode.PVE, List.of(again), again);

        PlayerStats a = find(repository.loadStats().get(GameMode.PVE), "A");
        assertEquals(new PlayerStats("A", 2, 1), a);
        assertEquals(50.0, a.winRate(), 1e-9);
    }

    /** 같은 이름이라도 모드별로 따로 집계되는지 검증한다. */
    @Test
    @DisplayName("전적은 모드별로 따로 집계한다")
    void modesAreSeparate() {
        Player a = new Player("A");
        repository.recordResult(GameMode.DUEL, List.of(a, new Player("B")), a);
        repository.recordResult(GameMode.PVE, List.of(new Player("A")), null);

        Map<GameMode, List<PlayerStats>> stats = repository.loadStats();
        assertEquals(new PlayerStats("A", 1, 1), find(stats.get(GameMode.DUEL), "A"));
        assertEquals(new PlayerStats("A", 1, 0), find(stats.get(GameMode.PVE), "A"));
        assertTrue(stats.get(GameMode.MULTI).isEmpty());
    }

    /** 저장한 전적을 새 저장소 객체로 다시 읽을 수 있는지 검증한다. (프로그램 재실행) */
    @Test
    @DisplayName("저장한 전적은 다시 실행해도 유지된다")
    void persistsAcrossInstances() {
        Player a = new Player("A");
        repository.recordResult(GameMode.DUEL, List.of(a, new Player("B")), a);

        StatsRepository reopened = new StatsRepository(file);
        assertEquals(new PlayerStats("A", 1, 1), find(reopened.loadStats().get(GameMode.DUEL), "A"));
    }

    /** 이름에 쉼표가 있어도 그대로 저장되고 읽히는지 검증한다. */
    @Test
    @DisplayName("쉼표가 들어간 이름도 저장된다")
    void nameWithComma() {
        Player p = new Player("홍,길동");
        repository.recordResult(GameMode.PVE, List.of(p), p);

        assertEquals(new PlayerStats("홍,길동", 1, 1), find(repository.loadStats().get(GameMode.PVE), "홍,길동"));
    }

    /** 형식이 잘못된 줄은 건너뛰고 올바른 줄만 읽는지 검증한다. */
    @Test
    @DisplayName("손상된 줄은 건너뛴다")
    void malformedLinesIgnored() throws IOException {
        Files.write(file, List.of(
                "mode,plays,wins,name",
                "PVE,3,1,A",
                "PVE,x,1,B",
                "UNKNOWN,1,1,C",
                "DUEL,1",
                "MULTI,1,2,D",
                ""), StandardCharsets.UTF_8);

        Map<GameMode, List<PlayerStats>> stats = repository.loadStats();
        assertEquals(List.of(new PlayerStats("A", 3, 1)), stats.get(GameMode.PVE));
        assertTrue(stats.get(GameMode.DUEL).isEmpty());
        assertTrue(stats.get(GameMode.MULTI).isEmpty());
    }

    /** 전적 목록이 승리 수가 많은 순서로 정렬되는지 검증한다. */
    @Test
    @DisplayName("전적은 승리 수가 많은 순서로 정렬한다")
    void sortedByWins() {
        Player a = new Player("A");
        Player b = new Player("B");
        repository.recordResult(GameMode.DUEL, List.of(a, b), b);
        repository.recordResult(GameMode.DUEL, List.of(a, b), b);
        repository.recordResult(GameMode.DUEL, List.of(a, b), a);

        List<PlayerStats> duel = repository.loadStats().get(GameMode.DUEL);
        assertEquals("B", duel.get(0).name());
        assertEquals("A", duel.get(1).name());
    }
}

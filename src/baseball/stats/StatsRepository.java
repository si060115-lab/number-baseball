package baseball.stats;

import baseball.model.GameMode;
import baseball.model.Player;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 모드별 전적을 CSV 파일에 저장하고 조회한다.
 * 파일 형식: 첫 줄은 헤더, 이후 한 줄에 "모드,플레이 수,승리 수,이름" 형식으로 한 유저의 전적을 기록한다.
 * 이름에 쉼표가 들어가도 읽을 수 있도록 이름을 마지막 칸에 둔다.
 * 호출할 때마다 파일을 다시 읽으므로 항상 파일 내용과 같은 결과를 반환한다.
 */
public class StatsRepository {
    private static final String HEADER = "mode,plays,wins,name";
    private static final String DELIMITER = ",";
    private static final int FIELD_COUNT = 4;

    /** 전적 표의 정렬 기준: 승리 수 내림차순, 승률 내림차순, 이름 오름차순 */
    private static final Comparator<PlayerStats> RANKING =
            Comparator.comparingInt(PlayerStats::wins).reversed()
                    .thenComparing(Comparator.comparingDouble(PlayerStats::winRate).reversed())
                    .thenComparing(PlayerStats::name);

    private final Path file;

    /**
     * @param file 전적 파일 경로. 파일이 없으면 첫 저장 시 생성한다.
     */
    public StatsRepository(Path file) {
        this.file = file;
    }

    /**
     * 한 판의 결과를 전적에 반영하고 파일에 저장한다. (함수 명세: recordResult)
     * 참가자 전원의 플레이 수를 1 늘리고, 승자의 승리 수를 1 늘린다.
     * 다인 PvP 모드는 우승자만 승자로 전달되므로 우승만 승리로 집계된다.
     *
     * @param mode    게임 모드
     * @param players 참가 유저 목록 (컴퓨터 제외)
     * @param winner  승자. PvE 실패 시 null
     * @throws IllegalArgumentException winner가 참가 유저가 아닌 경우
     * @throws UncheckedIOException     파일을 읽거나 쓸 수 없는 경우
     */
    public void recordResult(GameMode mode, List<Player> players, Player winner) {
        if (winner != null && !players.contains(winner)) {
            throw new IllegalArgumentException("승자가 참가 유저가 아닙니다: " + winner.getName());
        }
        Map<GameMode, Map<String, PlayerStats>> table = readTable();
        Map<String, PlayerStats> modeStats = table.get(mode);
        for (Player p : players) {
            String name = p.getName();
            PlayerStats before = modeStats.getOrDefault(name, PlayerStats.empty(name));
            modeStats.put(name, before.addResult(p == winner));
        }
        writeTable(table);
    }

    /**
     * 모든 유저의 전적을 모드별로 조회한다. (함수 명세: loadStats)
     * 모든 모드가 키로 포함되며, 기록이 없는 모드는 빈 목록을 가진다.
     *
     * @return 모드별 전적 목록 (승리 수, 승률, 이름 순으로 정렬, 수정 불가)
     * @throws UncheckedIOException 파일을 읽을 수 없는 경우
     */
    public Map<GameMode, List<PlayerStats>> loadStats() {
        Map<GameMode, Map<String, PlayerStats>> table = readTable();
        Map<GameMode, List<PlayerStats>> result = new EnumMap<>(GameMode.class);
        for (GameMode mode : GameMode.values()) {
            List<PlayerStats> list = new ArrayList<>(table.get(mode).values());
            list.sort(RANKING);
            result.put(mode, List.copyOf(list));
        }
        return result;
    }

    /**
     * 파일을 읽어 모드별·이름별 전적 표를 만든다. 파일이 없으면 빈 표를 반환한다.
     *
     * @return 모든 모드를 키로 가지는 전적 표
     * @throws UncheckedIOException 파일을 읽을 수 없는 경우
     */
    private Map<GameMode, Map<String, PlayerStats>> readTable() {
        Map<GameMode, Map<String, PlayerStats>> table = new EnumMap<>(GameMode.class);
        for (GameMode mode : GameMode.values()) {
            table.put(mode, new LinkedHashMap<>());
        }
        if (!Files.exists(file)) {
            return table;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("전적 파일을 읽을 수 없습니다: " + file, e);
        }
        for (String line : lines) {
            if (line.isBlank() || line.equals(HEADER)) {
                continue;
            }
            parseLine(line, table);
        }
        return table;
    }

    /**
     * 한 줄을 읽어 전적 표에 추가한다. 형식이 잘못된 줄은 건너뛴다.
     *
     * @param line  파일의 한 줄
     * @param table 추가할 전적 표
     */
    private static void parseLine(String line, Map<GameMode, Map<String, PlayerStats>> table) {
        // 이름에 쉼표가 있어도 나뉘지 않도록 최대 4칸으로만 나눈다.
        String[] fields = line.split(DELIMITER, FIELD_COUNT);
        if (fields.length != FIELD_COUNT || fields[3].isEmpty()) {
            return;
        }
        try {
            GameMode mode = GameMode.valueOf(fields[0].trim());
            int plays = Integer.parseInt(fields[1].trim());
            int wins = Integer.parseInt(fields[2].trim());
            table.get(mode).put(fields[3], new PlayerStats(fields[3], plays, wins));
        } catch (IllegalArgumentException e) {
            // 알 수 없는 모드, 숫자가 아닌 값(NumberFormatException 포함), 잘못된 횟수는 손상된 줄로 보고 건너뛴다.
        }
    }

    /**
     * 전적 표 전체를 파일에 저장한다.
     * 저장 도중 오류가 나도 기존 파일이 손상되지 않도록 임시 파일에 먼저 쓴 뒤 교체한다.
     *
     * @param table 저장할 전적 표
     * @throws UncheckedIOException 파일을 쓸 수 없는 경우
     */
    private void writeTable(Map<GameMode, Map<String, PlayerStats>> table) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Map.Entry<GameMode, Map<String, PlayerStats>> entry : table.entrySet()) {
            for (PlayerStats s : entry.getValue().values()) {
                lines.add(String.join(DELIMITER, entry.getKey().name(),
                        String.valueOf(s.plays()), String.valueOf(s.wins()), s.name()));
            }
        }

        Path target = file.toAbsolutePath();
        Path temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), "stats", ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("전적 파일을 저장할 수 없습니다: " + file, e);
        } finally {
            deleteQuietly(temp);
        }
    }

    /**
     * 남아 있는 임시 파일을 삭제한다. 삭제 실패는 무시한다.
     *
     * @param path 삭제할 파일. null이면 아무 동작도 하지 않는다.
     */
    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 삭제 실패는 전적 저장 결과에 영향을 주지 않는다.
        }
    }
}

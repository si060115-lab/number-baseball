package baseball.stats;

/**
 * 한 유저의 한 모드 전적.
 * 값을 바꾸지 않는 불변 객체이며, 결과를 반영하면 새 객체를 반환한다.
 *
 * @param name  유저 이름 (전적을 구분하는 기준)
 * @param plays 플레이 수
 * @param wins  승리 수 (다인 PvP는 우승 수)
 */
public record PlayerStats(String name, int plays, int wins) {

    /**
     * 횟수가 올바른지 검사한다.
     *
     * @throws IllegalArgumentException 횟수가 음수이거나 승리 수가 플레이 수보다 큰 경우
     */
    public PlayerStats {
        if (plays < 0 || wins < 0 || wins > plays) {
            throw new IllegalArgumentException("전적 횟수가 올바르지 않습니다: " + plays + "판 " + wins + "승");
        }
    }

    /**
     * 기록이 없는 전적을 생성한다.
     *
     * @param name 유저 이름
     * @return 플레이 수와 승리 수가 0인 전적
     */
    public static PlayerStats empty(String name) {
        return new PlayerStats(name, 0, 0);
    }

    /**
     * 한 판의 결과를 반영한 전적을 반환한다.
     *
     * @param won 승리 여부
     * @return 플레이 수를 1 늘리고, 승리했으면 승리 수도 1 늘린 새 전적
     */
    public PlayerStats addResult(boolean won) {
        return new PlayerStats(name, plays + 1, won ? wins + 1 : wins);
    }

    /**
     * 승률을 백분율로 반환한다.
     *
     * @return 승리 수 / 플레이 수 × 100. 플레이 수가 0이면 0
     */
    public double winRate() {
        return (plays == 0) ? 0.0 : wins * 100.0 / plays;
    }
}

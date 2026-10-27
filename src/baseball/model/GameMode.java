package baseball.model;

/**
 * 게임 모드와 모드별 참가 인원 범위를 정의한다.
 */
public enum GameMode {
    /** 1:1 PvP 모드 (2명) */
    DUEL("1:1 PvP 모드", 2, 2),
    /** 다인 PvP 모드 (3~5명) */
    MULTI("다인 PvP 모드", GameRules.MIN_MULTI_PLAYERS, GameRules.MAX_PLAYERS),
    /** PvE 모드 (1명) */
    PVE("PvE 모드", 1, 1);

    private final String displayName;
    private final int minPlayers;
    private final int maxPlayers;

    GameMode(String displayName, int minPlayers, int maxPlayers) {
        this.displayName = displayName;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    /** @return 화면에 표시할 모드 이름 */
    public String getDisplayName() {
        return displayName;
    }

    /** @return 최소 참가 인원 (컴퓨터 제외) */
    public int getMinPlayers() {
        return minPlayers;
    }

    /** @return 최대 참가 인원 (컴퓨터 제외) */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * 참가 인원이 이 모드의 허용 범위에 속하는지 확인한다.
     *
     * @param count 참가 인원 (컴퓨터 제외)
     * @return 허용 범위이면 true
     */
    public boolean isValidPlayerCount(int count) {
        return count >= minPlayers && count <= maxPlayers;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

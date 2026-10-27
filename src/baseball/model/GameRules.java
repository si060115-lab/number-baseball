package baseball.model;

/**
 * 게임 규칙에 사용되는 상수를 정의한다.
 * 규칙 값은 이 클래스에서만 관리하며, 다른 클래스는 이 상수를 참조한다.
 */
public final class GameRules {
    /** 비밀 숫자의 자릿수 */
    public static final int DIGITS = 3;
    /** 다인 PvP 모드의 최소 인원 */
    public static final int MIN_MULTI_PLAYERS = 3;
    /** 다인 PvP 모드의 최대 인원 */
    public static final int MAX_PLAYERS = 5;
    /** PvE 모드의 최대 시도 횟수 */
    public static final int PVE_MAX_ATTEMPTS = 10;
    /** 한 턴의 제한 시간(초) */
    public static final int TURN_SECONDS = 60;
    /** 유저 이름의 최대 길이 */
    public static final int NAME_MAX_LENGTH = 10;

    /** 상수 전용 클래스이므로 인스턴스 생성을 금지한다. */
    private GameRules() {
    }
}

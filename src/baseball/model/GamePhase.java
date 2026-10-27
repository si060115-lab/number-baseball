package baseball.model;

/**
 * 게임 세션의 진행 단계.
 * SETUP → PLAYING → FINISHED 순서로 진행하며, 도중에 취소되면 ABORTED가 된다.
 */
public enum GamePhase {
    /** 비밀 숫자 설정 단계 */
    SETUP,
    /** 턴 진행 단계 */
    PLAYING,
    /** 정상 종료 (전적 반영 대상) */
    FINISHED,
    /** 무효 처리 (전적 미반영) */
    ABORTED
}

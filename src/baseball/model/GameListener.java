package baseball.model;

import java.util.List;

/**
 * GameSession의 상태 변화를 전달받는 인터페이스 (Observer 패턴).
 * 모델은 화면을 직접 참조하지 않고 이 인터페이스로만 변화를 알린다.
 * 필요한 이벤트만 구현할 수 있도록 기본 구현은 비워 둔다.
 */
public interface GameListener {

    /**
     * 턴 순서가 결정되었을 때 호출된다. 첫 번째 턴이 시작되기 직전에 한 번 발생한다.
     *
     * @param order 턴 순서대로 정렬된 유저 목록
     */
    default void onTurnOrderDecided(List<Player> order) {
    }

    /**
     * 새 턴이 시작되었을 때 호출된다.
     *
     * @param current 턴을 가진 유저
     */
    default void onTurnStarted(Player current) {
    }

    /**
     * 추측 숫자가 판정되었을 때 호출된다.
     *
     * @param attacker 추측한 유저
     * @param target   지목된 상대
     * @param guess    추측한 숫자 (공백 제거)
     * @param result   판정 결과
     */
    default void onGuessJudged(Player attacker, Player target, String guess, GuessResult result) {
    }

    /**
     * 현재 턴의 유저가 기권하거나 제한 시간을 초과했을 때 호출된다.
     * 이후 모드에 따라 탈락 이벤트나 종료 이벤트가 이어서 발생한다.
     *
     * @param player 기권하거나 시간을 초과한 유저
     * @param reason 사유 (기권 또는 시간 초과)
     */
    default void onGaveUp(Player player, String reason) {
    }

    /**
     * 참가자가 탈락했을 때 호출된다. 다인 PvP 모드에서만 발생한다.
     *
     * @param player 탈락한 참가자
     * @param reason 탈락 사유
     */
    default void onEliminated(Player player, String reason) {
    }

    /**
     * 게임이 정상 종료되었을 때 호출된다.
     *
     * @param winner 승자. PvE 모드에서 실패한 경우 null
     */
    default void onFinished(Player winner) {
    }

    /** 게임이 무효 처리되었을 때 호출된다. */
    default void onAborted() {
    }
}

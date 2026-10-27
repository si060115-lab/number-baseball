package baseball.model;

/**
 * 추측 숫자에 대한 판정 결과.
 *
 * @param strike 숫자와 위치가 모두 일치한 개수
 * @param ball   숫자는 일치하나 위치가 다른 개수
 */
public record GuessResult(int strike, int ball) {

    /** 일치하는 숫자가 하나도 없을 때 표시하는 문구 */
    public static final String OUT_TEXT = "OUT!!";

    /**
     * 정답 여부를 반환한다.
     *
     * @return 스트라이크 수가 자릿수와 같으면 true
     */
    public boolean isCorrect() {
        return strike == GameRules.DIGITS;
    }

    /**
     * 아웃 여부를 반환한다.
     *
     * @return 스트라이크와 볼이 모두 0이면 true
     */
    public boolean isOut() {
        return strike == 0 && ball == 0;
    }

    /**
     * 게임 기록 출력 형식으로 변환한다.
     *
     * @return 아웃이면 "OUT!!", 그 외에는 "nS mB" 형식의 문자열 (예: 1S 2B)
     */
    @Override
    public String toString() {
        if (isOut()) {
            return OUT_TEXT;
        }
        return strike + "S " + ball + "B";
    }
}

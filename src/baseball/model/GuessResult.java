package baseball.model;

/**
 * 추측 숫자에 대한 판정 결과.
 *
 * @param strike 숫자와 위치가 모두 일치한 개수
 * @param ball   숫자는 일치하나 위치가 다른 개수
 */
public record GuessResult(int strike, int ball) {

    /**
     * 정답 여부를 반환한다.
     *
     * @return 스트라이크 수가 자릿수와 같으면 true
     */
    public boolean isCorrect() {
        return strike == GameRules.DIGITS;
    }

    /**
     * 게임 기록 출력 형식으로 변환한다.
     *
     * @return "nS mB" 형식의 문자열 (예: 1S 2B)
     */
    @Override
    public String toString() {
        return strike + "S " + ball + "B";
    }
}

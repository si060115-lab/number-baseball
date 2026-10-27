package baseball.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 1~9로 구성된 중복 없는 3자리 숫자를 표현한다.
 * 비밀 숫자와 추측 숫자에 공통으로 사용한다.
 * 생성자를 숨기고 parse(), random()으로만 생성하여 항상 유효한 상태를 보장한다.
 */
public final class SecretNumber {
    /** 각 자리의 숫자 */
    private final int[] digits;

    private SecretNumber(int[] digits) {
        this.digits = digits;
    }

    /**
     * 문자열을 검사하여 SecretNumber로 변환한다.
     * 앞뒤 공백은 제거한 뒤 검사한다.
     *
     * @param input 유저가 입력한 문자열
     * @return 변환된 SecretNumber
     * @throws InvalidInputException 자릿수, 숫자 범위(1~9), 중복 조건 중 하나라도 위반한 경우
     */
    public static SecretNumber parse(String input) {
        // 자릿수 검사
        if (input == null || input.trim().length() != GameRules.DIGITS) {
            throw new InvalidInputException(GameRules.DIGITS + "자리 숫자를 입력하세요.");
        }
        String s = input.trim();
        int[] d = new int[GameRules.DIGITS];

        // 숫자 범위 검사 (0과 숫자가 아닌 문자는 허용하지 않음)
        for (int i = 0; i < d.length; i++) {
            char c = s.charAt(i);
            if (c < '1' || c > '9') {
                throw new InvalidInputException("1~9 사이의 숫자만 입력할 수 있습니다.");
            }
            d[i] = c - '0';
        }

        // 중복 검사 (인덱스를 숫자로 사용)
        boolean[] used = new boolean[10];
        for (int n : d) {
            if (used[n]) {
                throw new InvalidInputException("중복된 숫자는 사용할 수 없습니다.");
            }
            used[n] = true;
        }
        return new SecretNumber(d);
    }

    /**
     * 중복 없는 임의의 숫자를 생성한다. PvE 모드의 컴퓨터 숫자에 사용한다.
     * 1~9를 섞은 뒤 앞에서부터 자릿수만큼 선택하므로 중복이 발생하지 않는다.
     *
     * @param rnd 난수 생성기 (테스트 시 시드를 고정하기 위해 외부에서 주입)
     * @return 생성된 SecretNumber
     */
    public static SecretNumber random(Random rnd) {
        List<Integer> pool = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(pool, rnd);
        int[] d = new int[GameRules.DIGITS];
        for (int i = 0; i < d.length; i++) {
            d[i] = pool.get(i);
        }
        return new SecretNumber(d);
    }

    /**
     * 이 숫자를 정답으로 하여 추측 숫자를 판정한다.
     * 같은 숫자가 같은 위치에 있으면 스트라이크, 다른 위치에 있으면 볼로 계산한다.
     *
     * @param guess 추측한 숫자
     * @return 스트라이크와 볼 개수를 담은 판정 결과
     */
    public GuessResult judge(SecretNumber guess) {
        int strike = 0;
        int ball = 0;
        for (int i = 0; i < digits.length; i++) {
            for (int j = 0; j < guess.digits.length; j++) {
                if (digits[i] == guess.digits[j]) {
                    if (i == j) {
                        strike++;
                    } else {
                        ball++;
                    }
                }
            }
        }
        return new GuessResult(strike, ball);
    }

    /**
     * 숫자를 문자열로 변환한다. PvE 실패 시 정답 표시에 사용한다.
     *
     * @return 각 자리를 이어 붙인 문자열 (예: "472")
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int n : digits) {
            sb.append(n);
        }
        return sb.toString();
    }
}

package baseball.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SecretNumber {
    private final int[] digits;

    private SecretNumber(int[] digits) {
        this.digits = digits;
    }

    public static SecretNumber parse(String input) {
        if (input == null || input.trim().length() != GameRules.DIGITS) {
            throw new InvalidInputException(GameRules.DIGITS + "자리 숫자를 입력하세요.");
        }
        String s = input.trim();
        int[] d = new int[GameRules.DIGITS];
        for (int i = 0; i < d.length; i++) {
            char c = s.charAt(i);
            if (c < '1' || c > '9') {
                throw new InvalidInputException("1~9 사이의 숫자만 입력할 수 있습니다.");
            }
            d[i] = c - '0';
        }
        boolean[] used = new boolean[10];
        for (int n : d) {
            if (used[n]) {
                throw new InvalidInputException("중복된 숫자는 사용할 수 없습니다.");
            }
            used[n] = true;
        }
        return new SecretNumber(d);
    }

    public static SecretNumber random(Random rnd) {
        List<Integer> pool = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(pool, rnd);
        int[] d = new int[GameRules.DIGITS];
        for (int i = 0; i < d.length; i++) {
            d[i] = pool.get(i);
        }
        return new SecretNumber(d);
    }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int n : digits) {
            sb.append(n);
        }
        return sb.toString();
    }
}

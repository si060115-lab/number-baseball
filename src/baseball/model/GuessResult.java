package baseball.model;

public record GuessResult(int strike, int ball) {
    public boolean isCorrect() {
        return strike == GameRules.DIGITS;
    }

    @Override
    public String toString() {
        return strike + "S " + ball + "B";
    }
}

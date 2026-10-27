package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SecretNumberTest {

    @Test
    @DisplayName("올바른 숫자는 통과하고 앞뒤 공백은 무시한다")
    void parseValid() {
        assertEquals("123", SecretNumber.parse("123").toString());
        assertEquals("987", SecretNumber.parse(" 987 ").toString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12", "1234", "12a", "012", "102", "112", "999", "-12", "1 2"})
    @DisplayName("잘못된 형식은 예외가 발생한다")
    void parseRejectsInvalid(String input) {
        assertThrows(InvalidInputException.class, () -> SecretNumber.parse(input));
    }

    @Test
    @DisplayName("중복 숫자는 중복 안내 메시지를 준다")
    void parseDuplicateMessage() {
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> SecretNumber.parse("112"));
        assertEquals("중복된 숫자는 사용할 수 없습니다.", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"123,3,0", "132,1,2", "312,0,3", "231,0,3", "145,1,0", "135,1,1", "456,0,0"})
    @DisplayName("비밀 숫자 123에 대한 스트라이크와 볼 판정")
    void judge(String guess, int strike, int ball) {
        GuessResult r = SecretNumber.parse("123").judge(SecretNumber.parse(guess));
        assertEquals(strike, r.strike());
        assertEquals(ball, r.ball());
    }

    @Test
    @DisplayName("3스트라이크일 때만 정답이다")
    void correctOnlyWithThreeStrikes() {
        assertTrue(new GuessResult(3, 0).isCorrect());
        assertFalse(new GuessResult(2, 0).isCorrect());
        assertEquals("1S 2B", new GuessResult(1, 2).toString());
    }

    @Test
    @DisplayName("랜덤 숫자는 항상 올바른 형식이다")
    void randomAlwaysValid() {
        Random rnd = new Random(42);
        for (int i = 0; i < 1000; i++) {
            String s = SecretNumber.random(rnd).toString();
            assertDoesNotThrow(() -> SecretNumber.parse(s));
        }
    }
}

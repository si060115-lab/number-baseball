package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SecretNumber와 GuessResult의 단위 테스트.
 * 대상 함수: parse, judge, random 
 */
class SecretNumberTest {

    /** 유효한 입력과 앞뒤 공백이 포함된 입력의 변환을 검증한다. */
    @Test
    @DisplayName("올바른 숫자는 통과하고 앞뒤 공백은 무시한다")
    void parseValid() {
        assertEquals("123", SecretNumber.parse("123").toString());
        assertEquals("987", SecretNumber.parse(" 987 ").toString());
    }

    /**
     * 형식 오류 입력이 모두 거부되는지 검증한다.
     * 대상: null, 빈 문자열, 자릿수 오류, 문자 포함, 0 포함, 중복, 음수, 중간 공백
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12", "1234", "12a", "012", "102", "112", "999", "-12", "1 2"})
    @DisplayName("잘못된 형식은 예외가 발생한다")
    void parseRejectsInvalid(String input) {
        assertThrows(InvalidInputException.class, () -> SecretNumber.parse(input));
    }

    /** 중복 숫자 입력 시 경고창에 표시될 메시지를 검증한다. */
    @Test
    @DisplayName("중복 숫자는 중복 안내 메시지를 준다")
    void parseDuplicateMessage() {
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> SecretNumber.parse("112"));
        assertEquals("중복된 숫자는 사용할 수 없습니다.", e.getMessage());
    }

    /**
     * 정답 123에 대한 판정 결과를 검증한다.
     * 각 행의 값: 추측 숫자, 기대 스트라이크, 기대 볼
     */
    @ParameterizedTest
    @CsvSource({"123,3,0", "132,1,2", "312,0,3", "231,0,3", "145,1,0", "135,1,1", "456,0,0"})
    @DisplayName("비밀 숫자 123에 대한 스트라이크와 볼 판정")
    void judge(String guess, int strike, int ball) {
        GuessResult r = SecretNumber.parse("123").judge(SecretNumber.parse(guess));
        assertEquals(strike, r.strike());
        assertEquals(ball, r.ball());
    }

    /** 정답 판정 기준과 출력 형식을 검증한다. */
    @Test
    @DisplayName("3스트라이크일 때만 정답이다")
    void correctOnlyWithThreeStrikes() {
        assertTrue(new GuessResult(3, 0).isCorrect());
        assertFalse(new GuessResult(2, 0).isCorrect());
        assertEquals("1S 2B", new GuessResult(1, 2).toString());
    }
    
    /** 일치하는 숫자가 없으면 OUT!!으로 표시되는지 검증한다. */
    @Test
    @DisplayName("스트라이크와 볼이 모두 0이면 OUT!!으로 표시한다")
    void outWhenNothingMatches() {
        GuessResult r = SecretNumber.parse("123").judge(SecretNumber.parse("456"));
        assertTrue(r.isOut());
        assertEquals("OUT!!", r.toString());
        assertFalse(new GuessResult(0, 1).isOut());
        assertFalse(new GuessResult(1, 0).isOut());
    }

    /**
     * 임의 생성 숫자가 항상 형식 규칙을 만족하는지 검증한다.
     * 시드를 고정하여 실행할 때마다 같은 결과가 나오도록 한다.
     */
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

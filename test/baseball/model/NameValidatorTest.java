package baseball.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * NameValidator의 단위 테스트.
 * 대상: SCR-02 이름 입력값 정의 (1~10자, 빈 이름·중복 이름 불가)
 */
class NameValidatorTest {

    /** 올바른 이름은 앞뒤 공백을 제거하여 그대로 반환하는지 검증한다. */
    @Test
    @DisplayName("올바른 이름은 앞뒤 공백을 제거하여 반환한다")
    void validNamesTrimmed() {
        assertEquals(List.of("홍길동", "김철수"), NameValidator.validate(List.of(" 홍길동 ", "김철수")));
    }

    /** 비어 있거나 공백뿐인 이름이 거부되는지 검증한다. (대안 흐름 1-1) */
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("빈 이름은 예외가 발생한다")
    void blankNameRejected(String blank) {
        assertThrows(InvalidInputException.class, () -> NameValidator.validate(List.of("홍길동", blank)));
    }

    /** 10자는 허용하고 11자는 거부하는지 검증한다. */
    @Test
    @DisplayName("이름은 10자까지 허용한다")
    void lengthLimit() {
        assertDoesNotThrow(() -> NameValidator.validate(List.of("가나다라마바사아자차")));
        assertThrows(InvalidInputException.class, () -> NameValidator.validate(List.of("가나다라마바사아자차카")));
    }

    /** 공백을 제거한 뒤 같은 이름이 있으면 거부하는지 검증한다. (대안 흐름 1-1) */
    @Test
    @DisplayName("같은 이름이 있으면 예외가 발생한다")
    void duplicateNameRejected() {
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> NameValidator.validate(List.of("홍길동", " 홍길동")));
        assertTrue(e.getMessage().contains("같은 이름"));
    }
}

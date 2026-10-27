package baseball.model;

/**
 * 입력값이 형식 규칙을 만족하지 않을 때 발생하는 예외.
 * 컨트롤러는 이 예외를 처리하여 경고 메시지를 출력하고 현재 턴을 유지한다.
 */
public class InvalidInputException extends RuntimeException {

    /**
     * @param message 사용자에게 표시할 오류 메시지
     */
    public InvalidInputException(String message) {
        super(message);
    }
}

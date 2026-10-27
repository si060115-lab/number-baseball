package baseball.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 참가자 이름 목록을 검사한다.
 * 규칙: 앞뒤 공백 제거 후 1~10자, 빈 이름과 중복 이름 불가 (SCR-02 입력값 정의)
 * 이름은 전적을 구분하는 기준이므로 한 판 안에서 중복을 허용하지 않는다.
 */
public final class NameValidator {

    /** 정적 메서드만 제공하므로 인스턴스 생성을 금지한다. */
    private NameValidator() {
    }

    /**
     * 이름 목록을 검사하고 앞뒤 공백을 제거한 목록을 반환한다.
     *
     * @param names 입력한 이름 목록 (입력칸 순서)
     * @return 공백을 제거한 이름 목록
     * @throws InvalidInputException 빈 이름, 길이 초과, 중복 이름이 있는 경우
     */
    public static List<String> validate(List<String> names) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < names.size(); i++) {
            String raw = names.get(i);
            String name = (raw == null) ? "" : raw.trim();
            String label = "유저" + (i + 1);

            if (name.isEmpty()) {
                throw new InvalidInputException(label + "의 이름을 입력하세요.");
            }
            if (name.length() > GameRules.NAME_MAX_LENGTH) {
                throw new InvalidInputException(label + "의 이름은 " + GameRules.NAME_MAX_LENGTH + "자 이하로 입력하세요.");
            }
            if (!seen.add(name)) {
                throw new InvalidInputException("같은 이름이 있습니다: " + name);
            }
            result.add(name);
        }
        return result;
    }
}

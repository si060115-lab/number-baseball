package baseball.model;

import java.util.Objects;

/**
 * 게임 참가자.
 * PvE 모드의 컴퓨터도 Player로 표현하여 유저와 같은 방식으로 판정한다.
 */
public class Player {
    /** 컴퓨터 참가자의 표시 이름 */
    public static final String COMPUTER_NAME = "컴퓨터";

    private final String name;
    private final boolean computer;
    private SecretNumber secret;
    private boolean alive = true;

    /**
     * 유저 참가자를 생성한다.
     *
     * @param name 유저 이름 (형식 검사는 참가자 입력 화면에서 수행)
     */
    public Player(String name) {
        this(name, false);
    }

    private Player(String name, boolean computer) {
        this.name = Objects.requireNonNull(name);
        this.computer = computer;
    }

    /**
     * 컴퓨터 참가자를 생성한다.
     *
     * @param secret 컴퓨터의 비밀 숫자
     * @return 비밀 숫자가 설정된 컴퓨터 참가자
     */
    public static Player computer(SecretNumber secret) {
        Player p = new Player(COMPUTER_NAME, true);
        p.setSecret(secret);
        return p;
    }

    /** @return 참가자 이름 */
    public String getName() {
        return name;
    }

    /** @return 컴퓨터 참가자이면 true */
    public boolean isComputer() {
        return computer;
    }

    /** @return 비밀 숫자. 설정 전이면 null */
    public SecretNumber getSecret() {
        return secret;
    }

    /**
     * 비밀 숫자를 설정한다.
     *
     * @param secret 형식 검사를 통과한 비밀 숫자
     */
    public void setSecret(SecretNumber secret) {
        this.secret = Objects.requireNonNull(secret);
    }

    /** @return 비밀 숫자가 설정되었으면 true */
    public boolean hasSecret() {
        return secret != null;
    }

    /** @return 탈락하지 않았으면 true */
    public boolean isAlive() {
        return alive;
    }

    /** 탈락 처리한다. 다인 PvP 모드와 기권 처리에서 사용한다. */
    public void eliminate() {
        alive = false;
    }

    /** 상대 선택 목록 등에 이름을 표시하기 위해 이름을 반환한다. */
    @Override
    public String toString() {
        return name;
    }
}

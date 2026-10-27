package baseball.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 한 판의 게임 진행 상태와 규칙을 관리한다.
 * 화면과 독립적으로 동작하며, 상태 변화는 GameListener를 통해 알린다.
 * PvE, 1:1 PvP, 다인 PvP 모드를 지원한다.
 */
public class GameSession {
    /** 기권 사유 */
    public static final String REASON_GIVE_UP = "기권";
    /** 제한 시간 초과 사유 */
    public static final String REASON_TIMEOUT = "시간 초과";

    private final GameMode mode;
    /** 참가 유저 (컴퓨터 제외, 입력 순서 유지) */
    private final List<Player> players;
    /** PvE 모드의 상대. 그 외 모드는 null */
    private final Player computer;
    /** 턴 순서. PvP 모드는 턴 시작 시 무작위로 섞는다. */
    private final List<Player> turnOrder;
    /** 컴퓨터 숫자 생성과 턴 순서 결정에 사용하는 난수 생성기 */
    private final Random random;
    private final List<GameListener> listeners = new ArrayList<>();

    private GamePhase phase = GamePhase.SETUP;
    /** turnOrder에서 현재 턴을 가진 유저의 위치 */
    private int turnIndex;
    /** PvE 모드에서 사용한 시도 횟수 (형식 오류 입력은 제외) */
    private int attempts;
    private Player winner;

    /**
     * 게임 세션을 생성한다. PvE 모드는 이 시점에 컴퓨터의 비밀 숫자를 생성한다.
     *
     * @param mode    게임 모드
     * @param players 참가 유저 목록
     * @param rnd     난수 생성기 (테스트 시 시드 고정용)
     * @throws IllegalArgumentException 참가 인원이 모드의 허용 범위를 벗어난 경우
     */
    public GameSession(GameMode mode, List<Player> players, Random rnd) {
        if (!mode.isValidPlayerCount(players.size())) {
            throw new IllegalArgumentException(mode + " 참가 인원이 올바르지 않습니다: " + players.size() + "명");
        }
        this.mode = mode;
        this.players = List.copyOf(players);
        this.turnOrder = new ArrayList<>(players);
        this.random = rnd;
        this.computer = (mode == GameMode.PVE) ? Player.computer(SecretNumber.random(rnd)) : null;
    }

    /**
     * 상태 변화를 전달받을 리스너를 등록한다.
     *
     * @param listener 등록할 리스너
     */
    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    /**
     * 유저의 비밀 숫자를 설정한다. PvP 모드의 설정 단계에서만 사용한다.
     *
     * @param player 비밀 숫자를 설정할 유저
     * @param input  입력한 비밀 숫자 문자열
     * @throws InvalidInputException    input이 형식 규칙을 위반한 경우 (설정되지 않음)
     * @throws IllegalStateException    설정 단계가 아니거나 PvE 모드인 경우
     * @throws IllegalArgumentException 참가자가 아닌 경우
     */
    public void setSecret(Player player, String input) {
        requirePhase(GamePhase.SETUP);
        if (mode == GameMode.PVE) {
            throw new IllegalStateException("PvE 모드는 유저가 비밀 숫자를 설정하지 않습니다.");
        }
        if (!players.contains(player)) {
            throw new IllegalArgumentException("참가자가 아닙니다: " + player.getName());
        }
        player.setSecret(SecretNumber.parse(input));
    }

    /**
     * 모든 유저의 비밀 숫자가 설정되었는지 확인한다.
     *
     * @return 모두 설정되었으면 true. PvE 모드는 설정 단계가 없으므로 항상 true
     */
    public boolean isAllSecretsSet() {
        if (mode == GameMode.PVE) {
            return true;
        }
        for (Player p : players) {
            if (!p.hasSecret()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 턴 진행을 시작한다.
     * PvP 모드는 턴 순서를 무작위로 정한 뒤 순서를 알리고, 첫 번째 턴을 시작한다.
     *
     * @throws IllegalStateException 설정 단계가 아니거나 비밀 숫자를 설정하지 않은 유저가 있는 경우
     */
    public void startTurns() {
        requirePhase(GamePhase.SETUP);
        if (!isAllSecretsSet()) {
            throw new IllegalStateException("비밀 숫자를 설정하지 않은 유저가 있습니다.");
        }
        if (mode != GameMode.PVE) {
            Collections.shuffle(turnOrder, random);
        }
        phase = GamePhase.PLAYING;
        turnIndex = 0;
        List<Player> order = List.copyOf(turnOrder);
        fire(l -> l.onTurnOrderDecided(order));
        fireTurnStarted();
    }

    /**
     * 게임을 무효 처리한다. 무효 처리된 게임은 전적에 반영하지 않는다.
     * 이미 종료되었거나 무효 처리된 게임에서는 아무 동작도 하지 않는다.
     */
    public void abort() {
        if (phase == GamePhase.FINISHED || phase == GamePhase.ABORTED) {
            return;
        }
        phase = GamePhase.ABORTED;
        fire(GameListener::onAborted);
    }

    /**
     * 현재 턴의 유저가 지목할 수 있는 상대 목록을 반환한다. (함수 명세: getTargets)
     * PvE 모드는 컴퓨터만, 그 외 모드는 자기 자신과 탈락한 유저를 제외한 유저를 반환한다.
     *
     * @param attacker 현재 턴의 유저
     * @return 지목 가능한 상대 목록 (입력 순서)
     */
    public List<Player> getTargets(Player attacker) {
        if (mode == GameMode.PVE) {
            return List.of(computer);
        }
        List<Player> targets = new ArrayList<>();
        for (Player p : players) {
            if (p != attacker && p.isAlive()) {
                targets.add(p);
            }
        }
        return targets;
    }

    /**
     * 현재 턴의 유저가 상대의 숫자를 추측한 결과를 처리한다. (함수 명세: guess)
     * 형식 오류인 경우 상태를 변경하지 않으므로 턴과 시도 횟수가 그대로 유지된다.
     * 정답이면 PvE·1:1 PvP는 승리, 다인 PvP는 상대 탈락 후 1명이 남으면 우승으로 처리한다.
     *
     * @param attacker 추측하는 유저
     * @param target   지목된 상대
     * @param input    추측한 숫자 문자열
     * @return 판정 결과
     * @throws InvalidInputException    input이 형식 규칙을 위반한 경우
     * @throws IllegalStateException    진행 중이 아니거나 attacker의 턴이 아닌 경우
     * @throws IllegalArgumentException 지목할 수 없는 상대인 경우
     */
    public GuessResult guess(Player attacker, Player target, String input) {
        requirePhase(GamePhase.PLAYING);
        requireTurn(attacker);
        if (!getTargets(attacker).contains(target)) {
            throw new IllegalArgumentException("지목할 수 없는 상대입니다: " + target.getName());
        }

        // 형식 검사를 먼저 수행하여 오류 시 어떤 상태도 바뀌지 않게 한다.
        SecretNumber guessNumber = SecretNumber.parse(input);
        GuessResult result = target.getSecret().judge(guessNumber);
        String guessText = input.trim();

        if (mode == GameMode.PVE) {
            attempts++;
        }
        fire(l -> l.onGuessJudged(attacker, target, guessText, result));

        if (result.isCorrect()) {
            if (mode == GameMode.MULTI) {
                // 다인 PvP: 숫자를 맞힌 상대를 탈락시키고, 1명만 남으면 우승 처리한다.
                eliminate(target, attacker.getName() + "님이 숫자를 맞힘");
                if (!finishIfLastSurvivor()) {
                    nextTurn();
                }
            } else {
                // PvE·1:1 PvP: 상대의 숫자를 맞힌 유저가 승리한다.
                finish(attacker);
            }
        } else if (mode == GameMode.PVE && attempts >= GameRules.PVE_MAX_ATTEMPTS) {
            // PvE: 시도 횟수를 모두 사용하면 실패
            finish(null);
        } else {
            nextTurn();
        }
        return result;
    }

    /**
     * 현재 턴의 유저가 기권하거나 제한 시간을 초과한 경우를 처리한다. (함수 명세: giveUp)
     * PvE는 실패, 1:1 PvP는 상대 승리, 다인 PvP는 본인 탈락으로 처리한다.
     * 다인 PvP에서 탈락 후 1명이 남으면 그 유저가 우승하고, 아니면 다음 유저에게 턴을 넘긴다.
     *
     * @param player 기권하거나 시간을 초과한 유저
     * @param reason 사유 (REASON_GIVE_UP 또는 REASON_TIMEOUT)
     * @throws IllegalStateException 진행 중이 아니거나 player의 턴이 아닌 경우
     */
    public void giveUp(Player player, String reason) {
        requirePhase(GamePhase.PLAYING);
        requireTurn(player);
        fire(l -> l.onGaveUp(player, reason));

        switch (mode) {
            case PVE -> finish(null);
            case DUEL -> finish(getTargets(player).get(0));
            case MULTI -> {
                eliminate(player, reason);
                if (!finishIfLastSurvivor()) {
                    nextTurn();
                }
            }
        }
    }

    /** @return 게임 모드 */
    public GameMode getMode() {
        return mode;
    }

    /** @return 현재 진행 단계 */
    public GamePhase getPhase() {
        return phase;
    }

    /** @return 참가 유저 목록 (입력 순서, 수정 불가) */
    public List<Player> getPlayers() {
        return players;
    }

    /** @return 탈락하지 않은 유저 목록 (입력 순서) */
    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) {
            if (p.isAlive()) {
                alive.add(p);
            }
        }
        return alive;
    }

    /** @return 탈락한 유저 목록 (입력 순서) */
    public List<Player> getEliminatedPlayers() {
        List<Player> eliminated = new ArrayList<>();
        for (Player p : players) {
            if (!p.isAlive()) {
                eliminated.add(p);
            }
        }
        return eliminated;
    }

    /** @return 턴 순서대로 정렬된 유저 목록 (수정 불가) */
    public List<Player> getTurnOrder() {
        return List.copyOf(turnOrder);
    }

    /** @return PvE 모드의 컴퓨터 참가자. 그 외 모드는 null */
    public Player getComputer() {
        return computer;
    }

    /** @return 현재 턴을 가진 유저 */
    public Player getCurrentPlayer() {
        return turnOrder.get(turnIndex);
    }

    /** @return PvE 모드에서 사용한 시도 횟수 */
    public int getAttempts() {
        return attempts;
    }

    /** @return PvE 모드의 남은 시도 횟수 */
    public int getRemainingAttempts() {
        return GameRules.PVE_MAX_ATTEMPTS - attempts;
    }

    /** @return 승자. 종료 전이거나 PvE 실패 시 null */
    public Player getWinner() {
        return winner;
    }

    /**
     * 참가자를 탈락시키고 탈락 이벤트를 알린다.
     *
     * @param player 탈락할 참가자
     * @param reason 탈락 사유
     */
    private void eliminate(Player player, String reason) {
        player.eliminate();
        fire(l -> l.onEliminated(player, reason));
    }

    /**
     * 생존 유저가 1명이면 그 유저를 우승자로 하여 게임을 종료한다.
     *
     * @return 게임을 종료했으면 true
     */
    private boolean finishIfLastSurvivor() {
        List<Player> alive = getAlivePlayers();
        if (alive.size() == 1) {
            finish(alive.get(0));
            return true;
        }
        return false;
    }

    /**
     * 다음 순서의 생존 유저에게 턴을 넘긴다.
     * 진행 중에는 생존 유저가 항상 2명 이상이므로 반복이 종료된다.
     */
    private void nextTurn() {
        do {
            turnIndex = (turnIndex + 1) % turnOrder.size();
        } while (!turnOrder.get(turnIndex).isAlive());
        fireTurnStarted();
    }

    /**
     * 게임을 정상 종료한다.
     *
     * @param winner 승자. PvE 실패 시 null
     */
    private void finish(Player winner) {
        this.winner = winner;
        phase = GamePhase.FINISHED;
        fire(l -> l.onFinished(winner));
    }

    private void fireTurnStarted() {
        Player current = getCurrentPlayer();
        fire(l -> l.onTurnStarted(current));
    }

    /** 등록된 모든 리스너에게 이벤트를 전달한다. 전달 중 목록이 바뀌어도 안전하도록 복사본을 사용한다. */
    private void fire(Consumer<GameListener> event) {
        for (GameListener l : List.copyOf(listeners)) {
            event.accept(l);
        }
    }

    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new IllegalStateException("현재 단계(" + phase + ")에서는 실행할 수 없습니다.");
        }
    }

    private void requireTurn(Player player) {
        if (player != getCurrentPlayer()) {
            throw new IllegalStateException(player.getName() + "의 턴이 아닙니다.");
        }
    }
}

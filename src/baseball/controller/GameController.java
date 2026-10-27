package baseball.controller;

import baseball.model.GameListener;
import baseball.model.GameMode;
import baseball.model.GamePhase;
import baseball.model.GameRules;
import baseball.model.GameSession;
import baseball.model.GuessResult;
import baseball.model.InvalidInputException;
import baseball.model.Player;
import baseball.view.GamePanel;
import baseball.view.MainFrame;
import baseball.view.SecretInputDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 화면 입력을 GameSession에 전달하고, GameSession의 이벤트를 화면에 반영한다.
 * 게임 시작, 비밀 숫자 설정, 턴 진행, 기권과 시간 초과, 결과 표시, 무효 처리의 흐름을 담당한다.
 * 전적 저장은 7단계에서 추가한다.
 */
public class GameController implements GameListener {
    private final MainFrame frame;
    private final GamePanel gamePanel;
    private final TurnTimer turnTimer;
    private final Random random = new Random();
    private GameSession session;

    /**
     * @param frame     화면 전환과 대화상자의 부모 창으로 사용하는 메인 창
     * @param gamePanel 게임 화면
     */
    public GameController(MainFrame frame, GamePanel gamePanel) {
        this.frame = frame;
        this.gamePanel = gamePanel;
        this.turnTimer = new TurnTimer(gamePanel::showRemainingTime, this::onTimeout);
        gamePanel.setController(this);
    }

    /**
     * 선택한 모드와 이름으로 게임을 시작한다. (함수 명세: startGame)
     * PvE 모드는 바로 턴을 시작하고, PvP 모드는 비밀 숫자 입력 단계로 넘어간다.
     *
     * @param mode  게임 모드
     * @param names 검사를 통과한 참가 유저 이름 목록
     */
    public void startGame(GameMode mode, List<String> names) {
        List<Player> players = new ArrayList<>();
        for (String name : names) {
            players.add(new Player(name));
        }
        session = new GameSession(mode, players, random);
        session.addListener(this);

        gamePanel.reset();
        gamePanel.appendLog("[시스템] " + mode + " 게임을 시작합니다.");
        frame.showGame();

        if (mode == GameMode.PVE) {
            // 개발 확인용 정답 출력 (8단계에서 제거)
            System.out.println("[디버그] 정답: " + session.getComputer().getSecret());
            session.startTurns();
        } else {
            // 게임 화면이 먼저 표시된 뒤 비밀 숫자 입력창을 띄우기 위해 이벤트 큐에 등록한다.
            SwingUtilities.invokeLater(this::runSecretSetup);
        }
    }

    /**
     * 비밀 숫자 입력창에서 확인을 눌렀을 때 호출된다.
     *
     * @param player 비밀 숫자를 설정할 유저
     * @param input  입력한 비밀 숫자
     * @throws InvalidInputException input이 형식 규칙을 위반한 경우 (입력창에서 경고 후 다시 입력)
     */
    public void setSecret(Player player, String input) {
        session.setSecret(player, input);
        gamePanel.appendLog("[시스템] " + player.getName() + "님이 비밀 숫자를 설정했습니다.");
    }

    /** 비밀 숫자 입력창에서 취소를 확정했을 때 호출된다. 게임을 무효 처리한다. (유즈케이스 1·2 대안 흐름 3-2) */
    public void cancelSetup() {
        session.abort();
    }

    /** 시작 화면으로 돌아간다. */
    public void showStartScreen() {
        frame.showStart();
    }

    /**
     * 게임 화면에서 추측 숫자가 제출되었을 때 호출된다.
     * 형식 오류는 경고 메시지로 표시하고 턴, 시도 횟수, 남은 시간은 유지한다.
     *
     * @param input 입력한 추측 숫자
     */
    public void onGuess(String input) {
        if (!isPlaying()) {
            return;
        }
        Player attacker = session.getCurrentPlayer();
        List<Player> targets = session.getTargets(attacker);
        // 상대가 한 명이면(PvE, 1:1 PvP) 자동으로 지정하고, 여러 명이면 선택한 상대를 사용한다.
        Player target = (targets.size() == 1) ? targets.get(0) : gamePanel.getSelectedTarget();
        if (target == null) {
            gamePanel.showWarning("공격할 상대를 선택하세요.");
            return;
        }
        try {
            session.guess(attacker, target, input);
        } catch (InvalidInputException e) {
            gamePanel.showWarning(e.getMessage());
        }
    }

    /**
     * 게임 화면에서 기권 버튼을 눌렀을 때 호출된다.
     * 확인을 받은 뒤 현재 턴의 유저를 기권 처리한다.
     */
    public void onGiveUp() {
        if (!isPlaying()) {
            return;
        }
        Player player = session.getCurrentPlayer();
        if (!gamePanel.confirmGiveUp(player.getName())) {
            return;
        }
        // 확인 창이 열려 있는 동안 시간 초과로 턴이 넘어갔거나 게임이 끝났으면 기권하지 않는다.
        if (!isPlaying() || session.getCurrentPlayer() != player) {
            return;
        }
        session.giveUp(player, GameSession.REASON_GIVE_UP);
    }

    @Override
    public void onTurnOrderDecided(List<Player> order) {
        if (session.getMode() == GameMode.PVE) {
            return;
        }
        String text = "턴 순서: " + joinNames(order, " → ");
        gamePanel.appendLog("[시스템] " + text);
        // 다인 PvP 모드의 상태 표시는 생존/탈락 유저로 사용하므로 1:1 PvP 모드에서만 턴 순서를 표시한다.
        if (session.getMode() == GameMode.DUEL) {
            gamePanel.showStatus(text);
        }
    }

    @Override
    public void onTurnStarted(Player current) {
        gamePanel.showTurn(current);
        switch (session.getMode()) {
            case PVE -> {
                gamePanel.hideTargets();
                gamePanel.showStatus("남은 시도 " + session.getRemainingAttempts() + "/" + GameRules.PVE_MAX_ATTEMPTS);
            }
            case DUEL -> {
                // 1:1 PvP 모드는 상대가 고정되므로 선택할 수 없게 표시한다.
                gamePanel.showTargets(session.getTargets(current), false);
                gamePanel.appendLog("[시스템] " + current.getName() + "님 차례입니다.");
            }
            case MULTI -> {
                gamePanel.showTargets(session.getTargets(current), true);
                gamePanel.showStatus(survivorText());
                gamePanel.appendLog("[시스템] " + current.getName() + "님 차례입니다.");
            }
        }
        gamePanel.setInputEnabled(true);
        // 턴마다 제한 시간을 처음부터 다시 측정한다.
        turnTimer.start();
    }

    @Override
    public void onGuessJudged(Player attacker, Player target, String guess, GuessResult result) {
        gamePanel.appendLog(attacker.getName() + " → " + target.getName() + " : " + guess + "  " + result);
    }

    @Override
    public void onGaveUp(Player player, String reason) {
        gamePanel.appendLog("[시스템] " + player.getName() + "님 " + reason);
    }

    @Override
    public void onEliminated(Player player, String reason) {
        gamePanel.appendLog("[시스템] " + player.getName() + "님 탈락 (" + reason + ")");
    }

    @Override
    public void onFinished(Player winner) {
        turnTimer.stop();
        gamePanel.setInputEnabled(false);
        String message;
        if (winner == null) {
            message = "실패! 정답은 " + session.getComputer().getSecret() + "입니다.";
        } else if (session.getMode() == GameMode.MULTI) {
            message = winner.getName() + "님 우승!";
        } else {
            message = winner.getName() + "님 승리!";
        }
        gamePanel.appendLog("[시스템] " + message);
        // 판정 처리가 모두 끝난 뒤 결과 창을 띄우기 위해 이벤트 큐에 등록한다.
        SwingUtilities.invokeLater(() -> showResult(message));
    }

    @Override
    public void onAborted() {
        turnTimer.stop();
        gamePanel.setInputEnabled(false);
        gamePanel.appendLog("[시스템] 게임이 무효 처리되었습니다.");
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame, "게임이 무효 처리되었습니다.\n시작 화면으로 돌아갑니다.",
                    "게임 무효", JOptionPane.INFORMATION_MESSAGE);
            showStartScreen();
        });
    }

    /** 제한 시간이 모두 지났을 때 TurnTimer가 호출한다. 현재 턴의 유저를 시간 초과로 처리한다. */
    private void onTimeout() {
        if (!isPlaying()) {
            return;
        }
        session.giveUp(session.getCurrentPlayer(), GameSession.REASON_TIMEOUT);
    }

    /** @return 게임이 진행 중이면 true */
    private boolean isPlaying() {
        return session != null && session.getPhase() == GamePhase.PLAYING;
    }

    /**
     * 참가 유저에게 차례대로 비밀 숫자 입력창을 띄운다. (Fig.3 비밀 숫자 설정 반복)
     * 입력창은 모달이므로 한 명의 입력이 끝나야 다음 유저로 넘어간다.
     * 도중에 취소되면 세션이 무효 상태가 되므로 반복을 중단한다.
     */
    private void runSecretSetup() {
        GameSession current = session;
        for (Player p : current.getPlayers()) {
            new SecretInputDialog(frame, this, p).requestSecret();
            if (current.getPhase() != GamePhase.SETUP) {
                return;
            }
        }
        current.startTurns();
    }

    /**
     * 결과 창(SCR-05)을 표시한 뒤 시작 화면으로 돌아간다.
     *
     * @param message 결과 메시지
     */
    private void showResult(String message) {
        JOptionPane.showMessageDialog(frame, message, "게임 결과", JOptionPane.INFORMATION_MESSAGE);
        showStartScreen();
    }

    /** @return 다인 PvP 모드의 상태 표시 문구 (예: 생존: A, B   탈락: C) */
    private String survivorText() {
        List<Player> eliminated = session.getEliminatedPlayers();
        String out = eliminated.isEmpty() ? "없음" : joinNames(eliminated, ", ");
        return "생존: " + joinNames(session.getAlivePlayers(), ", ") + "   탈락: " + out;
    }

    /**
     * 유저 이름을 구분자로 연결한다.
     *
     * @param players   유저 목록
     * @param delimiter 구분자
     * @return 연결한 문자열
     */
    private static String joinNames(List<Player> players, String delimiter) {
        return players.stream().map(Player::getName).collect(Collectors.joining(delimiter));
    }
}

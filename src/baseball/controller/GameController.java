package baseball.controller;

import baseball.model.GameListener;
import baseball.model.GameMode;
import baseball.model.GamePhase;
import baseball.model.GameRules;
import baseball.model.GameSession;
import baseball.model.GuessResult;
import baseball.model.InvalidInputException;
import baseball.model.Player;
import baseball.stats.StatsRepository;
import baseball.view.GamePanel;
import baseball.view.MainFrame;
import baseball.view.SecretInputDialog;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 화면 입력을 GameSession에 전달하고, GameSession의 이벤트를 화면에 반영한다.
 * 게임 시작, 비밀 숫자 설정, 턴 진행, 기권과 시간 초과, 결과 표시와 전적 저장, 무효 처리의 흐름을 담당한다.
 */
public class GameController implements GameListener {
    private final MainFrame frame;
    private final GamePanel gamePanel;
    private final StatsRepository statsRepository;
    private final TurnTimer turnTimer;
    private final Random random = new Random();
    private GameSession session;
    /** 프로그램 종료로 인한 무효 처리 여부. true이면 무효 안내 창을 띄우지 않는다. */
    private boolean exiting;

    /**
     * @param frame           화면 전환과 대화상자의 부모 창으로 사용하는 메인 창
     * @param gamePanel       게임 화면
     * @param statsRepository 게임 결과를 저장할 전적 저장소
     */
    public GameController(MainFrame frame, GamePanel gamePanel, StatsRepository statsRepository) {
        this.frame = frame;
        this.gamePanel = gamePanel;
        this.statsRepository = statsRepository;
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

    /**
     * 게임이 진행 중인지 확인한다. 비밀 숫자 설정 단계도 진행 중으로 본다.
     *
     * @return 설정 단계이거나 턴 진행 중이면 true
     */
    public boolean isGameInProgress() {
        return session != null
                && (session.getPhase() == GamePhase.SETUP || session.getPhase() == GamePhase.PLAYING);
    }

    /**
     * 메인 창의 닫기 요청을 처리한다.
     * 게임이 진행 중이면 확인을 받은 뒤 무효 처리하며, 확인 창이 열려 있는 동안 제한 시간을 멈춘다.
     *
     * @return 프로그램을 종료해도 되면 true
     */
    public boolean handleWindowClosing() {
        if (!isGameInProgress()) {
            return true;
        }
        turnTimer.pause();
        int choice = JOptionPane.showConfirmDialog(frame,
                "게임이 진행 중입니다.\n지금 종료하면 이 게임은 무효 처리되어 전적에 반영되지 않습니다.\n종료하시겠습니까?",
                "종료", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            turnTimer.resume();
            return false;
        }
        exiting = true;
        session.abort();
        return true;
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
        String message = resultMessage(winner);
        gamePanel.appendLog("[시스템] " + message);

        // 결과 창의 확인 여부와 관계없이 기록이 남도록 종료 즉시 저장한다. (유즈케이스 4)
        if (!saveResult(winner)) {
            message += "\n\n전적 저장에 실패하여 이번 결과는 전적에 반영되지 않았습니다.";
        }
        String dialogMessage = message;
        // 판정 처리가 모두 끝난 뒤 결과 창을 띄우기 위해 이벤트 큐에 등록한다.
        SwingUtilities.invokeLater(() -> showResult(dialogMessage));
    }

    @Override
    public void onAborted() {
        turnTimer.stop();
        gamePanel.setInputEnabled(false);
        if (exiting) {
            // 프로그램 종료 중이므로 안내 창을 띄우지 않는다.
            return;
        }
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

    /** @return 게임이 턴 진행 중이면 true */
    private boolean isPlaying() {
        return session != null && session.getPhase() == GamePhase.PLAYING;
    }

    /**
     * 게임 결과를 전적에 저장한다. (함수 명세: recordResult)
     * 저장에 실패해도 게임 진행에는 영향을 주지 않도록 예외를 기록으로만 남긴다.
     *
     * @param winner 승자. PvE 실패 시 null
     * @return 저장에 성공했으면 true
     */
    private boolean saveResult(Player winner) {
        try {
            statsRepository.recordResult(session.getMode(), session.getPlayers(), winner);
            return true;
        } catch (UncheckedIOException e) {
            gamePanel.appendLog("[시스템] " + e.getMessage());
            return false;
        }
    }

    /**
     * 모드와 승자에 맞는 결과 문구를 만든다.
     *
     * @param winner 승자. PvE 실패 시 null
     * @return 결과 문구
     */
    private String resultMessage(Player winner) {
        if (winner == null) {
            return "실패! 정답은 " + session.getComputer().getSecret() + "입니다.";
        }
        if (session.getMode() == GameMode.MULTI) {
            return winner.getName() + "님 우승!";
        }
        return winner.getName() + "님 승리!";
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

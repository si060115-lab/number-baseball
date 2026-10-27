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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 화면 입력을 GameSession에 전달하고, GameSession의 이벤트를 화면에 반영한다.
 * 현재는 PvE 모드의 진행만 담당하며, 화면 전환·타이머·전적 저장은 이후 단계에서 추가한다.
 */
public class GameController implements GameListener {
    private final JFrame frame;
    private final GamePanel gamePanel;
    private final Random random = new Random();
    private GameSession session;

    /**
     * @param frame     결과 창의 부모 창
     * @param gamePanel 게임 화면
     */
    public GameController(JFrame frame, GamePanel gamePanel) {
        this.frame = frame;
        this.gamePanel = gamePanel;
        gamePanel.setController(this);
    }

    /**
     * 선택한 모드와 이름으로 게임을 시작한다. (함수 명세: startGame)
     *
     * @param mode  게임 모드
     * @param names 참가 유저 이름 목록
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

        if (mode == GameMode.PVE) {
            // 개발 확인용 정답 출력 (8단계에서 제거)
            System.out.println("[디버그] 정답: " + session.getComputer().getSecret());
            // PvE 모드는 비밀 숫자 설정 단계가 없으므로 바로 턴을 시작한다.
            session.startTurns();
        }
        // 1:1·다인 모드의 비밀 숫자 입력은 4단계에서 추가한다.
    }

    /**
     * 게임 화면에서 추측 숫자가 제출되었을 때 호출된다.
     * 형식 오류는 경고 메시지로 표시하고 턴과 시도 횟수는 유지한다.
     *
     * @param input 입력한 추측 숫자
     */
    public void onGuess(String input) {
        if (session == null || session.getPhase() != GamePhase.PLAYING) {
            return;
        }
        Player attacker = session.getCurrentPlayer();
        // PvE와 1:1 모드는 상대가 한 명뿐이므로 첫 번째 대상을 사용한다.
        // 다인 모드의 상대 선택은 5단계에서 추가한다.
        Player target = session.getTargets(attacker).get(0);
        try {
            session.guess(attacker, target, input);
        } catch (InvalidInputException e) {
            gamePanel.showWarning(e.getMessage());
        }
    }

    @Override
    public void onTurnStarted(Player current) {
        gamePanel.showTurn(current);
        if (session.getMode() == GameMode.PVE) {
            gamePanel.showStatus("남은 시도 " + session.getRemainingAttempts() + "/" + GameRules.PVE_MAX_ATTEMPTS);
        }
    }

    @Override
    public void onGuessJudged(Player attacker, Player target, String guess, GuessResult result) {
        gamePanel.appendLog(attacker.getName() + " → " + target.getName() + " : " + guess + "  " + result);
    }

    @Override
    public void onEliminated(Player player, String reason) {
        gamePanel.appendLog("[시스템] " + player.getName() + " 탈락 (" + reason + ")");
    }

    @Override
    public void onFinished(Player winner) {
        gamePanel.setInputEnabled(false);
        String message;
        if (winner != null) {
            message = winner.getName() + "님 승리!";
        } else {
            message = "실패! 정답은 " + session.getComputer().getSecret() + "입니다.";
        }
        gamePanel.appendLog("[시스템] " + message);
        // 판정 처리가 모두 끝난 뒤 결과 창을 띄우기 위해 이벤트 큐에 등록한다.
        SwingUtilities.invokeLater(() -> showResult(message));
    }

    /**
     * 결과 창(SCR-05)을 표시한다.
     * 시작 화면은 3단계에서 구현하므로, 현재는 같은 설정으로 새 게임을 시작할지 묻는다.
     *
     * @param message 결과 메시지
     */
    private void showResult(String message) {
        int choice = JOptionPane.showConfirmDialog(frame,
                message + "\n새 게임을 시작하시겠습니까?",
                "게임 결과", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            List<String> names = new ArrayList<>();
            for (Player p : session.getPlayers()) {
                names.add(p.getName());
            }
            startGame(session.getMode(), names);
        }
    }
}

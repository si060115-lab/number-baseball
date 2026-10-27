package baseball;

import baseball.controller.GameController;
import baseball.model.GameMode;
import baseball.view.GamePanel;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 프로그램 진입점.
 * 현재는 임시 이름으로 PvE 모드를 바로 시작한다.
 */
public class Main {
    public static void main(String[] args) {
        // Swing 컴포넌트는 이벤트 디스패치 스레드에서 생성한다.
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("숫자야구 게임");
            GamePanel gamePanel = new GamePanel();
            GameController controller = new GameController(frame, gamePanel);

            frame.setContentPane(gamePanel);
            frame.setSize(600, 450);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            controller.startGame(GameMode.PVE, List.of("플레이어"));
        });
    }
}

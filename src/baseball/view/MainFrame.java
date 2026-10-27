package baseball.view;

import baseball.controller.GameController;
import baseball.model.GameMode;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * 프로그램의 메인 창.
 * CardLayout으로 시작 화면, 참가자 입력 화면, 게임 화면을 전환한다.
 * 화면 전환은 이 클래스의 show 메서드로만 수행한다.
 */
public class MainFrame extends JFrame {
    private static final String START = "start";
    private static final String SETUP = "setup";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final SetupPanel setupPanel;

    /** 화면과 컨트롤러를 생성하고 시작 화면을 표시한다. */
    public MainFrame() {
        super("숫자야구 게임");
        GamePanel gamePanel = new GamePanel();
        GameController controller = new GameController(this, gamePanel);
        setupPanel = new SetupPanel(this, controller);

        root.add(new StartPanel(this), START);
        root.add(setupPanel, SETUP);
        root.add(gamePanel, GAME);
        setContentPane(root);

        setSize(600, 450);
        setMinimumSize(new Dimension(480, 380));
        // 게임 중 창 닫기의 무효 처리는 7단계에서 추가한다.
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        showStart();
    }

    /** 시작 화면(SCR-01)으로 전환한다. */
    public void showStart() {
        cards.show(root, START);
    }

    /**
     * 참가자 입력 화면(SCR-02)으로 전환한다.
     *
     * @param mode 선택한 게임 모드
     */
    public void showSetup(GameMode mode) {
        setupPanel.showSetup(mode);
        cards.show(root, SETUP);
    }

    /** 게임 화면(SCR-04)으로 전환한다. */
    public void showGame() {
        cards.show(root, GAME);
    }

    /** 전적 화면(SCR-06)을 표시한다. 현재는 안내 메시지만 표시하며, 7단계에서 StatsDialog로 교체한다. */
    public void showStats() {
        JOptionPane.showMessageDialog(this, "전적 기능은 7단계에서 구현할 예정입니다.",
                "전적", JOptionPane.INFORMATION_MESSAGE);
    }
}

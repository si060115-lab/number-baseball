package baseball.view;

import baseball.controller.GameController;
import baseball.model.GameMode;
import baseball.stats.PlayerStats;
import baseball.stats.StatsRepository;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * 프로그램의 메인 창.
 * CardLayout으로 시작 화면, 참가자 입력 화면, 게임 화면을 전환하고, 전적 화면 표시와 창 닫기를 처리한다.
 * 화면 전환은 이 클래스의 show 메서드로만 수행한다.
 */
public class MainFrame extends JFrame {
    private static final String START = "start";
    private static final String SETUP = "setup";
    private static final String GAME = "game";
    /** 전적 파일 경로. 프로그램을 실행한 위치(작업 디렉터리)에 생성된다. */
    private static final String STATS_FILE = "stats.csv";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final StatsRepository statsRepository = new StatsRepository(Path.of(STATS_FILE));
    private final GameController controller;
    private final SetupPanel setupPanel;

    /** 화면과 컨트롤러를 생성하고 시작 화면을 표시한다. */
    public MainFrame() {
        super("숫자야구 게임");
        GamePanel gamePanel = new GamePanel();
        controller = new GameController(this, gamePanel, statsRepository);
        setupPanel = new SetupPanel(this, controller);

        root.add(new StartPanel(this), START);
        root.add(setupPanel, SETUP);
        root.add(gamePanel, GAME);
        setContentPane(root);

        setSize(600, 450);
        setMinimumSize(new Dimension(480, 380));
        // 게임 중 창 닫기를 확인 후 무효 처리하기 위해 닫기 동작을 직접 처리한다.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestExit();
            }
        });
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

    /** 전적을 불러와 전적 화면(SCR-06)을 표시한다. 파일을 읽을 수 없으면 오류 메시지를 표시한다. */
    public void showStats() {
        Map<GameMode, List<PlayerStats>> stats;
        try {
            stats = statsRepository.loadStats();
        } catch (UncheckedIOException e) {
            JOptionPane.showMessageDialog(this, "전적을 불러올 수 없습니다.\n" + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new StatsDialog(this, stats).setVisible(true);
    }

    /** 종료를 요청한다. 게임이 진행 중이면 컨트롤러가 확인을 받은 뒤 무효 처리한다. */
    private void requestExit() {
        if (!controller.handleWindowClosing()) {
            return;
        }
        dispose();
        System.exit(0);
    }
}

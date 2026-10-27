package baseball.view;

import baseball.model.GameMode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 시작 화면 (SCR-01).
 * 모드 버튼을 누르면 해당 모드의 참가자 입력 화면으로, 전적 버튼을 누르면 전적 화면으로 이동한다.
 */
public class StartPanel extends JPanel {

    /**
     * @param frame 화면 전환을 담당하는 메인 창
     */
    public StartPanel(MainFrame frame) {
        // GridBagLayout은 내용을 화면 가운데에 배치한다.
        super(new GridBagLayout());

        // 제목(①)
        JLabel title = new JLabel("숫자야구 게임", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // 1:1 PvP(②), 다인 PvP(③), PvE(④), 전적(⑤)
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 10));
        buttons.add(createModeButton(frame, GameMode.DUEL));
        buttons.add(createModeButton(frame, GameMode.MULTI));
        buttons.add(createModeButton(frame, GameMode.PVE));
        JButton statsButton = createButton("전적");
        statsButton.addActionListener(e -> frame.showStats());
        buttons.add(statsButton);

        JPanel box = new JPanel(new BorderLayout());
        box.add(title, BorderLayout.NORTH);
        box.add(buttons, BorderLayout.CENTER);
        add(box);
    }

    /**
     * 누르면 해당 모드의 참가자 입력 화면으로 이동하는 버튼을 만든다.
     *
     * @param frame 메인 창
     * @param mode  버튼에 연결할 모드
     * @return 생성한 버튼
     */
    private JButton createModeButton(MainFrame frame, GameMode mode) {
        JButton button = createButton(mode.getDisplayName());
        button.addActionListener(e -> frame.showSetup(mode));
        return button;
    }

    /**
     * 시작 화면 공통 크기와 글꼴의 버튼을 만든다.
     *
     * @param text 버튼 문구
     * @return 생성한 버튼
     */
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 16f));
        button.setPreferredSize(new Dimension(220, 44));
        return button;
    }
}

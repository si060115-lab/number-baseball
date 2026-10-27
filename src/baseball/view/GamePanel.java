package baseball.view;

import baseball.controller.GameController;
import baseball.model.Player;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * 게임 화면 (SCR-04).
 * 현재 턴, 상태 표시, 게임 기록, 추측 입력 영역으로 구성된다.
 * 입력은 GameController로 전달하고, 표시 내용은 GameController의 호출로만 갱신한다.
 * 남은 시간(②), 상대 선택(⑤), 기권(⑧)은 이후 단계에서 추가한다.
 */
public class GamePanel extends JPanel {
    private final JLabel turnLabel = new JLabel("현재 턴: -");
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final JTextField guessField = new JTextField(6);
    private final JButton guessButton = new JButton("확인");
    private GameController controller;

    public GamePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 상단: 현재 턴(①), 상태 표시(③)
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.add(turnLabel);
        top.add(statusLabel);
        add(top, BorderLayout.NORTH);

        // 중앙: 게임 기록(④)
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 하단: 추측 입력(⑥), 확인(⑦)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("추측"));
        bottom.add(guessField);
        bottom.add(guessButton);
        add(bottom, BorderLayout.SOUTH);

        // 확인 버튼과 Enter 키를 같은 동작으로 처리한다.
        guessButton.addActionListener(e -> submitGuess());
        guessField.addActionListener(e -> submitGuess());
    }

    /**
     * 입력 이벤트를 전달할 컨트롤러를 지정한다.
     *
     * @param controller 게임 컨트롤러
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }

    /** 새 게임을 위해 기록과 상태를 초기화하고 입력을 활성화한다. */
    public void reset() {
        logArea.setText("");
        statusLabel.setText(" ");
        setInputEnabled(true);
        guessField.requestFocusInWindow();
    }

    /**
     * 현재 턴을 가진 유저를 표시한다.
     *
     * @param current 턴을 가진 유저
     */
    public void showTurn(Player current) {
        turnLabel.setText("현재 턴: " + current.getName());
    }

    /**
     * 상태 표시 영역의 내용을 바꾼다. (PvE: 남은 시도 횟수, 다인: 생존/탈락 유저)
     *
     * @param text 표시할 내용
     */
    public void showStatus(String text) {
        statusLabel.setText(text);
    }

    /**
     * 게임 기록에 한 줄을 추가하고 마지막 줄로 스크롤한다.
     *
     * @param line 추가할 내용
     */
    public void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * 경고 메시지를 표시한 뒤 입력칸으로 포커스를 되돌린다.
     *
     * @param message 경고 내용
     */
    public void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
        guessField.requestFocusInWindow();
    }

    /**
     * 추측 입력 가능 여부를 설정한다. 게임 종료 후에는 비활성화한다.
     *
     * @param enabled 입력 가능 여부
     */
    public void setInputEnabled(boolean enabled) {
        guessField.setEnabled(enabled);
        guessButton.setEnabled(enabled);
    }

    /** 입력값을 컨트롤러에 전달하고 다음 입력을 위해 입력칸을 비운다. */
    private void submitGuess() {
        if (controller == null) {
            return;
        }
        String input = guessField.getText();
        guessField.setText("");
        guessField.requestFocusInWindow();
        controller.onGuess(input);
    }
}

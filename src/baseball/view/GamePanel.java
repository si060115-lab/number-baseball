package baseball.view;

import baseball.controller.GameController;
import baseball.model.Player;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * 게임 화면 (SCR-04).
 * 현재 턴, 남은 시간, 상태 표시, 게임 기록, 상대 선택, 추측 입력, 기권 영역으로 구성된다.
 * 입력은 GameController로 전달하고, 표시 내용은 GameController의 호출로만 갱신한다.
 */
public class GamePanel extends JPanel {
    /** 남은 시간을 강조 표시하기 시작하는 기준 (초) */
    private static final int WARNING_SECONDS = 10;

    private final JLabel turnLabel = new JLabel("현재 턴: -");
    private final JLabel timeLabel = new JLabel("남은 시간: -");
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final JLabel targetLabel = new JLabel("상대");
    private final JComboBox<Player> targetBox = new JComboBox<>();
    private final JTextField guessField = new JTextField(6);
    private final JButton guessButton = new JButton("확인");
    private final JButton giveUpButton = new JButton("기권");
    /** 남은 시간 라벨의 기본 글자색 */
    private final Color normalTimeColor;
    private GameController controller;
    /** 상대 선택 가능 여부 (다인 PvP 모드에서만 true) */
    private boolean targetSelectable;

    public GamePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 상단: 현재 턴(①), 남은 시간(②), 상태 표시(③)
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 16f));
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD, 16f));
        normalTimeColor = timeLabel.getForeground();
        JPanel turnRow = new JPanel(new BorderLayout());
        turnRow.add(turnLabel, BorderLayout.WEST);
        turnRow.add(timeLabel, BorderLayout.EAST);
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.add(turnRow);
        top.add(statusLabel);
        add(top, BorderLayout.NORTH);

        // 중앙: 게임 기록(④)
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 하단: 상대 선택(⑤), 추측 입력(⑥), 확인(⑦), 기권(⑧)
        // 이름 최대 길이(10자)에 맞춰 상대 선택 목록의 너비를 고정한다.
        targetBox.setPrototypeDisplayValue(new Player("가나다라마바사아자차"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(targetLabel);
        bottom.add(targetBox);
        bottom.add(new JLabel("추측"));
        bottom.add(guessField);
        bottom.add(guessButton);
        bottom.add(giveUpButton);
        add(bottom, BorderLayout.SOUTH);

        // 확인 버튼과 Enter 키를 같은 동작으로 처리한다.
        guessButton.addActionListener(e -> submitGuess());
        guessField.addActionListener(e -> submitGuess());
        giveUpButton.addActionListener(e -> {
            if (controller != null) {
                controller.onGiveUp();
            }
        });
        hideTargets();
    }

    /**
     * 입력 이벤트를 전달할 컨트롤러를 지정한다.
     *
     * @param controller 게임 컨트롤러
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }

    /** 새 게임을 위해 표시 내용을 초기화한다. 입력은 첫 턴이 시작될 때 활성화한다. */
    public void reset() {
        turnLabel.setText("현재 턴: -");
        timeLabel.setText("남은 시간: -");
        timeLabel.setForeground(normalTimeColor);
        statusLabel.setText(" ");
        logArea.setText("");
        hideTargets();
        setInputEnabled(false);
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
     * 남은 시간을 표시한다. 기준 시간 이하가 되면 빨간색으로 강조한다.
     *
     * @param seconds 남은 시간 (초)
     */
    public void showRemainingTime(int seconds) {
        timeLabel.setText("남은 시간: " + seconds + "초");
        timeLabel.setForeground(seconds <= WARNING_SECONDS ? Color.RED : normalTimeColor);
    }

    /**
     * 상태 표시 영역의 내용을 바꾼다. (PvE: 남은 시도 횟수, 1:1: 턴 순서, 다인: 생존/탈락 유저)
     *
     * @param text 표시할 내용
     */
    public void showStatus(String text) {
        statusLabel.setText(text);
    }

    /**
     * 상대 선택 목록을 표시한다.
     *
     * @param targets    지목 가능한 상대 목록 (GameSession.getTargets 결과)
     * @param selectable 직접 선택 가능 여부. 1:1 PvP는 상대가 고정이므로 false
     */
    public void showTargets(List<Player> targets, boolean selectable) {
        targetBox.removeAllItems();
        for (Player p : targets) {
            targetBox.addItem(p);
        }
        targetSelectable = selectable;
        targetLabel.setVisible(true);
        targetBox.setVisible(true);
        targetBox.setEnabled(selectable);
    }

    /** 상대 선택 목록을 숨긴다. PvE 모드에서 사용한다. */
    public void hideTargets() {
        targetBox.removeAllItems();
        targetSelectable = false;
        targetLabel.setVisible(false);
        targetBox.setVisible(false);
    }

    /** @return 선택된 상대. 선택된 상대가 없으면 null */
    public Player getSelectedTarget() {
        return (Player) targetBox.getSelectedItem();
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
     * 기권 여부를 확인한다. 실수로 누르는 경우를 막기 위해 사용한다.
     *
     * @param name 기권하려는 유저 이름
     * @return 기권을 확정했으면 true
     */
    public boolean confirmGiveUp(String name) {
        int choice = JOptionPane.showConfirmDialog(this,
                name + "님, 기권하시겠습니까?",
                "기권", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        guessField.requestFocusInWindow();
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * 추측 입력과 기권 가능 여부를 설정한다. 설정 단계와 게임 종료 후에는 비활성화한다.
     * 상대 선택 목록은 선택 가능한 모드에서만 함께 활성화한다.
     *
     * @param enabled 입력 가능 여부
     */
    public void setInputEnabled(boolean enabled) {
        guessField.setEnabled(enabled);
        guessButton.setEnabled(enabled);
        giveUpButton.setEnabled(enabled);
        targetBox.setEnabled(enabled && targetSelectable);
        if (enabled) {
            guessField.requestFocusInWindow();
        }
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

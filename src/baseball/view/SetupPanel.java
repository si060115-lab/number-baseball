package baseball.view;

import baseball.controller.GameController;
import baseball.model.GameMode;
import baseball.model.GameRules;
import baseball.model.InvalidInputException;
import baseball.model.NameValidator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * 참가자 입력 화면 (SCR-02).
 * 선택한 모드에 맞게 인원수와 이름 입력칸을 표시하고, 이름 검사를 통과하면 게임을 시작한다.
 * 인원수는 다인 PvP 모드에서만 표시하며, 1:1 PvP는 2칸, PvE는 1칸을 고정으로 표시한다.
 */
public class SetupPanel extends JPanel {
    private final GameController controller;
    private final JLabel modeLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel countLabel = new JLabel("인원수");
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(
            GameRules.MIN_MULTI_PLAYERS, GameRules.MIN_MULTI_PLAYERS, GameRules.MAX_PLAYERS, 1));
    private final JLabel[] nameLabels = new JLabel[GameRules.MAX_PLAYERS];
    private final JTextField[] nameFields = new JTextField[GameRules.MAX_PLAYERS];
    private GameMode mode = GameMode.PVE;

    /**
     * @param frame      취소 시 시작 화면으로 돌아가기 위한 메인 창
     * @param controller 게임 시작을 요청할 컨트롤러
     */
    public SetupPanel(MainFrame frame, GameController controller) {
        super(new BorderLayout(8, 8));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 모드 표시(①)
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(modeLabel, BorderLayout.NORTH);

        // 인원수(②), 이름 입력칸(③)
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 0;
        form.add(countLabel, c);
        c.gridx = 1;
        form.add(countSpinner, c);
        for (int i = 0; i < nameFields.length; i++) {
            nameLabels[i] = new JLabel("유저" + (i + 1));
            nameFields[i] = new JTextField(12);
            // 이름 입력칸에서 Enter를 누르면 시작 버튼과 같은 동작을 한다.
            nameFields[i].addActionListener(e -> onStart());
            c.gridy = i + 1;
            c.gridx = 0;
            form.add(nameLabels[i], c);
            c.gridx = 1;
            form.add(nameFields[i], c);
        }
        add(form, BorderLayout.CENTER);

        // 시작(④), 취소(⑤)
        JButton startButton = new JButton("시작");
        JButton cancelButton = new JButton("취소");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.add(startButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        // 직접 입력을 막아 3~5 범위 밖의 값은 선택할 수 없게 한다. (SCR-02 입력값 정의)
        ((JSpinner.DefaultEditor) countSpinner.getEditor()).getTextField().setEditable(false);
        countSpinner.addChangeListener(e -> updateNameFields());
        startButton.addActionListener(e -> onStart());
        cancelButton.addActionListener(e -> frame.showStart());
    }

    /**
     * 선택한 모드에 맞게 화면을 초기화한다. 화면에 들어올 때마다 호출된다.
     *
     * @param mode 선택한 게임 모드
     */
    public void showSetup(GameMode mode) {
        this.mode = mode;
        modeLabel.setText(mode.getDisplayName());

        boolean multi = (mode == GameMode.MULTI);
        countLabel.setVisible(multi);
        countSpinner.setVisible(multi);
        if (multi) {
            countSpinner.setValue(GameRules.MIN_MULTI_PLAYERS);
        }
        for (JTextField field : nameFields) {
            field.setText("");
        }
        updateNameFields();
        // 화면 전환이 끝난 뒤 첫 번째 이름 입력칸으로 포커스를 옮긴다.
        SwingUtilities.invokeLater(() -> nameFields[0].requestFocusInWindow());
    }

    /** @return 현재 모드의 참가 인원 (다인 PvP는 스피너 값, 그 외는 고정 인원) */
    private int getPlayerCount() {
        if (mode == GameMode.MULTI) {
            return (Integer) countSpinner.getValue();
        }
        return mode.getMinPlayers();
    }

    /** 참가 인원만큼 이름 입력칸을 표시하고 나머지는 숨긴다. */
    private void updateNameFields() {
        int count = getPlayerCount();
        for (int i = 0; i < nameFields.length; i++) {
            boolean visible = i < count;
            nameLabels[i].setVisible(visible);
            nameFields[i].setVisible(visible);
        }
        revalidate();
        repaint();
    }

    /**
     * 표시된 입력칸의 이름을 검사한다. (Fig.2 validateNames)
     *
     * @return 공백을 제거한 이름 목록
     * @throws InvalidInputException 빈 이름, 길이 초과, 중복 이름이 있는 경우
     */
    private List<String> validateNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < getPlayerCount(); i++) {
            names.add(nameFields[i].getText());
        }
        return NameValidator.validate(names);
    }

    /** 이름 검사를 통과하면 게임을 시작하고, 실패하면 경고 후 다시 입력을 기다린다. (유즈케이스 대안 흐름 1-1, 1-2) */
    private void onStart() {
        List<String> names;
        try {
            names = validateNames();
        } catch (InvalidInputException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.startGame(mode, names);
    }
}

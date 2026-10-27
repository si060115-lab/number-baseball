package baseball.view;

import baseball.controller.GameController;
import baseball.model.InvalidInputException;
import baseball.model.Player;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * 비밀 숫자 입력창 (SCR-03).
 * 입력한 숫자는 ●로 가려지며, 형식이 틀리면 경고 후 다시 입력을 기다린다.
 * 취소하거나 창을 닫으면 확인을 거쳐 게임을 무효 처리한다.
 * 모달 대화상자이므로 이 창이 닫히기 전에는 다른 화면을 조작할 수 없다.
 */
public class SecretInputDialog extends JDialog {
    private final GameController controller;
    private final Player player;
    private final JPasswordField secretField = new JPasswordField(6);

    /**
     * @param owner      부모 창
     * @param controller 입력 결과를 전달할 컨트롤러
     * @param player     비밀 숫자를 입력할 유저
     */
    public SecretInputDialog(JFrame owner, GameController controller, Player player) {
        super(owner, "비밀 숫자 설정", true);
        this.controller = controller;
        this.player = player;

        // 안내(①)
        JLabel notice = new JLabel(player.getName() + "님만 화면을 보세요", SwingConstants.CENTER);
        notice.setFont(notice.getFont().deriveFont(Font.BOLD, 15f));

        // 비밀 숫자(②)
        secretField.setEchoChar('●');
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputRow.add(new JLabel("비밀 숫자"));
        inputRow.add(secretField);
        JLabel hint = new JLabel("1~9 중 서로 다른 숫자 3자리", SwingConstants.CENTER);
        hint.setForeground(Color.GRAY);
        JPanel center = new JPanel(new GridLayout(2, 1));
        center.add(inputRow);
        center.add(hint);

        // 확인(③), 취소(④)
        JButton okButton = new JButton("확인");
        JButton cancelButton = new JButton("취소");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.add(okButton);
        buttons.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        content.add(notice, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        okButton.addActionListener(e -> onConfirm());
        secretField.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> onCancel());

        // 창 닫기(X)도 취소와 같이 처리한다.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** 입력창을 표시하고 창이 닫힐 때까지 대기한다. (Fig.3 requestSecret) */
    public void requestSecret() {
        setVisible(true);
    }

    /** 입력값을 컨트롤러에 전달한다. 형식 오류면 경고 후 입력칸을 비우고 다시 입력을 기다린다. */
    private void onConfirm() {
        String input = new String(secretField.getPassword());
        try {
            controller.setSecret(player, input);
            dispose();
        } catch (InvalidInputException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 오류", JOptionPane.WARNING_MESSAGE);
            secretField.setText("");
            secretField.requestFocusInWindow();
        }
    }

    /** 실수로 누르는 경우를 막기 위해 확인을 받은 뒤 게임을 무효 처리한다. (대안 흐름 3-2) */
    private void onCancel() {
        int choice = JOptionPane.showConfirmDialog(this,
                "입력을 취소하면 게임이 무효 처리됩니다.\n취소하시겠습니까?",
                "입력 취소", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            controller.cancelSetup();
            dispose();
        } else {
            secretField.requestFocusInWindow();
        }
    }
}

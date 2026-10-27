package baseball.view;

import baseball.model.GameMode;
import baseball.stats.PlayerStats;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * 전적 화면 (SCR-06).
 * 모드별 탭에 전체 유저의 이름, 플레이 수, 승리 수, 승률을 표로 표시한다.
 * 표는 조회 전용이며 수정할 수 없다.
 */
public class StatsDialog extends JDialog {
    private static final String[] COLUMNS = {"이름", "플레이", "승리", "승률"};

    /**
     * @param owner 부모 창
     * @param stats 모드별 전적 목록 (StatsRepository.loadStats 결과)
     */
    public StatsDialog(JFrame owner, Map<GameMode, List<PlayerStats>> stats) {
        super(owner, "전적", true);

        // 모드 탭(①), 전적 표(②)
        JTabbedPane tabs = new JTabbedPane();
        for (GameMode mode : GameMode.values()) {
            tabs.addTab(mode.getDisplayName(), createStatsView(stats.getOrDefault(mode, List.of())));
        }

        // 닫기(③)
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(closeButton);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(tabs, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(440, 320);
        setLocationRelativeTo(owner);
    }

    /**
     * 한 모드의 전적 표를 만든다. 기록이 없으면 안내 문구를 표시한다.
     *
     * @param list 정렬된 전적 목록
     * @return 탭에 넣을 화면 구성 요소
     */
    private JComponent createStatsView(List<PlayerStats> list) {
        if (list.isEmpty()) {
            return new JLabel("기록이 없습니다.", SwingConstants.CENTER);
        }

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (PlayerStats s : list) {
            model.addRow(new Object[] {s.name(), s.plays(), s.wins(), String.format("%.1f%%", s.winRate())});
        }

        JTable table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        // 이름을 제외한 숫자 열은 가운데 정렬한다.
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int column = 1; column < COLUMNS.length; column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(center);
        }
        return new JScrollPane(table);
    }
}

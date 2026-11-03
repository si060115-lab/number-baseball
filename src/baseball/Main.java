package baseball;

import baseball.view.MainFrame;
import javax.swing.SwingUtilities;

/**
 * 프로그램 진입점. 메인 창을 생성하고 시작 화면을 표시한다.<>
 */
public class Main {
    public static void main(String[] args) {
        // Swing 컴포넌트는 이벤트 디스패치 스레드에서 생성한다.
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

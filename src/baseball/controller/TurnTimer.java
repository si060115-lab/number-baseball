package baseball.controller;

import java.util.function.IntConsumer;
import javax.swing.Timer;

/**
 * 턴 제한 시간을 측정한다.
 * javax.swing.Timer로 1초마다 남은 시간을 알리고, 시간이 모두 지나면 시간 초과를 알린다.
 * 콜백은 이벤트 디스패치 스레드에서 호출되므로 화면을 바로 갱신할 수 있다.
 */
public class TurnTimer {
    /** 한 턴의 제한 시간 (초) */
    public static final int TIME_LIMIT_SECONDS = 60;
    private static final int TICK_MILLIS = 1000;

    private final Timer timer;
    private final IntConsumer onTick;
    private final Runnable onTimeout;
    private int remaining;

    /**
     * @param onTick    남은 시간(초)이 바뀔 때마다 호출할 동작
     * @param onTimeout 제한 시간이 모두 지났을 때 호출할 동작
     */
    public TurnTimer(IntConsumer onTick, Runnable onTimeout) {
        this.onTick = onTick;
        this.onTimeout = onTimeout;
        this.timer = new Timer(TICK_MILLIS, e -> tick());
    }

    /** 제한 시간을 처음부터 다시 측정한다. 턴이 시작될 때마다 호출한다. */
    public void start() {
        remaining = TIME_LIMIT_SECONDS;
        onTick.accept(remaining);
        timer.restart();
    }

    /** 시간 측정을 중지한다. 게임이 종료되거나 무효 처리될 때 호출한다. */
    public void stop() {
        timer.stop();
    }

    /** @return 남은 시간 (초) */
    public int getRemaining() {
        return remaining;
    }

    /**
     * 1초마다 남은 시간을 줄이고, 0이 되면 측정을 멈춘 뒤 시간 초과를 알린다.
     * 시간 초과 처리 중 다음 턴이 시작되면 start()로 다시 측정을 시작한다.
     */
    private void tick() {
        remaining--;
        onTick.accept(remaining);
        if (remaining <= 0) {
            timer.stop();
            onTimeout.run();
        }
    }
}

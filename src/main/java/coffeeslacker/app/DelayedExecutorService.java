package coffeeslacker.app;

import java.util.concurrent.TimeUnit;

public interface DelayedExecutorService {
    public void schedule(Runnable pRunnable, long pDelay, TimeUnit pDelayTimeUnit);
}

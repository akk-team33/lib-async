package net.team33.async.consumer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to launch {@link Runnable} instances within a distinct named new Thread
 */
public class Launcher {

    private static final String NAME_FORMAT = "%s[%d:%d]";

    /**
     * Number of totally created instances of this class.
     * May overflow (after several million years ;-)
     * Anyway - doesn't matter!
     */
    private static final AtomicLong INSTANCES = new AtomicLong(0);

    /**
     * 1-based unique index number of this instance.
     * Used to generate unique names for new worker threads.
     */
    private final long instance = INSTANCES.incrementAndGet();

    /**
     * Number of totally started threads (by this instance).
     * May overflow (after several million years ;-)
     * Anyway - doesn't matter!
     * Used to generate unique names for new worker threads.
     */
    private final AtomicLong started = new AtomicLong(0);

    public final Thread launch(final Runnable worker) {
        return launch(worker, instance, started.incrementAndGet());
    }

    private static Thread launch(final Runnable runnable, final long major, final long minor) {
        return launch(runnable, String.format(NAME_FORMAT, runnable.getClass().getName(), major, minor));
    }

    private static Thread launch(final Runnable runnable, final String name) {
        return launch(new Thread(runnable, name));
    }

    private static Thread launch(final Thread thread) {
        thread.start();
        return thread;
    }
}

package net.team33.async.consumer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Service to launch {@link Runnable} instances within a distinct named new Thread
 */
class Launcher {

    /**
     * Number of totally created instances of this class.
     * May overflow (after several million years ;-)
     * Anyway - doesn't matter!
     */
    private static final AtomicLong INSTANCES = new AtomicLong(0);
    private static final String TO_STRING_FORMAT = "Launcher(instance(%s), started(%s))";
    private static final String NAME_FORMAT = "%s[%s:%s]";

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

    private final BiFunction<Runnable, String, Thread> newThread;

    Launcher(BiFunction<Runnable, String, Thread> newThread) {
        this.newThread = requireNonNull(newThread);
    }

    private static Thread start(final Thread thread) {
        thread.start();
        return thread;
    }

    @Override
    public final String toString() {
        return format(TO_STRING_FORMAT, instance, started);
    }

    final Thread launch(final Runnable worker) {
        return start(worker, instance, started.incrementAndGet());
    }

    private Thread start(final Runnable runnable, final long major, final long minor) {
        return start(runnable, format(NAME_FORMAT, runnable.getClass().getName(), major, minor));
    }

    private Thread start(final Runnable runnable, final String name) {
        return start(newThread.apply(runnable, name));
    }
}

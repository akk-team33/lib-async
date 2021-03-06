package net.team33.async.consumer;

import java.util.*;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static net.team33.async.consumer.Payload.payload;

/**
 * Generic {@link java.util.function.Consumer Consumer} implementation for the asynchronous processing of
 * messages in separate worker threads.
 *
 * @param <MSG> The type of messages to be consumed.
 * @author AKK - Andreas Kluge-Kaindl, Bremen (de)
 * @since team33-async-8.0.1
 */
@SuppressWarnings("UnusedDeclaration")
public class Scheduler<MSG> implements Consumer<MSG> {

    private static final String TO_STRING_FORMAT = "%s(%s)";

    private final Collection<Throwable> problems = new LinkedList<>();
    private final Launcher launcher = new Launcher(Thread::new);
    private final Runnable worker = new Worker();
    private final Queue<MSG> queue = new ArrayDeque<>(0);
    private final Variable variable = new Variable();

    private final Strategy strategy;
    private final Consumer<? super MSG> target;

    private Scheduler(final Builder<MSG> origin) {
        this.strategy = origin.strategy;
        this.target = origin.target;
    }

    public static <MSG> Builder<MSG> builder(final Strategy strategy, final Consumer<MSG> target) {
        return new Builder<>(strategy, target);
    }

    private static void throwProblems(final Iterator<Throwable> iterator) throws Throwable {
        if (iterator.hasNext()) {
            final Throwable head = iterator.next();
            while (iterator.hasNext()) {
                head.addSuppressed(iterator.next());
            }
            throw head;
        }
    }

    /**
     * Retrieves a list of all problems that may have been occurred during asynchronous processing of incoming messages.
     */
    public final List<Throwable> getProblems() {
        synchronized (problems) {
            return new ArrayList<>(problems);
        }
    }

    /**
     * Throws an accumulated exception, if there are problems.
     *
     * @throws Throwable if there is at least one problem.
     */
    public final void throwProblems() throws Throwable {
        synchronized (problems) {
            throwProblems(problems.iterator());
        }
    }

    @Override
    public final String toString() {
        return format(TO_STRING_FORMAT, getClass().getSimpleName(), strategy);
    }

    /**
     * The number of currently running worker threads (from scheduler´s point of view), greater or equal to {@code 0}.
     * <p>
     * Includes currently rising threads not yet really working (but going to).
     * <p>
     * Excludes those threads in deed still running but definitely finished processing messages
     * and so definitely are going to terminate.
     */
    public final synchronized int getRunning() {
        return variable.started;
    }

    /**
     * The number of messages passed and not yet finally processed, greater or equal to {@code 0}.
     */
    public final synchronized int getLoad() {
        return queue.size() + variable.working;
    }

    /**
     * The number of messages queued for future processing, greater or equal to {@code 0}.
     * <p/>
     * Excludes those messages been queued in deed but already dedicated to
     * just rising new worker threads. So the real queue size may be greater
     * than this value (at most by {@link #getRunning()}).
     */
    public final synchronized int getOverhead() {
        return queue.size() + variable.working - variable.started;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <b>This implementation</b> atomically queues the message for asynchronous processing
     * and starts a new worker thread if appropriate or necessary.
     *
     * @param message The message, not {@code null}.
     *
     * @throws NullPointerException  if {@code message} is {@code null}.
     * @throws IllegalStateException if {@linkplain #stop() stopped} and not
     *                               yet {@linkplain #start() restarted}
     *                               (basically unspecific for Consumers).
     */
    @Override
    public final synchronized void accept(final MSG message) throws NullPointerException, IllegalStateException {
        if (variable.ready) {
            this.queue.add(message);
            if (strategy.test(payload(queue.size() + variable.working, variable.started))) {
                variable.started += 1;
                launcher.launch(worker);
            }
        } else {
            throw new IllegalStateException("not ready");
        }
    }

    /**
     * Blocks the calling thread until all queued messages are processed (so
     * that all worker threads should be terminated) or a timeout occurs.
     *
     * @param millis The timeout time in milliseconds.
     * @return {@code true} if all queued messages are processed.
     * @throws IllegalArgumentException
     * @throws InterruptedException
     */
    public final synchronized boolean join(final long millis) throws IllegalArgumentException, InterruptedException {
        if (0 > millis) {
            throw new IllegalArgumentException("0 > millis (" + millis + ")");

        } else if (0 < millis) {
            final long time0 = currentTimeMillis();
            for (long delta = 0; (delta < millis) && (0 < variable.started); delta = currentTimeMillis() - time0) {
                this.wait(millis - delta);
            }
        }
        return (0 == variable.started);
    }

    /**
     * Called by {@link Worker#run()} to poll a message from the queue.
     *
     * @return The next queued message or {@code null} if the queue is empty.
     */
    private synchronized MSG poll(final boolean first) {
        if (first) {
            // This thread now formally starts working (even if the queue might be empty) ...
            variable.working += 1;
        }

        final MSG result = this.queue.poll();
        if (null == result) {
            // This thread will stop working (even if just formally started) and definitely will be going to terminate,
            // so recognize that right now ...
            variable.started -= 1;
            variable.working -= 1;

            // If 'somebody' is waiting for no worker thread is running ...
            if (1 > variable.started) {
                assert (0 == variable.started);
                assert (0 == variable.working);
                assert (0 == queue.size());
                this.notifyAll();
            }
        }

        return result;
    }

    /**
     * Causes the scheduler to (re)gain normal operation:
     * <ul>
     * <li>Instantly further messages may be {@linkplain #accept(Object) passed}.</li>
     * <li>Processing of {@linkplain #accept(Object) passed} messages will continue as far as
     * {@link #getLoad()} is greater than {@code 0}.</li>
     * </ul>
     * <p/>
     * Does nothing if already started (respectively never {@linkplain #stop() stopped}).
     */
    public final synchronized void start() {
        variable.ready = true;
    }

    /**
     * Causes the scheduler to stop normal operation:
     * <ul>
     * <li>Instantly no more messages may be {@linkplain #accept(Object) passed}. Any attempt will cause an
     * {@link IllegalStateException}.</li>
     * <li>Ongoing and outstanding processing of already {@linkplain #accept(Object) passed} messages will be
     * continued until {@link #getLoad()} is finally {@code 0}.</li>
     * </ul>
     * <p/>
     * Does nothing if already stopped.
     */
    public final synchronized void stop() {
        variable.ready = false;
    }

    /**
     * Causes the scheduler to stop normal operation as soon as possible:
     * <ul>
     * <li>Instantly no more messages may be {@linkplain #accept(Object) passed}. Any attempt will cause an
     * {@link IllegalStateException}.</li>
     * <li>Queued messages not yet in processing will be un-queued (and returned)</li>
     * <li>Ongoing processing is completed normally, however, until {@link #getLoad()} is finally {@code 0}.</li>
     * </ul>
     * <p/>
     * Does nothing and returns an empty list if already stopped asap.
     * <p/>
     * If already stopped (but not asap) supplementary turns the stop into an asap stop.
     */
    public final synchronized List<MSG> stopASAP() {
        stop();
        final List<MSG> result = new ArrayList<>(queue);
        queue.clear();
        return result;
    }

    public final synchronized boolean isStopped() {
        return !variable.ready;
    }

    private static class Variable {
        /**
         * The number of started worker threads not finished or may be not even
         * started working (started from scheduler´s point of view).
         */
        private int started = 0;
        /**
         * The number of running worker threads not finished but definitely
         * started working (started from worker´s point of view).
         */
        private int working = 0;
        /**
         * Indicates if the instance is ready to receive (and process) messages.
         * Initially {@code true}. If not the instance will behave unspecific as
         * a listener throwing an IllegalStateException when a message is passed.
         */
        private boolean ready = true;
    }

    public static class Builder<MSG> {

        private final Strategy strategy;
        private final Consumer<MSG> target;

        private Builder(final Strategy strategy, final Consumer<MSG> target) throws NullPointerException {
            this.strategy = requireNonNull(strategy);
            this.target = requireNonNull(target);
        }

        public Scheduler<MSG> build() {
            return new Scheduler<>(this);
        }
    }

    private class Worker implements Runnable {
        @Override
        public final void run() {
            MSG message = poll(true);
            while (null != message) {
                try {
                    target.accept(message);
                } catch (final Throwable caught) {
                    addProblem(caught);
                }
                message = poll(false);
            }
        }

        private void addProblem(final Throwable caught) {
            synchronized (problems) {
                problems.add(caught);
            }
        }
    }
}

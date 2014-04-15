package net.team33.async;

import net.team33.async.consumer.Launcher;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

/**
 * Generic {@link java.util.function.Consumer Consumer} implementation for the asynchronous processing of
 * messages in separate worker threads.
 *
 * @param <MSG> The type of messages to be consumed.
 * @author AKK - Andreas Kluge-Kaindl, Bremen (de)
 * @since team33-messaging-8.0.1
 */
public class Consumer<MSG> implements java.util.function.Consumer<MSG> {

    private final Launcher launcher = new Launcher();
    private final WORKER worker = new WORKER();
    private final Queue<MSG> queue = new ArrayDeque<>(0);
    private final List<Throwable> problems = new LinkedList<>();
    private final Strategy strategy;
    private final java.util.function.Consumer<? super MSG> target;
    /**
     * The number of started worker threads that did not finish yet or may be not even started working.
     */
    private int started = 0;
    /**
     * The number of running worker threads that did not finish yet but definitely started working.
     */
    private int working = 0;
    /**
     * Indicates if the instance is ready to receive (and process) messages. Initially {@code true}.
     * If not the instance will behave unspecific, throwing an IllegalStateException when a message is passed.
     */
    private transient boolean ready = true;

    public Consumer(final Strategy strategy, final java.util.function.Consumer<? super MSG> target) {
        this.strategy = strategy;
        this.target = target;
    }

    @Override
    public final synchronized void accept(final MSG message) {
        if (ready) {
            queue.add(message);
            if (strategy.isStartCondition(started, queue.size() + working)) {
                started += 1;
                launcher.launch(worker);
            }
        } else {
            throw new IllegalStateException("not ready");
        }
    }

    /**
     * Abstracts a strategy for launching new worker threads dependant on numbers of already started worker threads
     * and passed but not finally processed messages.
     */
    @FunctionalInterface
    public interface Strategy {

        /**
         * Retrieves a strategy that provides a linear relation between queue size and the number of started threads.
         *
         * @param loadFactor The intended relation of [queue size] : [number of started threads]
         * @param threshold  An upper limit for for the number of started threads
         * @return Not {@code null}
         * @throws IllegalArgumentException When {@code loadFactor < 1} or {@code threshold < 1}
         */
        static Strategy linear(final int loadFactor, final int threshold) throws IllegalArgumentException {
            return new Rational(Function.LINEAR, loadFactor, threshold);
        }

        /**
         * Retrieves a strategy that provides a quadratic relation between queue size and the number of started threads.
         *
         * @param loadFactor The intended relation of [queue size] : [number of started threads]²
         * @param threshold  An upper limit for for the number of started threads
         * @return Not {@code null}
         * @throws IllegalArgumentException When {@code loadFactor < 1} or {@code threshold < 1}
         */
        static Strategy quadratic(final int loadFactor, final int threshold) throws IllegalArgumentException {
            return new Rational(Function.QUADRATIC, loadFactor, threshold);
        }

        /**
         * Retrieves a strategy that provides a cubic relation between queue size and the number of started threads.
         *
         * @param loadFactor The intended relation of [queue size] : [number of started threads]³
         * @param threshold  An upper limit for for the number of started threads
         * @return Not {@code null}
         * @throws IllegalArgumentException When {@code loadFactor < 1} or {@code threshold < 1}
         */
        static Strategy cubic(final int loadFactor, final int threshold) throws IllegalArgumentException {
            return new Rational(Function.CUBIC, loadFactor, threshold);
        }

        /**
         * Retrieves a strategy that provides a linear relation between queue size and the number of started threads.
         *
         * @param loadFactor The intended relation of [queue size] : [number of started threads]
         * @return Not {@code null}
         * @throws IllegalArgumentException When {@code loadFactor < 1} or {@code threshold < 1}
         */
        static Strategy linear(final int loadFactor) throws IllegalArgumentException {
            return linear(loadFactor, Integer.MAX_VALUE);
        }

        /**
         * Retrieves a strategy that provides a quadratic relation between queue size and the number of started threads.
         *
         * @param loadFactor The intended relation of [queue size] : [number of started threads]²
         * @return Not {@code null}
         * @throws IllegalArgumentException When {@code loadFactor < 1} or {@code threshold < 1}
         */
        static Strategy quadratic(final int loadFactor) throws IllegalArgumentException {
            return quadratic(loadFactor, Integer.MAX_VALUE);
        }

        /**
         * Retrieves a strategy that provides a cubic relation between queue size and the number of started threads.
         *
         * @param loadFactor The intended relation of [queue size] : [number of started threads]³
         * @return Not {@code null}
         * @throws IllegalArgumentException When {@code loadFactor < 1} or {@code threshold < 1}
         */
        static Strategy cubic(final int loadFactor) throws IllegalArgumentException {
            return cubic(loadFactor, Integer.MAX_VALUE);
        }

        /**
         * Determines if a new worker thread should be started, based on the number
         * of already started worker threads and the number of passed but not
         * finally processed messages.
         * <p/>
         * An implementation should require at least one worker thread for at least
         * one message to be processed.
         *
         * @param running The number of already running worker threads.
         *                At least {@code 0}.
         * @param load    The number of messages passed and not yet finally
         *                processed. At least {@code 0}.
         * @return {@code true} if a new thread should be started.
         * @throws IllegalArgumentException (optional) if an argument is less
         *                                  than its lower limit.
         */
        boolean isStartCondition(int running, int load) throws IllegalArgumentException;
    }

    private enum Function {

        LINEAR {
            @Override
            public long rate(final long running, final long loadFactor) {
                return ((running * loadFactor));
            }
        },
        QUADRATIC {
            @Override
            public long rate(final long running, final long loadFactor) {
                return ((running * running * loadFactor));
            }
        },
        CUBIC {
            @Override
            public long rate(final long running, final long loadFactor) {
                return ((running * running * running * loadFactor));
            }
        };

        abstract long rate(long running, long loadFactor);
    }

    private static class Rational implements Strategy {

        private static final String ILLEGAL_ARGUMENT_MESSAGE_A = "(1 > loadFactor(%d)) || (1 > threshold(%d))";
        private static final String ILLEGAL_ARGUMENT_MESSAGE_B = "(0 > running(%d)) || (0 > load(%d))";
        private static final String TO_STRING_0 = "RATIONAL(f:%s, a:%d";
        private static final String TO_STRING_A;
        private static final String TO_STRING_B;

        static {
            TO_STRING_A = TO_STRING_0 + ")";
            TO_STRING_B = TO_STRING_0 + ", t:%d)";
        }

        private final Function function;
        private final int loadFactor;
        private final int threshold;

        private Rational(final Function function, final int loadFactor, final int threshold)
                throws NullPointerException, IllegalArgumentException {

            this.function = requireNonNull(function);
            if ((1 > loadFactor) || (1 > threshold)) {
                throw new IllegalArgumentException(
                        String.format(ILLEGAL_ARGUMENT_MESSAGE_A, loadFactor, threshold));
            } else {
                this.loadFactor = loadFactor;
                this.threshold = threshold;
            }
        }

        @Override
        public final String toString() {
            if (Integer.MAX_VALUE == threshold) {
                return String.format(TO_STRING_A, function, loadFactor);
            } else {
                return String.format(TO_STRING_B, function, loadFactor, threshold);
            }
        }

        @Override
        public final boolean isStartCondition(final int running, final int load) throws IllegalArgumentException {
            if ((0 > running) || (0 > load)) {
                throw new IllegalArgumentException(String.format(ILLEGAL_ARGUMENT_MESSAGE_B, running, load));
            } else {
                return ((running < threshold) && (function.rate(running, loadFactor) < load));
            }
        }
    }

    /**
     * Called by {@link WORKER#run()} to poll a message from the queue.
     * <p/>
     * Sorgt dafür dass die Anzahl der gestarteten/laufenden Threads
     * heruntergezählt wird und benachrichtigt andere Threads, die ggf.
     * darauf warten, wenn keine weitere Nachricht aus der Warteschlange
     * geholt werden kann. Der WORKER-Thread ist damit praktisch am Ende.
     *
     * @return Die nächste Nachricht aus der Warteschlange oder {@code null}
     *         wenn die Warteschlange leer ist.
     */
    private synchronized MSG poll(final boolean first) {
        if (first) {
            // This thread now (formally) starts working
            // (even if the queue is empty) ...
            working += 1;
        }

        final MSG result = this.queue.poll();
        if(null == result){
            // This thread stoppes working (even if just formally started)
            // and definitely will be going to terminate, so recognize that
            // right now ...
            started -= 1;
            working -= 1;

            // Falls jemand darauf wartet, dass (effektiv) kein
            // WORKER-Thread mehr läuft (z.B. über join()) ...
            if (1 > started) {
                assert (0 == started);
                assert (0 == working);
                assert (0 == queue.size());
                this.notifyAll();
            }
        }

        return result;
    }

    private class WORKER implements Runnable {
        public void run() {
            MSG message = poll(true);
            while (null != message) {
                try {
                    target.accept(message);

                } catch (final Throwable caught) {
                    synchronized (problems) {
                        problems.add(caught);
                    }
                }
                message = poll(false);
            }
        }
    }
}

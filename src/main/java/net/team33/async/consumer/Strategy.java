package net.team33.async.consumer;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Abstracts a strategy for launching new worker threads dependant on numbers
 * of already started worker threads and passed but not finally processed
 * messages.
 */
public class Strategy implements Predicate<Payload> {

    private static final Function LINEAR = (running, loadFactor) -> loadFactor * running;
    private static final Function QUADRATIC = (running, loadFactor) -> loadFactor * running * running;
    private static final Function CUBIC = (running, loadFactor) -> loadFactor * running * running * running;
    private final Function function;
    private final int loadFactor;
    private final int threshold;

    private Strategy(final Function function, final int loadFactor, final int threshold)
            throws NullPointerException, IllegalArgumentException {

        this.function = requireNonNull(function, "function == null");
        if ((1 > loadFactor) || (1 > threshold)) {
            throw new IllegalArgumentException(
                    String.format("(1 > loadFactor(%d)) || (1 > threshold(%d))", loadFactor, threshold));
        } else {
            this.loadFactor = loadFactor;
            this.threshold = threshold;
        }
    }

    /**
     * Retrieves a strategy that provides a linear relation between already started worker threads and passed but not
     * finally processed messages.
     *
     * @param loadFactor The relation factor (messages / threads).
     * @param threshold  A max. number of threads.
     * @return Not {@code null}.
     * @throws IllegalArgumentException when (loadFactor &lt; 1) or (threshold &lt; 1)
     */
    public static Strategy linear(final int loadFactor, final int threshold) throws IllegalArgumentException {
        return new Strategy(LINEAR, loadFactor, threshold);
    }

    /**
     * Retrieves a strategy that provides a quadratic relation between already started worker threads and passed but not
     * finally processed messages.
     *
     * @param loadFactor The relation factor (messages / threads²).
     * @param threshold  A max. number of threads.
     * @return Not {@code null}.
     * @throws IllegalArgumentException when (loadFactor &lt; 1) or (threshold &lt; 1)
     */
    public static Strategy quadratic(final int loadFactor, final int threshold) throws IllegalArgumentException {
        return new Strategy(QUADRATIC, loadFactor, threshold);
    }

    /**
     * Retrieves a strategy that provides a cubic relation between already started worker threads and passed but not
     * finally processed messages.
     *
     * @param loadFactor The relation factor (messages / threads³).
     * @param threshold  A max. number of threads.
     * @return Not {@code null}.
     * @throws IllegalArgumentException when (loadFactor &lt; 1) or (threshold &lt; 1)
     */
    public static Strategy cubic(final int loadFactor, final int threshold) throws IllegalArgumentException {
        return new Strategy(CUBIC, loadFactor, threshold);
    }

    /**
     * Retrieves a strategy that provides a linear relation between already started worker threads and passed but not
     * finally processed messages.
     *
     * @param loadFactor The relation factor (messages / threads).
     * @return Not {@code null}.
     * @throws IllegalArgumentException when (loadFactor &lt; 1)
     */
    public static Strategy linear(final int loadFactor) throws IllegalArgumentException {
        return linear(loadFactor, Integer.MAX_VALUE);
    }

    /**
     * Retrieves a strategy that provides a quadratic relation between already started worker threads and passed but not
     * finally processed messages.
     *
     * @param loadFactor The relation factor (messages / threads²).
     * @return Not {@code null}.
     * @throws IllegalArgumentException when (loadFactor &lt; 1)
     */
    public static Strategy quadratic(final int loadFactor) throws IllegalArgumentException {
        return quadratic(loadFactor, Integer.MAX_VALUE);
    }

    /**
     * Retrieves a strategy that provides a cubic relation between already started worker threads and passed but not
     * finally processed messages.
     *
     * @param loadFactor The relation factor (messages / threads³).
     * @return Not {@code null}.
     * @throws IllegalArgumentException when (loadFactor &lt; 1)
     */
    public static Strategy cubic(final int loadFactor) throws IllegalArgumentException {
        return cubic(loadFactor, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException when the argument is {@code null}.
     */
    @Override
    public final boolean test(final Payload payload) throws NullPointerException {
        return test(payload.getRunning(), payload.getCharge());
    }

    private boolean test(final int running, final int charge) {
        return ((running < threshold) && (function.load(loadFactor, running) < charge));
    }

    @FunctionalInterface
    private interface Function {
        long load(long loadFactor, long running);
    }
}


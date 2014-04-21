package net.team33.async.consumer;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Abstracts a strategy for launching new worker threads dependant on numbers
 * of already started worker threads and passed but not finally processed
 * messages.
 */
public class Strategy implements Predicate<Payload> {

    private static final String TO_STRING_FORMAT = "Strategy(function(%s), loadFactor(%d), threshold(%d))";
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
        return new Strategy(Rational.LINEAR, loadFactor, threshold);
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
        return new Strategy(Rational.QUADRATIC, loadFactor, threshold);
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
        return new Strategy(Rational.CUBIC, loadFactor, threshold);
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

    private static int hashCode(final int... singles) {
        return hashCode(singles.length - 1, singles);
    }

    private static int hashCode(final int index, final int[] singles) {
        return (0 > index) ? 0 : (singles[index] + (31 * hashCode(index - 1, singles)));
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

    @Override
    public final boolean equals(final Object other) {
        return (this == other) || ((other instanceof Strategy) && this.equals((Strategy) other));
    }

    private boolean equals(final Strategy other) {
        return (loadFactor == other.loadFactor) && (threshold == other.threshold) && function.equals(other.function);
    }

    @Override
    public final int hashCode() {
        return hashCode(function.hashCode(), loadFactor, threshold);
    }

    @Override
    public final String toString() {
        return String.format(TO_STRING_FORMAT, function, loadFactor, threshold);
    }

    private enum Rational implements Function {
        LINEAR((running, loadFactor) -> loadFactor * running),
        QUADRATIC((running, loadFactor) -> loadFactor * running * running),
        CUBIC((running, loadFactor) -> loadFactor * running * running * running);

        private final Function core;

        Rational(final Function core) {
            this.core = core;
        }

        @Override
        public final long load(final long loadFactor, final long running) {
            return core.load(loadFactor, running);
        }
    }

    @FunctionalInterface
    private interface Function {
        long load(long loadFactor, long running);
    }
}


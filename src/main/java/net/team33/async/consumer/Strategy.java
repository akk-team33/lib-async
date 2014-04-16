package net.team33.async.consumer;

import static java.util.Objects.requireNonNull;

/**
 * Abstracts a strategy for launching new worker threads dependant on numbers
 * of already started worker threads and passed but not finally processed
 * messages.
 */
public abstract class Strategy {

    /**
     * TODO
     *
     * @param loadFactor TODO
     * @param threshold  TODO
     * @return           TODO
     * @throws IllegalArgumentException TODO
     */
    public static Strategy linear(final int loadFactor, final int threshold) throws IllegalArgumentException {
        return new RATIONAL(FUNCTION.LINEAR, loadFactor, threshold);
    }

    public static Strategy quadratic(final int loadFactor, final int threshold) throws IllegalArgumentException {
        return new RATIONAL(FUNCTION.QUADRATIC, loadFactor, threshold);
    }

    public static Strategy cubic(final int loadFactor, final int threshold) throws IllegalArgumentException {
        return new RATIONAL(FUNCTION.CUBIC, loadFactor, threshold);
    }

    public static Strategy linear(final int loadFactor) throws IllegalArgumentException {
        return linear(loadFactor, Integer.MAX_VALUE);
    }

    public static Strategy quadratic(final int loadFactor) throws IllegalArgumentException {
        return quadratic(loadFactor, Integer.MAX_VALUE);
    }

    public static Strategy cubic(final int loadFactor) throws IllegalArgumentException {
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
    public abstract boolean isStartCondition(int running, int load) throws IllegalArgumentException;

    private enum FUNCTION {
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

    private static class RATIONAL extends Strategy {

        private final FUNCTION function;
        private final int loadFactor;
        private final int threshold;

        private RATIONAL(final FUNCTION function, final int loadFactor, final int threshold)
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

        @Override
        public final String toString() {
            if(Integer.MAX_VALUE == threshold) {
                return String.format("RATIONAL(f:%s, a:%d)", function, loadFactor);
            } else {
                return String.format("RATIONAL(f:%s, a:%d, t:%d)", function, loadFactor, threshold);
            }
        }

        @Override
        public final boolean isStartCondition(final int running, final int load) throws IllegalArgumentException {
            if ((0 > running) || (0 > load)) {
                throw new IllegalArgumentException(String.format("0 > running(%d) || 0 > load(%d)", running, load));
            } else {
                return ((running < threshold) && (function.rate(running, loadFactor) < load));
            }
        }
    }
}


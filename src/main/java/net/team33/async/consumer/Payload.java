package net.team33.async.consumer;

/**
 * Represents the payload of a scheduler by two components:
 * <ul>
 * <li>{@link #getCharge() charge}</li>
 * <li>{@link #getRunning() running}</li>
 * </ul>
 */
class Payload {

    private static final String TO_STRING_FORMAT = "Payload(charge(%d), running(%d))";
    private static final String PROBLEM_FORMAT = "(0 > charge(%d)) || (0 > running(%d))";

    private final int charge;
    private final int running;

    public Payload(final int charge, final int running) {
        if ((0 > charge) || (0 > running)) {
            throw new IllegalArgumentException(String.format(PROBLEM_FORMAT, charge, running));
        } else {
            this.charge = charge;
            this.running = running;
        }
    }

    public static Payload payload(final int charge, final int running) {
        return new Payload(charge, running);
    }

    /**
     * The number of tasks that have already been passed to a scheduler for processing, but not yet fully processed.
     */
    public final int getCharge() {
        return charge;
    }

    /**
     * The number of tasks that are currently in processing.
     */
    public final int getRunning() {
        return running;
    }

    @Override
    public final String toString() {
        return String.format(TO_STRING_FORMAT, charge, running);
    }

    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof Payload) {
            return equals((Payload) other);
        } else {
            return false;
        }
    }

    private boolean equals(final Payload other) {
        return (charge == other.charge) && (running == other.running);
    }

    @Override
    public final int hashCode() {
        return (31 * charge) + running;
    }
}

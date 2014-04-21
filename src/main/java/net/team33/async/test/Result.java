package net.team33.async.test;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class Result {

    public static final Result NO_PROBLEMS = new Result(Collections.emptyList());

    private final List<Object> problems;

    private Result(final List<Object> problems) {
        this.problems = requireNonNull(problems);
    }

    public Result() {
        this(new LinkedList<>());
    }

    private static String toString(final Iterator<Object> iterator) {
        final StringBuilder result = new StringBuilder(0);
        result.append("[");
        if (iterator.hasNext()) {
            appendNext(iterator, result);
            while (iterator.hasNext()) {
                result.append(",");
                appendNext(iterator, result);
            }
            result.append("\n");
        }
        result.append("]");
        return result.toString();
    }

    private static void appendNext(final Iterator<Object> iterator, final StringBuilder result) {
        result.append("\n-   ");
        result.append(iterator.next());
    }

    @Override
    public final boolean equals(final Object other) {
        return (this == other) || ((other instanceof Result) && equals((Result) other));
    }

    private boolean equals(final Result other) {
        return problems.equals(other.problems);
    }

    @Override
    public final int hashCode() {
        return problems.hashCode();
    }

    @Override
    public final String toString() {
        return toString(problems.iterator());
    }

    public Result assertEquals(final Supplier<String> message, final Object expected, final Object result) {
        if (!Objects.equals(expected, result)) {
            problems.add(String.format("%s: expected <%s> but was <%s>", message.get(), expected, result));
        }
        return this;
    }
}

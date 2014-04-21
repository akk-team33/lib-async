package net.team33.async.consumer;

import net.team33.async.test.Result;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class SchedulerTest {

    private static List<?> newMessages(final int size) {
        final List<Integer> result = new ArrayList<>(size);
        for (int index = 0; index < size; ++index) {
            result.add(index);
        }
        return result;
    }

    private static Tester tester() {
        return new Tester();
    }

    @Test
    public final void testAccept() throws InterruptedException {
        assertEquals(
                tester().result(),
                tester().testAccept(Strategy.linear(1), 1000, HashSet::new)
                        .testAccept(Strategy.linear(1), 1000, ArrayList::new)
                        .testAccept(Strategy.linear(1000), 1, ArrayList::new)
                        .result()
        );
    }

    private static class Aggregator implements Consumer<Object> {
        private final List<Object> accepted = new ArrayList<>(0);

        @Override
        public void accept(final Object message) {
            synchronized (accepted) {
                accepted.add(message);
            }
        }
    }

    private static class Tester {
        private final Result result = new Result();

        <N> Tester testAccept(final Strategy strategy, final int size, final Function<Collection<?>, N> normal)
                throws InterruptedException {

            final Aggregator aggregator = new Aggregator();
            final Scheduler<Object> subject = Scheduler.builder(strategy, aggregator).build();
            final List<?> input = newMessages(size);

            input.forEach(subject);
            subject.join(Long.MAX_VALUE);

            result.assertEquals(
                    () -> String.format("[strategy(%s), size(%d)]", strategy, size),
                    normal.apply(input),
                    normal.apply(aggregator.accepted)
            );

            return this;
        }

        public Result result() {
            return result;
        }
    }
}

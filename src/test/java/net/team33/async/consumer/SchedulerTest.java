package net.team33.async.consumer;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class SchedulerTest {

    private static void testAccept(final Strategy strategy, final int size) throws InterruptedException {
        final Aggregator aggregator = new Aggregator();
        final Scheduler<Object> subject = Scheduler.builder(strategy, aggregator).build();
        final List<?> input = newMessages(size);

        input.forEach(subject);
        subject.join(Long.MAX_VALUE);

        Assert.assertEquals(
                new HashSet<>(input),
                new HashSet<>(aggregator.accepted)
        );
    }

    private static List<?> newMessages(final int size) {
        final List<Integer> result = new ArrayList<>(size);
        for (int index = 0; index < size; ++index) {
            result.add(index);
        }
        return result;
    }

    @Test
    public final void testAccept() throws InterruptedException {
        testAccept(Strategy.linear(1), 1000);
        testAccept(Strategy.linear(1000), 1);
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
}

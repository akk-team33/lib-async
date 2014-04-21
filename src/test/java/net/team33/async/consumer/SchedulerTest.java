package net.team33.async.consumer;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class SchedulerTest {

    @Test
    public final void test() throws InterruptedException {
        final Aggregator aggregator = new Aggregator();
        final Scheduler<Object> subject = new Scheduler<>(Strategy.linear(1), new Scheduler<>(Strategy.linear(1), new Scheduler<>(Strategy.linear(1), aggregator)));
        final List<?> input = newMessages(1000);

        input.forEach(subject);
        subject.join(Long.MAX_VALUE);

        Assert.assertEquals(
                new HashSet<>(input),
                new HashSet<>(aggregator.accepted)
        );
    }

    private List<?> newMessages(final int size) {
        final List<Integer> result = new ArrayList<>(size);
        for (int index = 0; index < size; ++index) {
            result.add(index);
        }
        return result;
    }

    private class Aggregator implements Consumer<Object> {
        private final List<Object> accepted = new ArrayList<>(0);

        @Override
        public void accept(final Object message) {
            synchronized (accepted) {
                accepted.add(message);
            }
        }
    }
}

package net.team33.async.consumer;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"TypeMayBeWeakened", "AccessingNonPublicFieldOfAnotherObject"})
public class SchedulerTest {

    private static final Object MESSAGE_00 = null;
    private static final Object MESSAGE_01 = "Message 01";
    private static final Object MESSAGE_02 = 278;
    private static final Object MESSAGE_03 = 3.141592654;
    private static final Object MESSAGE_04 = true;
    private static final Object MESSAGE_05 = 'c';
    private static final Object MESSAGE_06 = new Date();

    private static final Object[] MESSAGES;
    private static final List<Throwable> NO_PROBLEMS = emptyList();

    static {
        MESSAGES = new Object[]{MESSAGE_00, MESSAGE_01, MESSAGE_02, MESSAGE_03, MESSAGE_04, MESSAGE_05, MESSAGE_06};
    }

    @Test
    public void testGetProblems() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testThrowProblems() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testToString() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testGetRunning() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testGetLoad() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testGetOverhead() throws Exception {
        Assert.fail("Not yet implemented");
    }

    /**
     * Wenn eine Reihe von Nachrichten an einen Scheduler übergeben werden, müssen diese tatsächlich beim endgültigen
     * Consumer ankommen.
     *
     * Insbesondere müssen sie in der ursprünglichen Reihenfolge ankommen, wenn der Scheduler höchstens einen
     * Worker-Thread startet.
     */
    @Test
    public void testAccept() throws Exception {
        testAccept_(Strategy.linear(1), HashSet::new);
        testAccept_(Strategy.linear(1, 1), ArrayList::new);
        testAccept_(Strategy.linear(1), ArrayList::new);
    }

    private static <R> void testAccept_(final Strategy strategy, final Function<Collection<?>, R> function)
            throws InterruptedException {

        final Collector target = new Collector();
        final Scheduler<Object> subject = new Scheduler<>(strategy, target);
        Stream.of(MESSAGES).forEach(subject);
        assertTrue(
                subject.join(1000));
        subject.getRunning();
        assertEquals(
                NO_PROBLEMS,
                subject.getProblems());
        assertEquals(
                function.apply(Arrays.asList(MESSAGES)),
                function.apply(target.accepted));
    }

    @Test
    public void testJoin() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testStart() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testStop() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testStopASAP() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testIsStopped() throws Exception {
        Assert.fail("Not yet implemented");
    }

    private static class Collector implements Consumer<Object> {
        private final List<Object> accepted = new LinkedList<>();

        @Override
        public final void accept(final Object o) {
            accepted.add(o);
            Thread.yield();
        }
    }
}

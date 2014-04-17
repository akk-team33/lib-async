package net.team33.async.consumer;

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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        MESSAGES = new Object[]{MESSAGE_01, MESSAGE_02, MESSAGE_03, MESSAGE_04, MESSAGE_05, MESSAGE_06};
    }

    private static final int TIMEOUT_1000 = 1000;
    private static final String PROBLEM_TIMEOUT = "subject.join(%d) -> timeout";
    private static final String PROBLEM_EXPECTED = "expected: %s but was: %s";

    @Test
    public void testGetProblems() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testThrowProblems() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testToString() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testGetRunning() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testGetLoad() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOverhead() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public final void testAccept1() throws InterruptedException, IllegalArgumentException {
        assertEquals(
                emptyList(),
                testAccept(Strategy.quadratic(1), origin -> new HashSet(origin))
        );
    }

    @Test
    public final void testAccept2() throws InterruptedException, IllegalArgumentException {
        assertEquals(
                emptyList(),
                testAccept(Strategy.linear(1, 1), origin -> new ArrayList(origin))
        );
    }

    @Test
    public final void testAccept3() throws InterruptedException, IllegalArgumentException {
        assertNotEquals(
                emptyList(),
                testAccept(Strategy.linear(1), origin -> new ArrayList(origin))
        );
    }

    private static <N> List<Object> testAccept(final Strategy strategy,
                                               final Function<Collection<?>, N> normal) throws InterruptedException {

        return testAccept(new LinkedList<>(), strategy, normal);
    }

    private static <N> List<Object> testAccept(final List<Object> problems, final Strategy strategy,
                                               final Function<Collection<?>, N> normal)
            throws InterruptedException {

        final Collector target = new Collector();
        final Scheduler<Object> subject = new Scheduler<>(strategy, new Scheduler<>(strategy, target));
        final List<Object> messages = newMessages(1000);

        messages.stream().forEach(subject);

        if (subject.join(TIMEOUT_1000)) {
            final N accepted = normal.apply(target.accepted);
            final N expected = normal.apply(messages);
            if (!accepted.equals(expected)) {
                problems.add(String.format(PROBLEM_EXPECTED, expected, accepted));
            }

        } else {
            problems.add(String.format(PROBLEM_TIMEOUT, TIMEOUT_1000));
        }

        return problems;
    }

    private static List<Object> newMessages(final int size) {
        final List<Object> result = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            result.add(i);
        }
        return result;
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
        fail("Not yet implemented");
    }

    @Test
    public void testStart() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testStop() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testStopASAP() throws Exception {
        fail("Not yet implemented");
    }

    @Test
    public void testIsStopped() throws Exception {
        fail("Not yet implemented");
    }

    private static class Collector implements Consumer<Object> {
        private final List<Object> accepted = new LinkedList<>();

        @Override
        public final void accept(final Object o) {
            accepted.add(o);
        }
    }
}

package io.seata.common.thread;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PositiveAtomicCounterTest {

    @Test
    public void testConstructor() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter).isInstanceOf(PositiveAtomicCounter.class);
    }

    @Test
    public void testIncrementAndGet() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter.incrementAndGet()).isEqualTo(1);
    }

    @Test
    public void testGetAndIncrement() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter.getAndIncrement()).isEqualTo(0);
    }

    @Test
    public void testGet() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter.get()).isEqualTo(0);
    }
}

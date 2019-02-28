package com.alibaba.fescar.common;


import com.alibaba.fescar.common.thread.NamedThreadFactory;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
public class NamedThreadFactoryTest {

    @Test
    public void testNewThread() {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("testNameThread", 5);

        Thread testNameThread = namedThreadFactory
            .newThread(() -> System.out.println(Thread.currentThread().getName()));
        System.out.println(testNameThread.toString());
        Assert.assertTrue(testNameThread.getName().startsWith("testNameThread"));
        Assert.assertTrue(testNameThread.isDaemon());
    }
}
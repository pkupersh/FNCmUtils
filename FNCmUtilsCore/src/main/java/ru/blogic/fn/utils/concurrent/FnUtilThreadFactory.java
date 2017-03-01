package ru.blogic.fn.utils.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by pkupershteyn on 22.02.2017.
 * A simple thread factory that creates normal threads with numeric names
 */
public class FnUtilThreadFactory implements ThreadFactory {
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final ThreadGroup group;

    public FnUtilThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = ( s != null ) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(group, runnable, "thread " + threadNumber.getAndIncrement(), 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}

package ru.blogic.fn.utils.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by pkupershteyn on 27.02.2017.
 * Interface to use as a parent with {@link FnUtilRunnable}
 */
public interface FnUtilRunnableParent {

    /**
     * Prints a message to std output
     * @param message A message to print
     */
    void printInfo(String message);

    /**
     * Print a message to stderr
     * @param message An error message to print
     */
    void printError(String message);

    /**
     * A flag that signals that this RunnableParent is canceled
     * @return
     */
    AtomicBoolean isCanceled();

}

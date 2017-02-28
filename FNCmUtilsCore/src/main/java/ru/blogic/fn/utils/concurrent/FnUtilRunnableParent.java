package ru.blogic.fn.utils.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by pkupershteyn on 27.02.2017.
 */
public interface FnUtilRunnableParent {

    void printInfo(String message);

    void printError(String message);

    AtomicBoolean isCanceled();

}

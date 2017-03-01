package ru.blogic.fn.utils.concurrent;

/**
 * Created by pkupershteyn on 27.02.2017.
 * Thrown when execution is interrupted by user
 * Used inside {@link FnUtilRunnable} to interrupt execution
 */
public class ExecutionInterruptedException extends Exception{
    public ExecutionInterruptedException() {
    }
}

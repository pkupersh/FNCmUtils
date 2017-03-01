package ru.blogic.fn.utils.concurrent;

/**
 * Created by pkupershteyn on 27.02.2017.
 * {@link Runnable} interface to use inside {@link ru.blogic.fn.utils.FnExecutor}
 * Has ready to use methods to check parent executor cancel flag
 * Implementation lasss has to provide {@link #fnRun()} method as Runnable body,
 * and use {@link #checkCanceled()} inside it to verify cancel status
 */
public abstract class FnUtilRunnable implements Runnable {
    private final FnUtilRunnableParent runnableParent;
    private String threadName;
    private final boolean printThreadName;

    /**
     *
     * @param runnableParent Parent for tus runnnable (usually {@link ru.blogic.fn.utils.FnExecutor})
     * @param printThreadName For cases when several threads are used, it is important to print thread name for every console message. This parameter defines will be thread name printed or not
     */
    public FnUtilRunnable(FnUtilRunnableParent runnableParent, boolean printThreadName) {
        this.runnableParent = runnableParent;
        this.printThreadName = printThreadName;
    }

    /**
     * Method mush be used inside runnnable body (@see {@link #fnRun()}) to preiodically check if execution is canceled
     * @throws ExecutionInterruptedException Exception that is used internally to interrupt work if execution is canceled
     */
    protected void checkCanceled() throws ExecutionInterruptedException {
        System.out.println("checkCAnceled: "+runnableParent.isCanceled().get());
        if (runnableParent.isCanceled().get()) {
            printInfo("Canceling work...");
            throw new ExecutionInterruptedException();
        }
    }

    @Override
    public final void run() {
        threadName = printThreadName ? Thread.currentThread().getName() + ": " : "";
        try {
            fnRun();
        } catch (ExecutionInterruptedException e) {

        }
    }

    /**
     * Prints a message to std output
     * @param info
     */
    protected void printInfo(String info) {
        runnableParent.printInfo(threadName + info);
    }

    /**
     * Prints an error
     * @param error
     */
    protected void printError(String error) {
        runnableParent.printError(threadName + error);
    }

    protected String getThreadName() {
        return threadName;
    }

    /**
     * The main body to run
     * @throws ExecutionInterruptedException
     */
    public abstract void fnRun() throws ExecutionInterruptedException;
}

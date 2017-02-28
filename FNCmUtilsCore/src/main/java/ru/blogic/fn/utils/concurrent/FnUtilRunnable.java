package ru.blogic.fn.utils.concurrent;

/**
 * Created by pkupershteyn on 27.02.2017.
 */
public abstract class FnUtilRunnable implements Runnable {
    private final FnUtilRunnableParent runnableParent;
    private String threadName;
    private final boolean printThreadName;

    public FnUtilRunnable(FnUtilRunnableParent runnableParent, boolean printThreadName) {
        this.runnableParent = runnableParent;
        this.printThreadName = printThreadName;
    }

    protected void checkCanceled() throws ExecutionInterruptedException {
        System.out.println("checkCAnceled: "+runnableParent.isCanceled().get());
        if (runnableParent.isCanceled().get()) {
            printInfo("Canceling work...");
            throw new ExecutionInterruptedException();
        }
    }

    @Override
    public void run() {
        threadName = printThreadName ? Thread.currentThread().getName() + ": " : "";
        try {
            fnRun();
        } catch (ExecutionInterruptedException e) {

        }
    }

    protected void printInfo(String info) {
        runnableParent.printInfo(threadName + info);
    }

    protected void printError(String error) {
        runnableParent.printError(threadName + error);
    }

    protected String getThreadName() {
        return threadName;
    }

    public abstract void fnRun() throws ExecutionInterruptedException;
}

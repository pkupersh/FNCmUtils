package ru.blogic.fn.utils.ui;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import ru.blogic.fn.runner.Runner;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by pkupershteyn on 11.01.2017.
 */
public class RunnerService extends Service<Void> {
    interface Events {
        void beforeRunnerStart();

        void afterRunnerStop();
    }

    private static Events EVTS = new Events() {
        public void beforeRunnerStart() {

        }

        public void afterRunnerStop() {

        }
    };

    private final List<String> params;
    private final PrintWriter out;
    private final PrintWriter err;
    private static List<RunnerService> SERVICES = Collections.synchronizedList(new ArrayList<RunnerService>());
    private Runner runner;

    private EventHandler<WorkerStateEvent> serviceStopHandler = new EventHandler<WorkerStateEvent>() {
        public void handle(WorkerStateEvent event) {
            SERVICES.remove(event.getSource());
            EVTS.afterRunnerStop();
        }
    };

    public static void setEvents(Events events) {
        EVTS = events;
    }

    public static void start(List<String> params, PrintWriter out, PrintWriter err) {
        if (SERVICES.isEmpty()) {
            RunnerService runnerService = new RunnerService(params, out, err);
            SERVICES.add(runnerService);
            runnerService.start();
        } else {
            System.out.println("Service already running");
        }
    }

    public static void cancelAll() {
        for (RunnerService runnerService : new ArrayList<RunnerService>(SERVICES)) {
            if (runnerService.getState().equals(State.RUNNING)) {
                System.out.println(runnerService.toString() + "cancel result: " + runnerService.cancel());
            }
        }
    }

    @Override
    public boolean cancel() {
        if (runner != null) {
            runner.interrupt();
        }
        return super.cancel();
    }

    private RunnerService(List<String> params, PrintWriter out, PrintWriter err) {
        this.params = params;
        this.out = out;
        this.err = err;

        this.setOnCancelled(serviceStopHandler);
        this.setOnFailed(serviceStopHandler);
        this.setOnSucceeded(serviceStopHandler);
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                System.out.println("RUN!!");
                runner = new Runner();

                runner.setOut(out);
                runner.setErr(err);

                EVTS.beforeRunnerStart();

                runner.run(params.toArray(new String[params.size()]));
                return null;
            }
        };
    }

}

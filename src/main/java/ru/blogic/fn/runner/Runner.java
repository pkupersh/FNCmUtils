package ru.blogic.fn.runner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import ru.blogic.fn.util.FnBulkUpdater;
import ru.blogic.fn.util.FnCounter;
import ru.blogic.fn.util.FnDeleter;

/**
 * Created by sam158 on 28.06.2016.
 * This is entry point for all utilities. Name of utility must be in "--utility || -ut" parameter
 */
public class Runner {

    @Parameter(names = { "--utility", "-ut" }, description = "Name of utility for running. Permitted values: deleter, counter, bulkUpdater. Value by Default is counter")
    private String executeUtitityName;

    public static void main(String... args){
        Runner runner = new Runner();
        new JCommander(runner, args);

        if(runner.executeUtitityName ==null){
            runner.executeUtitityName="counter";
        }

        switch(runner.executeUtitityName){
            case "counter":
                System.out.println("Run counter utility...");
                FnCounter.main(args);
                break;
            case "deleter":
                System.out.println("Run deleter utility...");
                FnDeleter.main(args);
                break;
            case "bulkUpdater":
                System.out.println("Run bulk updater utility...");
                FnBulkUpdater.main(args);
                break;
            default:
                System.out.println("Permitted values for parametr -ut deleter, counter(or empty), bulkUpdater ");
        }
    }
}

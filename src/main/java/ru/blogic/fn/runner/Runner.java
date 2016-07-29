package ru.blogic.fn.runner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.reflections.Reflections;
import ru.blogic.fn.utils.FnExecutor;
import ru.blogic.fn.utils.annotations.Utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sam158 on 28.06.2016.
 * This is entry point for all utilities. Name of utility must be in "--utility || -ut" parameter
 */
public class Runner {

    @Parameter(names = { "--utility", "-ut" }, description = "Name of utility for running. Permitted values: deleter, counter, bulkUpdater. Value by Default is counter")
    private String executeUtitityName ="counter";


    public static void main(String... args) {

        Runner runner = new Runner();
        JCommander jCommander = new JCommander(runner);
        try{
            jCommander.parse( args);
            jCommander.setProgramName("FNCmUtils");
        }catch (ParameterException ex){
            jCommander.usage();
            return;
        }

        // Collect all classes from utils pckages
        Map<String, Class<FnExecutor>> utilCollect = runner.getUtilitiesCollection();

        //create and run required instance of class
        if(utilCollect.containsKey(runner.executeUtitityName)){
            try {
                Class<? extends FnExecutor> util = utilCollect.get(runner.executeUtitityName);
                FnExecutor instanceOfUtil = util.newInstance();
                Method methodRun = util.getMethod("execute", new Class[]{String[].class} );
                methodRun.invoke(instanceOfUtil,new Object[]{args});

            } catch (InstantiationException e) {
                jCommander.usage();
            } catch (IllegalAccessException e) {
                jCommander.usage();
            } catch (NoSuchMethodException e) {
                jCommander.usage();
            } catch (InvocationTargetException e) {
                jCommander.usage();
            }

        }else{
            System.out.println("Incorrect -ut parameter value. There is no "+runner.executeUtitityName +" utilities ");
            jCommander.usage();
        }
    }

    <E extends FnExecutor> Map<String,Class<E>> getUtilitiesCollection(){
        //get all utilities with"@Utilities" annotation
        Map<String,Class<E>> result = new HashMap<String,Class<E>> ();

        Reflections reflections = new Reflections("ru.blogic.fn.utils"); //scan only utils directory


        for (Class<? extends FnExecutor> util : reflections.getSubTypesOf(FnExecutor.class) ) {
            //Not use abstract classes
            if(Modifier.isAbstract(util.getModifiers())){
                continue;
            }
            //check if class have annotation
            //if no or value is empty- key is class name
            //else - value value param
            Utility annotation = util.getAnnotation(Utility.class);
            if ((annotation == null || "".equals(annotation.value().trim())) ){
                result.put(util.getSimpleName(), (Class<E>) util);
            }else{
                result.put(annotation.value(),(Class<E>) util);
            }
        }

        return result;
    }

}

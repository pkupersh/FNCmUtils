package ru.blogic.fn.utils;

import org.reflections.Reflections;
import ru.blogic.fn.utils.annotations.Utility;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pkupershteyn on 02.03.2017.
 */
public class FnExecutorManager {

    private static final Map<String, Class<? extends FnExecutor>> EXECUTORS = new HashMap<String, Class<? extends FnExecutor>>();

    static {
        //get all utilities with"@Utilities" annotation

        Reflections reflections = new Reflections("ru.blogic.fn.utils"); //scan only utils directory


        for (Class<? extends FnExecutor> util : reflections.getSubTypesOf(FnExecutor.class)) {
            //Not use abstract classes
            if (Modifier.isAbstract(util.getModifiers())) {
                continue;
            }
            //check if class have annotation
            //if no or value is empty- key is class name
            //else - value value param
            Utility annotation = util.getAnnotation(Utility.class);
            if (( annotation == null || "".equals(annotation.value().trim()) )) {
                EXECUTORS.put(util.getSimpleName(), util);
            } else {
                EXECUTORS.put(annotation.value(), util);
            }
        }
    }

    public static Map<String, Class<? extends FnExecutor>> getAvailableExecutors() {
        return Collections.unmodifiableMap(EXECUTORS);
    }

    public static Set<String> getAvailableExecutorNames(){
        return Collections.unmodifiableSet(EXECUTORS.keySet());
    }

    public static FnExecutor getExecutorInstance(String executorName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class<? extends FnExecutor> executorClass = EXECUTORS.get(executorName);
        if(executorClass !=null){
            return executorClass.newInstance();
        }
        throw new ClassNotFoundException("Could not find FnExcutor class for name '"+executorName+"'");
    }

    public static FnExecutor findFnExecutorByClassName(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class clazz = Class.forName(className);
            if (!FnExecutor.class.isAssignableFrom(clazz)) {
                throw new ClassCastException("Error: Utility '" + className + "' found as a class but DOES NOT extend " + FnExecutor.class.getName());
            }
        EXECUTORS.put(className,clazz);
        return getExecutorInstance(className);
    }

}

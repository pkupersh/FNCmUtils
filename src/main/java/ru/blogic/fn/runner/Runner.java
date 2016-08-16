package ru.blogic.fn.runner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.reflections.Reflections;
import ru.blogic.fn.utils.FnExecutor;
import ru.blogic.fn.utils.annotations.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sam158 on 28.06.2016.
 * This is entry point for all utilities. Name of utility must be in "--utility || -ut" parameter
 */
public class Runner {

    private static final String DEFAULT_UTILITY_NAME = "counter";

    private static Option OPTION_UTILITY = new Option("ut", "utility", true, "Name of utility for running.");
    private static Option OPTION_OPTIONSFILE = new Option("o", "options", true, "File with options. May be repeated. Explicit options take precedence over options from file. ");
    private static Options OPTIONS = new Options();
    private static Map<String, Class<FnExecutor>> UTILS;
    private static CommandLineParser PARSER = new DefaultParser();

    static {
        OPTIONS.addOption(OPTION_UTILITY);
        OPTIONS.addOption(OPTION_OPTIONSFILE);
        UTILS = getUtilitiesCollection();
        OPTION_UTILITY.setDescription(OPTION_UTILITY.getDescription() + " Permitted values: " + UTILS.keySet() + ". Value by Default is " + DEFAULT_UTILITY_NAME);
        try {
            for (Option option : getAllOptions()) {
                OPTIONS.addOption(option);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<Option> getAllOptions() throws InstantiationException, IllegalAccessException {
        Set<Option> options = new HashSet<Option>();
        for (Class<FnExecutor> fnExecutorClass : UTILS.values()) {
            FnExecutor theUtil = fnExecutorClass.newInstance();
            List<FnExecutor.CmParameter> cmParameters = theUtil.getAllCmParameters();
            for (FnExecutor.CmParameter cmParameter : cmParameters) {
                options.add(cmParameter.toOption());
            }
        }
        return options;
    }

    public static void main(String... args) {
        try {


            CommandLine commandLine = null;
            try {
                commandLine = PARSER.parse(OPTIONS, args, false);
            } catch (ParseException e) {
                help("Error: Cannot parse command line");
            }
            String utilityName = commandLine.getOptionValue(OPTION_UTILITY.getLongOpt(), DEFAULT_UTILITY_NAME);


            Class<? extends FnExecutor> util = UTILS.get(utilityName);
            if (util == null) {
                help("Error: No utility '" + utilityName + "' present.");
            }
            FnExecutor instanceOfUtil = util.newInstance();
            Method methodRun = util.getMethod("execute", String[].class);
            methodRun.invoke(instanceOfUtil, new Object[]{buildResultingOptions(instanceOfUtil, commandLine)});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class OptionsSet extends LinkedHashSet<Option> {
        public void addAllOptions(CommandLine commandLine, Option[] exclude) {
            Iterator<Option> iterator = commandLine.iterator();
            while (iterator.hasNext()) {
                boolean add = true;
                Option option2add = iterator.next();
                if (exclude != null) {
                    for (Option option2exclude : exclude) {
                        if (option2add.equals(option2exclude)) {
                            add = false;
                            break;
                        }
                    }
                }
                if (add) {
                    add(option2add);
                }
            }
        }
    }

    private static String[] buildResultingOptions(FnExecutor executor, CommandLine commandLine) {

        String[] optionFiles = commandLine.getOptionValues(OPTION_OPTIONSFILE.getLongOpt());
        if (optionFiles == null || optionFiles.length == 0) {
            return commandLine.getArgs();
        }
        OptionsSet resultingOptions = new OptionsSet();
        //Сначала добавляем параметры из командной строки, исключая специфичные для Ruuner
        resultingOptions.addAllOptions(commandLine, new Option[]{OPTION_UTILITY, OPTION_OPTIONSFILE});
        //Переворачиваем массив файлов, с тем, чтобы параметры последующег офайла перетирали параметры предыдущего
        List<String> optionFilesList = new ArrayList<String>();
        optionFilesList.addAll(Arrays.asList(optionFiles));
        Collections.reverse(optionFilesList);
        for (String optionsFile : optionFilesList) {
            try {
                String[] fileOptions = readOptionsFile(optionsFile);
                CommandLine clFromFile = PARSER.parse(OPTIONS, fileOptions);
                resultingOptions.addAllOptions(clFromFile, null);

            } catch (FileNotFoundException fne) {
                help("Error: Could not find options file " + optionsFile);
            } catch (IOException e) {
                help("Error: Could not read options file " + optionsFile);
            } catch (ParseException e) {
                help("Error: Could not parse options file " + optionsFile);
            }
        }
        List<String> argList = new ArrayList<String>();
        for (Option option : resultingOptions) {
            for (String value : option.getValues()) {
                argList.add("-" + option.getOpt());
                argList.add(value);
            }
        }
        return argList.toArray(new String[argList.size()]);
    }

    private static String[] readOptionsFile(String path) throws IOException {
        List<String> options = new ArrayList<String>();
        File optionsFile = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(optionsFile));
        String line;
        while (( line = reader.readLine() ) != null) {
            options.add(line);
        }
        return options.toArray(new String[options.size()]);
    }

    private static void help(String message) {
        if (message != null) {
            System.out.println(message);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Runner.class.getSimpleName(), OPTIONS);
        System.exit(0);
    }

    private static <E extends FnExecutor> Map<String, Class<E>> getUtilitiesCollection() {
        //get all utilities with"@Utilities" annotation
        Map<String, Class<E>> result = new HashMap<String, Class<E>>();

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
                result.put(util.getSimpleName(), (Class<E>) util);
            } else {
                result.put(annotation.value(), (Class<E>) util);
            }
        }

        return result;
    }

}

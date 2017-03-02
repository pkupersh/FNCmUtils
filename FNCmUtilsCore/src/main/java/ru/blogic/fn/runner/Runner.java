package ru.blogic.fn.runner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import ru.blogic.fn.utils.CmParameter;
import ru.blogic.fn.utils.FnExecutor;
import ru.blogic.fn.utils.FnExecutorException;
import ru.blogic.fn.utils.FnExecutorManager;
import ru.blogic.fn.utils.InvalidParametersException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by sam158 on 28.06.2016.
 * This is entry point for all utilities. Name of utility must be in "--utility || -ut" parameter
 */
public class Runner {

    private static final String DEFAULT_UTILITY_NAME = "counter";

    private static final Option OPTION_OPTIONSFILE = new Option("o", "options", true, "File with options. You can provide this option several times (resulting options will be merged). Explicit options take precedence over options from file(s).");
    private static final Option OPTION_UTILITY = new Option("ut", "utility", true, "Name of utility to execute.");
    private static final Option OPTION_HELP = new Option("h", "help", false, "Get help for specific utility. Use with combination with --" + OPTION_UTILITY.getLongOpt() + " option");
    private static final Options ALL_OPTIONS = new Options();
    private static final Options RUNNER_OPTIONS = new Options();
    private static final CommandLineParser PARSER = new DefaultParser();

    private FnExecutor currentExecutor;
    private PrintWriter out;
    private PrintWriter err;

    private static final String APP_NAME;
    private static final String APP_VERSION;

    static {
        // init props
        InputStream propStream = Runner.class.getClassLoader().getResourceAsStream("app.properties");
        Properties props = new Properties();

        try {
            props.load(propStream);

        } catch (Exception ignored) {
        }

        APP_NAME=props.getProperty("name","{UNKNOWN}");
        APP_VERSION=props.getProperty("version","{UNKNOWN}");

        ALL_OPTIONS.addOption(OPTION_UTILITY);
        ALL_OPTIONS.addOption(OPTION_OPTIONSFILE);
        ALL_OPTIONS.addOption(OPTION_HELP);
        RUNNER_OPTIONS.addOption(OPTION_UTILITY);
        RUNNER_OPTIONS.addOption(OPTION_OPTIONSFILE);
        RUNNER_OPTIONS.addOption(OPTION_HELP);
        //UTILS = getUtilitiesCollection();
        OPTION_UTILITY.setDescription(OPTION_UTILITY.getDescription() + " Permitted values: " + FnExecutorManager.getAvailableExecutorNames() + ". You may also provide external utility class and use it's fully qualified name as a name of utility. Value by Default is " + DEFAULT_UTILITY_NAME);
        try {
            for (Option option : getAllOptions()) {
                ALL_OPTIONS.addOption(option);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<Option> getAllOptions() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Set<Option> options = new HashSet<Option>();
        for (String excutorName: FnExecutorManager.getAvailableExecutorNames()) {
            FnExecutor theUtil = FnExecutorManager.getExecutorInstance(excutorName);
            List<CmParameter> cmParameters = theUtil.getAllCmParameters();
            for (CmParameter cmParameter : cmParameters) {
                options.add(toOption(cmParameter));
            }
        }
        return options;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void setErr(PrintWriter err) {
        this.err = err;
    }

    private void printVersion() {
        out.println(getName()+" v. "+getVersion());

    }

    public static String getVersion() {
        return APP_VERSION;
    }

    public static String getName(){
        return APP_NAME;
    }

    public static void main(String... args) {
        Runner runner = new Runner();
        runner.out = new PrintWriter(System.out, true);
        runner.err = new PrintWriter(System.err, true);
        runner.run(args);
    }

    public void run(String... args) {
        try {
            printVersion();
            CommandLine commandLine;
            try {
                commandLine = PARSER.parse(ALL_OPTIONS, args, true);
            } catch (ParseException e) {
                help("Error: Cannot parse command line: " + e.getMessage(), null, null);
                return;
            }
            if (commandLine.hasOption(OPTION_OPTIONSFILE.getLongOpt())) {
                args = buildResultingOptions(commandLine);
                if (args == null) {
                    return;
                }
                commandLine = PARSER.parse(ALL_OPTIONS, args);
            }

            String utilityName = commandLine.getOptionValue(OPTION_UTILITY.getLongOpt(), null);
            boolean useDefaultUtility = false;
            if (utilityName == null) {
                utilityName = DEFAULT_UTILITY_NAME;
                useDefaultUtility = true;
            }


            FnExecutor executor;
            try {
                executor=FnExecutorManager.getExecutorInstance(utilityName);
            }catch (ClassNotFoundException e){
                //try to resolve string as a class name
                try {
                    executor=FnExecutorManager.findFnExecutorByClassName(utilityName);
                    if (executor == null) {
                        return;
                    }
                    //redo collecting options and command line parse here
                    executor.setOut(out);
                    executor.setErr(err);

                    List<CmParameter> cmParameters = executor.getAllCmParameters();
                    for (CmParameter cmParameter : cmParameters) {
                        ALL_OPTIONS.addOption(toOption(cmParameter));
                    }
                    try {
                        commandLine = PARSER.parse(ALL_OPTIONS, args, true);
                    } catch (ParseException pe) {
                        help("Error: Cannot parse command line: " + e.getMessage(), null, null);
                        return;
                    }
                } catch (ClassNotFoundException cnfe) {
                    help("Error: No utility '" + utilityName + "' present or found as a class.", null, null);
                    return;
                }
            }

            currentExecutor = executor;
            currentExecutor.setOut(out);
            currentExecutor.setErr(err);

            if (commandLine.hasOption(OPTION_HELP.getLongOpt())) {
                if (!useDefaultUtility) {
                    help(null, utilityName, currentExecutor.getAllCmParameters());
                    return;
                } else {
                    help(null, null, null);
                    return;
                }
            }

            execute(currentExecutor,getArgsWithoutCommon(commandLine));

        } catch (Exception e) {
            err.println(e.getMessage());
        }
    }

    public void interrupt() {
        if (currentExecutor != null) {
            currentExecutor.cancel();
        }
    }

    private static String[] getArgsWithoutCommon(CommandLine commandLine) {
        List<String> args = new ArrayList<String>();
        Iterator<Option> iterator = commandLine.iterator();
        while (iterator.hasNext()) {
            Option option = iterator.next();
            if (!option.equals(OPTION_UTILITY)) {
                args.add("--" + option.getLongOpt());
                args.add(option.getValue());
            }
        }
        return args.toArray(new String[args.size()]);
    }

    private Class<? extends FnExecutor> cast2FnExecutor(Class clazz, String utilityName) {
        if (!FnExecutor.class.isAssignableFrom(clazz)) {
            help("Error: Utility '" + utilityName + "' found as a class but DOES NOT extend " + FnExecutor.class.getName(), null, null);
            return null;
        }
        return (Class<? extends FnExecutor>) clazz;
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

    private String[] buildResultingOptions(CommandLine commandLine) {

        String[] optionFiles = commandLine.getOptionValues(OPTION_OPTIONSFILE.getLongOpt());
        if (optionFiles == null || optionFiles.length == 0) {
            return commandLine.getArgs();
        }
        OptionsSet resultingOptions = new OptionsSet();
        //Сначала добавляем параметры из командной строки, исключая optionsFile
        resultingOptions.addAllOptions(commandLine, new Option[]{OPTION_OPTIONSFILE});
        //Переворачиваем массив файлов, с тем, чтобы параметры последующего файла перетирали параметры предыдущего
        List<String> optionFilesList = new ArrayList<String>();
        optionFilesList.addAll(Arrays.asList(optionFiles));
        Collections.reverse(optionFilesList);
        for (String optionsFile : optionFilesList) {
            try {
                String[] fileOptions = readOptionsFile(optionsFile);
                CommandLine clFromFile = PARSER.parse(ALL_OPTIONS, fileOptions);
                resultingOptions.addAllOptions(clFromFile, null);

            } catch (FileNotFoundException fne) {
                help("Error: Could not find options file " + optionsFile, null, null);
                return null;
            } catch (IOException e) {
                help("Error: Could not read options file " + optionsFile, null, null);
                return null;
            } catch (ParseException e) {
                help("Error: Could not parse options file " + optionsFile, null, null);
                return null;
            }
        }
        List<String> argList = new ArrayList<String>();
        for (Option option : resultingOptions) {
            String[] values = option.getValues();
            if (values != null) {
                for (String value : values) {
                    argList.add("--" + option.getLongOpt());
                    argList.add(value);
                }
            } else {
                argList.add("--" + option.getLongOpt());
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
            //exclude empty and commented lines
            if (!"".equals(line) && line.charAt(0) != '\'') {
                options.add(line);
            }
        }
        return options.toArray(new String[options.size()]);
    }

    private void help(String message, String utilName, List<CmParameter> cmParameters) {
        if (message != null) {
            err.println(message);
        }
        HelpFormatter formatter = new HelpFormatter();
        if (cmParameters == null) {
            formatter.printHelp(out, 80, "java -jar "+getName()+".jar <basic options> <utility specific options>", "Basic options:", RUNNER_OPTIONS, 0, 0, "");
        } else {
            Options options = new Options();
            options.addOption(OPTION_OPTIONSFILE);

            for (CmParameter cmParameter : cmParameters) {
                options.addOption(toOption(cmParameter));
            }
            formatter.printHelp(out, 80, "java -jar "+getName()+".jar --" + OPTION_UTILITY.getLongOpt() + "=" + utilName + " <options>", "Options:", options, 0, 0, "");
        }
    }

    /*private static <E extends FnExecutor> Map<String, Class<E>> getUtilitiesCollection() {
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
    }*/

    public static Option toOption(CmParameter cmParameter){
        return new Option(cmParameter.getShortName(), cmParameter.getName(), cmParameter.isHasArgs(), cmParameter.getDescr());
    }

    public void execute(FnExecutor executor, String[] args) {

        Options options = new Options();
        try {
            List<CmParameter> cmParameters = executor.getAllCmParameters();
            Map<String,CmParameter> cmParametersByName=new HashMap<String, CmParameter>();
            for (CmParameter paramDef : cmParameters ) {
                options.addOption(toOption(paramDef));
                cmParametersByName.put(paramDef.getName(),paramDef);
            }
            CommandLine parsedLine;
            try {
                parsedLine = PARSER.parse(options, args);
            } catch (ParseException e) {
                out.println("Error: " + e.getMessage());
                help(executor,options);
                return;
            }
            Map<CmParameter, String> paramValues = new HashMap<CmParameter, String>();

            Iterator<Option> iterator = parsedLine.iterator();
            while (iterator.hasNext()){
                Option option = iterator.next();
                CmParameter cmParameter=cmParametersByName.get(option.getLongOpt());
                if(cmParameter !=null) {
                    paramValues.put(cmParameter,option.getValue());
                }
            }


            executor.execute(paramValues);

        } catch (InvalidParametersException ipe) {
            ipe.printMessages(err);
            help(executor, options);
        } catch (FnExecutorException e) {
            err.println("Error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace(err);
        }
    }

    private void help(FnExecutor executor, Options options) {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(out, 80, executor.getExecutorName()+" <options>", "where <options> are:", options, 0, 0, "", false);
    }
}

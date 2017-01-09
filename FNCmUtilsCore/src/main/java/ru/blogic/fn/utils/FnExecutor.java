package ru.blogic.fn.utils;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pkupershteyn on 03.06.2016.
 * Abstract FileNet Executor
 */
public abstract class FnExecutor {

    private final List<CmParameter> cmParameters;

    static private final CommandLineParser PARSER = new DefaultParser();

    protected static final CmParameter CMPARM_URI = new CmParameter("uri", "U", true, "An url to connect to filenet", true, null);
    protected static final CmParameter CMPARM_USER = new CmParameter("user", "u", true, "User name", true, null);
    protected static final CmParameter CMPARM_PWD = new CmParameter("password", "p", true, "Usr password", true, null);
    protected static final CmParameter CMPARM_OS = new CmParameter("objectStore", "s", true, "An object store to connect to", true, null);

    private PrintWriter out;
    private PrintWriter err;

    /**
     * Sets writer for Executor's standard output
     *
     * @param out Writer for Executor standard output
     */
    public void setOut(PrintWriter out) {
        this.out = out;
    }

    /**
     * Sets writer for Executor's error output
     *
     * @param err Writer for Executor's error output
     */
    public void setErr(PrintWriter err) {
        this.err = err;
    }

    public PrintWriter getOut() {
        return out;
    }

    public PrintWriter getErr() {
        return err;
    }

    /**
     * Exception that must be thrown in case of error found in any Executor parameter
     */
    public static class InvalidParametersException extends Exception {
        private final CmParameter cmParameter;

        /**
         * @param parameter Executor parameter that contains wrong value
         * @param message   Message that describes error details
         */
        public InvalidParametersException(CmParameter parameter, String message) {
            super(message);
            cmParameter = parameter;
        }

        /**
         * Get wrong parameter
         *
         * @return Wrong parameter
         */
        public CmParameter getCmParameter() {
            return cmParameter;
        }
    }

    static final List<CmParameter> CONNECTION_PARAMETERS = Collections.unmodifiableList(Arrays.asList(CMPARM_URI,
            CMPARM_USER,
            CMPARM_PWD,
            CMPARM_OS));

    /**
     * Executor parameter
     */
    public static class CmParameter {
        private final String name;
        private final String shortName;
        private final boolean hasArgs;
        private final String descr;
        private final boolean mandatory;
        private final String defaultValue;

        /**
         * Constructs new mandatory Executor parameter thith null default value
         *
         * @param name      Name of parameter
         * @param shortName Short name of parameter
         * @param hasArgs   Does that parameter has arguments.
         * @param descr     Description of parameter
         */
        public CmParameter(String name, String shortName, boolean hasArgs, String descr) {
            this(name, shortName, hasArgs, descr, true, null);
        }

        /**
         * Constructs new Executor parameter
         *
         * @param name         Name of parameter
         * @param shortName    Short name of parameter
         * @param hasArgs      Does that parameter has arguments.
         * @param descr        Description of parameter
         * @param mandatory    Is that parameter mandatory
         * @param defaultValue Default value for parameter
         */
        public CmParameter(String name, String shortName, boolean hasArgs, String descr, boolean mandatory, String defaultValue) {
            this.name = name;
            this.shortName = shortName;
            this.hasArgs = hasArgs;
            this.descr = descr;
            this.mandatory = mandatory;
            this.defaultValue = defaultValue;
        }

        /**
         * @return Does that parameter has arguments.
         */
        public boolean isHasArgs() {
            return hasArgs;
        }

        /**
         * @return Parameter name
         */
        public String getName() {
            return name;
        }

        /**
         * @return Parameter description
         */
        public String getDescr() {
            return descr;
        }

        /**
         * @return Parameter short name
         */
        public String getShortName() {
            return shortName;
        }

        /**
         * @return Is that parameter mandatory
         */
        public boolean isMandatory() {
            return mandatory;
        }

        /**
         * @return Parameter default value
         */
        public String getDefaultValue() {
            return defaultValue;
        }


        public Option toOption() {
            return new Option(shortName, name, hasArgs, descr);
        }

        @Override
        public String toString() {
            return "--" + getName() + " (-" + getShortName() + ")";
        }
    }

    /**
     * Must return a list of parameters that are defined immediately by implementing class
     * The final list of all parameters will be merged (@see {@link #getAllCmParameters()})
     *
     * @return
     */
    protected abstract List<CmParameter> getImmediateCmParameters();

    /**
     * Get All Executor parameters
     *
     * @return All Executor parameters
     */
    public List<CmParameter> getAllCmParameters() {
        List<CmParameter> cmParameters = new ArrayList<CmParameter>();
        cmParameters.addAll(CONNECTION_PARAMETERS);
        cmParameters.addAll(getImmediateCmParameters());
        return cmParameters;
    }

    protected FnExecutor() {
        cmParameters = getAllCmParameters();
    }

    /**
     * Main entry point to run executor from command line
     *
     * @param args Command line arguments
     */
    public void execute(String[] args) {
        Options options = new Options();
        try {

            for (CmParameter paramDef : cmParameters) {
                options.addOption(paramDef.toOption());
            }
            CommandLine parsedLine;
            try {
                parsedLine = PARSER.parse(options, args);
            } catch (ParseException e) {
                out.println("Error: " + e.getMessage());
                help(options);
                return;
            }
            boolean emptyParams = false;
            Map<CmParameter, String> paramValues = new HashMap<CmParameter, String>();
            for (CmParameter paramDef : cmParameters) {
                if (paramDef.isMandatory() && !parsedLine.hasOption(paramDef.getName())) {
                    out.println("No parameter --" + paramDef.getName() + " (-" + paramDef.getShortName() + ") specified");
                    emptyParams = true;
                }
                String v = parsedLine.getOptionValue(paramDef.getName());
                if (v == null) {
                    v = paramDef.getDefaultValue();
                }
                paramValues.put(paramDef, v);
            }
            if (emptyParams) {
                help(options);
                return;
            }

            initCmParameterValues(paramValues);

            doWork(paramValues);

        } catch (InvalidParametersException ipe) {
            err.println("Wrong parameter " + ipe.getCmParameter().getName() + ": " + ipe.getLocalizedMessage());
            help(options);
        } catch (FnExecutorException e) {
            err.println("Error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace(err);
        }
    }


    private void help(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(out, 80, this.getClass().getSimpleName(), "", options, 0, 0, "", false);
    }


    private void doWork(Map<CmParameter, String> parms) throws Exception {
        String uri = parms.get(CMPARM_URI);
        String username = parms.get(CMPARM_USER);
        String password = parms.get(CMPARM_PWD);


        String OS = parms.get(CMPARM_OS);
        // Get the connection

        Connection conn = Factory.Connection.getConnection(uri);

        // Get the user context
        UserContext uc = UserContext.get();

        uc.pushSubject(
                UserContext.createSubject(conn, username, password, "FileNetP8WSI")
        );
        try {
            // Get the default domain
            Domain domain = Factory.Domain.fetchInstance(conn, null, null);
            // Get an object store
            ObjectStore os = Factory.ObjectStore.fetchInstance(domain,
                    OS, null);

            out.println("OS name '" + os.get_Name() + "' (id: '" + os.get_Id().toString() + "') descriptive text: " + os.get_DescriptiveText());

            doFnWork(domain, os, parms);


        } finally {
            // Pop the subject off the UserContext stack
            uc.popSubject();
        }
    }

    /**
     * The main method that must be implemented by descendants Performs the Executor FileNet work.
     *
     * @param domain      FileNet domain
     * @param objectStore FileNet Object Store
     * @param parms       Excecutor parameters
     * @throws Exception
     */
    protected abstract void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception;

    /**
     * Method that must perform parameter value checking  and initialization.
     *
     * @param parms map of parameter-value pairs
     * @throws InvalidParametersException
     */
    protected abstract void initCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException;


    /**
     * Write to standard output
     *
     * @param info The string to write
     */
    protected void printInfo(String info) {
        if (info != null) {
            out.println(info);
        }
    }

    /**
     * Write to error output
     *
     * @param error The satring to write
     */
    protected void printError(String error) {
        if (error != null) {
            err.println(error);
        }
    }
}

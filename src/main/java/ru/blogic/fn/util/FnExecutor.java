package ru.blogic.fn.util;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pkupershteyn on 03.06.2016.
 */
public abstract class FnExecutor {
    private List<CmParameter> cmParameters;

    static private final CommandLineParser PARSER = new DefaultParser();

    protected static final CmParameter CMPARM_URI = new CmParameter("uri", "U", true, "An url to connect to filenet", true, null);
    protected static final CmParameter CMPARM_USER = new CmParameter("user", "u", true, "User name", true, null);
    protected static final CmParameter CMPARM_PWD = new CmParameter("password", "p", true, "Usr password", true, null);
    protected static final CmParameter CMPARM_OS = new CmParameter("objectStore", "s", true, "An object store to connect to", true, null);

    public static class InvalidParametersException extends Exception {
        private CmParameter cmParameter;
        public InvalidParametersException(CmParameter parameter, String message) {
            super(message);
            cmParameter=parameter;
        }


        public CmParameter getCmParameter() {
            return cmParameter;
        }
    }

    protected static final List<CmParameter> CONNECTION_PARAMETERS = Arrays.asList(new CmParameter[]{
            CMPARM_URI,
            CMPARM_USER,
            CMPARM_PWD,
            CMPARM_OS
    });

    public static class CmParameter {
        private final String name;
        private String shortName;
        private final boolean hasArgs;
        private final String descr;
        private final boolean mandatory;
        private final String defaultValue;

        CmParameter(String name, String shortName, boolean hasArgs, String descr) {
            this(name, shortName, hasArgs, descr, true, null);
        }

        CmParameter(String name, String shortName, boolean hasArgs, String descr, boolean mandatory, String defaultValue) {
            this.name = name;
            this.shortName = shortName;
            this.hasArgs = hasArgs;
            this.descr = descr;
            this.mandatory = mandatory;
            this.defaultValue = defaultValue;
        }

        public boolean isHasArgs() {
            return hasArgs;
        }

        public String getName() {
            return name;
        }

        public String getDescr() {
            return descr;
        }

        public String getShortName() {
            return shortName;
        }

        public boolean isMandatory() {
            return mandatory;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        Option toOption() {
            return new Option(shortName, name, hasArgs, descr);
        }

    }

    public abstract List<CmParameter> constructCmParameters();

    private List<CmParameter> getCmParametersInternal() {
        List<CmParameter> cmParameters = new ArrayList<CmParameter>();
        cmParameters.addAll(CONNECTION_PARAMETERS);
        cmParameters.addAll(constructCmParameters());
        return cmParameters;
    }

    protected FnExecutor() {
        cmParameters = getCmParametersInternal();
    }


    public void execute(String[] args) {
        Options options = new Options();
        try {

            for (CmParameter paramDef : cmParameters) {
                options.addOption(paramDef.toOption());
            }
            CommandLine parsedLine = PARSER.parse(options, args);

            boolean emptyParams = false;
            Map<CmParameter, String> paramValues = new HashMap<CmParameter,String>();
            for (CmParameter paramDef : cmParameters) {
                if (paramDef.isMandatory() && !parsedLine.hasOption(paramDef.getName())) {
                    System.out.println("No parameter --" + paramDef.getName() + " (-" + paramDef.getShortName() + ") specified");
                    emptyParams = true;
                }
                String v=parsedLine.getOptionValue(paramDef.getName());
                if(v==null){
                    v=paramDef.getDefaultValue();
                }
                paramValues.put(paramDef,v );
            }
            if (emptyParams) {
                help(options);
            }

            initCmParameterValues(paramValues);

            doWork(paramValues);

        } catch (InvalidParametersException ipe) {
            System.out.println("Wrong parameter "+ipe.getCmParameter().getName()+": " + ipe.getLocalizedMessage());
            help(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void help(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(FNDeleterOld.class.getSimpleName(), options);
        System.exit(0);
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
            Domain domain = Factory.Domain.getInstance(conn, null);
            // Get an object store
            ObjectStore os = Factory.ObjectStore.fetchInstance(domain,
                    OS, null);

            System.out.println("OS name '" + os.get_Name() + "' (id: '" + os.get_Id().toString() + "') descriptive text: " + os.get_DescriptiveText());

            doFnWork(domain, os, parms);


        } finally {
            // Pop the subject off the UserContext stack
            uc.popSubject();
        }
    }

    protected abstract void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception;

    protected void initCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException {
    }
}

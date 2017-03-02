package ru.blogic.fn.utils;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import ru.blogic.fn.utils.annotations.Utility;
import ru.blogic.fn.utils.concurrent.FnUtilRunnableParent;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by pkupershteyn on 03.06.2016.
 * Abstract FileNet Executor
 */
public abstract class FnExecutor implements FnUtilRunnableParent {

    private List<CmParameter> cmParameters;

    protected static final CmParameter CMPARM_URI = new CmParameter("uri", "U", true, "An url to connect to filenet", true, null);
    protected static final CmParameter CMPARM_USER = new CmParameter("user", "u", true, "User name", true, null);
    protected static final CmParameter CMPARM_PWD = new CmParameter("password", "p", true, "User password", true, null);
    protected static final CmParameter CMPARM_OS = new CmParameter("objectStore", "s", true, "An object store to connect to", true, null);

    private PrintWriter out;
    private PrintWriter err;

    private final AtomicBoolean canceled = new AtomicBoolean(false);

    static final List<CmParameter> CONNECTION_PARAMETERS = Collections.unmodifiableList(Arrays.asList(
            CMPARM_URI,
            CMPARM_USER,
            CMPARM_PWD,
            CMPARM_OS
    ));

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
     * Must return a list of applied parameters (i.e. all parameters except connectivity)
     * The final list of all parameters will be merged (@see {@link #getAllCmParameters()})
     *
     * @return
     */
    protected abstract List<CmParameter> getAppliedCmParameters();

    /**
     * Get All Executor parameters
     *
     * @return All Executor parameters
     */
    public List<CmParameter> getAllCmParameters() {
        if (cmParameters == null) {
            cmParameters = new ArrayList<CmParameter>();
            cmParameters.addAll(getConnectionParameters());
            cmParameters.addAll(getAppliedCmParameters());
        }
        return cmParameters;
    }

    public String getExecutorName() {
        Class c = this.getClass();
        Utility annotation = (Utility) c.getAnnotation(Utility.class);
        if (( annotation == null || "".equals(annotation.value().trim()) )) {
            return c.getSimpleName();
        } else {
            return annotation.value();
        }
    }

    /**
     * The main method that does execution
     * @param parms Map of parameter values
     * @throws Exception
     */
    public void execute(Map<CmParameter, String> parms) throws Exception {

        checkMandatoryAndSetDefaults(parms);
        checkCmParameterValues(parms);

        String uri = parms.get(CMPARM_URI);
        String username = parms.get(CMPARM_USER);
        String password = parms.get(CMPARM_PWD);


        String OS = parms.get(CMPARM_OS);
        // Get the connection

        long millis = System.currentTimeMillis();

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

            printInfo("OS name '" + os.get_Name() + "' (id: '" + os.get_Id().toString() + "') descriptive text: " + os.get_DescriptiveText());

            doFnWork(domain, os, parms);

            printInfo(String.format("%s finished (duration: %s ms).", getExecutorName(), System.currentTimeMillis() - millis));

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
     * Method that must perform parameter value checking and initialization.
     *
     * @param parms map of parameter-value pairs
     * @throws InvalidParametersException
     */
    protected abstract void checkCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException;

    /**
     * Returns description of executor
     * @return description of executor
     */
    public abstract String getExecutorDescription();

    private void checkMandatoryAndSetDefaults(Map<CmParameter, String> parms) throws InvalidParametersException {
        Map<CmParameter, String> paramErrors = new HashMap<CmParameter, String>();
        for (CmParameter cmParameter : getAllCmParameters()) {
            if (parms.get(cmParameter) == null && cmParameter.getDefaultValue() != null) {
                parms.put(cmParameter, cmParameter.getDefaultValue());
            }
            if (parms.get(cmParameter) == null && cmParameter.isMandatory()) {
                printError("No parameter --" + cmParameter.getName() + " (-" + cmParameter.getShortName() + ") specified");
                paramErrors.put(cmParameter, "No value specified for mandatory parameter");
            }
        }

        if (!paramErrors.isEmpty()) {
            throw new InvalidParametersException(paramErrors);
        }
    }


    /**
     * Write to standard output
     *
     * @param info The string to write
     */
    public void printInfo(String info) {
        System.out.println("try to printinfo");
        if (info != null) {
            out.println(info);
        }
    }

    /**
     * Write to error output
     *
     * @param error The satring to write
     */
    public void printError(String error) {
        if (error != null) {
            err.println(error);
        }
    }

    /**
     * Provides ability to check if curent executor was canceled (@see {@link #cancel()})
     * @return
     */
    public final AtomicBoolean isCanceled() {
        return canceled;
    }

    /**
     * Cancels execution/
     */
    public void cancel() {
        canceled.set(true);
        System.out.println(getExecutorName()+" cancel flag is set");
    }

    /**
     * The method to use inside executors's body (@see {@link #doFnWork(Domain, ObjectStore, Map)}) to check if current executio is canceled
     * Executor implementation must call this method before every atomic operation to check cancel status, and terminate execution if it is set
     * @return cancel status
     */
    protected boolean checkCanceled() {
        if (isCanceled().get()) {
            printInfo("Cancel signal detected, stopping "+getExecutorName()+"...");
            return true;
        }
        return false;
    }

    public final static List<CmParameter> getConnectionParameters(){
        return new ArrayList<CmParameter>(CONNECTION_PARAMETERS);
    }
}

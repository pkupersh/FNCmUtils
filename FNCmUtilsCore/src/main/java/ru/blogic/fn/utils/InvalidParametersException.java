package ru.blogic.fn.utils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception that must be thrown in case of error found in any Executor parameter
 */
public class InvalidParametersException extends Exception {
    private final Map<CmParameter, String> cmParameters = new HashMap<CmParameter, String>();

    /**
     * @param parameter Executor parameter that contains wrong value
     * @param message   Message that describes error details
     */
    public InvalidParametersException(CmParameter parameter, String message) {
        super(message);
        cmParameters.put(parameter, message);
    }

    public InvalidParametersException(Map<CmParameter, String> cmParameters) {
        super("Multiply wrong parameters");
        cmParameters.putAll(cmParameters);
    }

    /**
     * Get wrong parameters
     *
     * @return Wrong parameters
     */
    public Map<CmParameter, String> getCmParameters() {
        return cmParameters;
    }

    /**
     * Prints information about wrong paramteters
     * @param out output
     */
    public void printMessages(PrintWriter out) {
        for (Map.Entry<CmParameter, String> entry : cmParameters.entrySet()) {
            out.println("Wrong parameter: " + entry.getKey().getName() + ": " + entry.getValue());
        }
    }
}

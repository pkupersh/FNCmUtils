package ru.blogic.fn.utils;

/**
 * Executor parameter
 */
public class CmParameter {
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


    @Override
    public String toString() {
        return "--" + getName() + " (-" + getShortName() + ")";
    }
}

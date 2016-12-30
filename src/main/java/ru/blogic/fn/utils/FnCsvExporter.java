package ru.blogic.fn.utils;

import com.filenet.api.collection.EngineCollection;
import com.filenet.api.collection.PageIterator;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.core.EngineObject;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.UpdatingBatch;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.meta.ClassDescription;
import com.filenet.api.meta.PropertyDescription;
import com.filenet.api.property.Property;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import org.apache.commons.csv.CSVFormat;
import ru.blogic.fn.utils.annotations.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by pkupershteyn on 12.12.2016.
 */
@Utility("csvExporter")
public class FnCsvExporter extends FnSelectExecutor {

    private static final String DEFAULT_DELIMITER_4_STDOUT = "\t";
    private static final String DEFAULT_DELIMITER_4_FILE = ",";


    protected static final CmParameter CMPARM_OUTPUT = new CmParameter("output", "out", true, "output CSV file. If omitted, the data are written to stdout", false, null);
    protected static final CmParameter CMPARM_CHARSET = new CmParameter("charset", "cs", true, "output CSV file character set. UTF-8 is used if omitted. Note that parameter is only for file, (default charset is always used for stdout. Use -Dfile.encoding to change it)", false, "utf-8");
    protected static final CmParameter CMPARM_DELIMITER = new CmParameter("delimiter", "d", true, "delimiter for CSV data. If omitted, ',' (comma) is used for given output file, and '\\t' is used for output to  stdout.", false, DEFAULT_DELIMITER_4_FILE);
    protected static final CmParameter CMPARM_QUOTE = new CmParameter("quote", "qt", true, "quote for CSV file. If omitted, '\"' (doulble quote) is used.", false, "\"");
    protected static final CmParameter CMPARM_RECORD_SEPARATOR = new CmParameter("recordSeparator", "rc", true, "Record separator for CSV file. If omitted, '\\r\\n' (CRLF) is used.", false, "\r\n");

    protected static final CmParameter CMPARM_FIELDS;

    private boolean doOutput = true;

    private CSVFormat csvFormat;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("Comma separated fields to export. Wildcards in combinations may be used. Allowed wildcards:");
        for (FieldWildcard fw : FieldWildcard.values()) {
            sb.append("\t'");
            sb.append(fw.getWildcard());
            sb.append("':\t");
            sb.append(fw.getDescription());
        }
        CMPARM_FIELDS = new CmParameter("fields", "f", true, sb.toString(), false, null);
    }

    private static final String WIDLCARD_PREFIX = "$";
    private static final String WILDCARD_ASTERISK = "*";

    private static Pattern FIELDS_MATCH_PATTERN = Pattern.compile("^(([" + WIDLCARD_PREFIX + "a-zA-Z][a-zA-Z0-9_]*)|\\" + WILDCARD_ASTERISK + ")(\\s*,\\s*(([" + WIDLCARD_PREFIX + "a-zA-Z][a-zA-Z0-9_]+)|\\" + WILDCARD_ASTERISK + "))*$");
    private static String FIELDS_SEPARATOR = ",";
    private List<String> fields = new ArrayList<String>();
    private PrintWriter output;
    private boolean output2Stdout = false;
    private boolean headStringWritten = false;
    private String recordSeparator;

    enum FieldWildcard {

        direct("All properties directly declared in given class"),
        custom("All custom properties, including inherited"),
        all(WILDCARD_ASTERISK, "All properties. Of course, any combinations with that wildcard are meaningless."),
        system("All system properties"),
        systemDates("Creation and modification dates");

        private String wildcard;
        private String description;

        FieldWildcard(String wildcard, String description) {
            this.wildcard = wildcard;
            this.description = description;
        }

        FieldWildcard(String description) {
            this.wildcard = WIDLCARD_PREFIX + this.name();
            this.description = description;
        }

        public String getWildcard() {
            return wildcard;
        }

        public String getDescription() {
            return description;
        }

        public static FieldWildcard find(String wc) {
            if (wc == null) {
                return null;
            }
            wc = wc.toLowerCase();
            for (FieldWildcard wcard : values()) {
                if (wcard.wildcard.toLowerCase().equals(wc)) {
                    return wcard;
                }
            }
            return null;
        }
    }

    static private class FieldWildCardResolver {
        private ObjectStore os;
        private String className;
        private ClassDescription classDescription;

        FieldWildCardResolver(ObjectStore os, String className) {
            this.os = os;
            this.className = className;
        }

        private List<String> getPropertyNames(boolean doHier, boolean doSystem, boolean doCustom) {
            if (classDescription == null) {
                classDescription = Factory.ClassDescription.fetchInstance(os, className, null);
            }
            return fetchPropertyNames(classDescription, doHier, doSystem, doCustom);
        }

        private List<String> fetchPropertyNames(ClassDescription cd, boolean doDirect, boolean doSystem, boolean doCustom) {
            if (className == null) {
                return Collections.emptyList();
            }
            List<String> props = new ArrayList<String>();
            List<String> parentProps = new ArrayList<String>();
            if (doDirect) {
                ClassDescription parentCd = cd.get_SuperclassDescription();
                for (Object o : parentCd.get_PropertyDescriptions()) {
                    PropertyDescription pd = (PropertyDescription) o;
                    parentProps.add(pd.get_SymbolicName());
                }
            }


            for (Object o : cd.get_PropertyDescriptions()) {
                PropertyDescription pd = (PropertyDescription) o;
                if (( doDirect && !parentProps.contains(pd.get_SymbolicName()) ) || ( doCustom && !pd.get_IsSystemOwned() ) || ( doSystem && pd.get_IsSystemOwned() )) {
                    props.add(pd.get_SymbolicName());
                }
            }
            return props;
        }

        public List<String> resolveWildcard(FieldWildcard wildcard) {
            switch (wildcard) {
                case direct:
                    return getPropertyNames(true, false, false);
                case custom:
                    return getPropertyNames(false, false, true);
                case all:
                    return Collections.singletonList(WILDCARD_ASTERISK);
                case system:
                    return getPropertyNames(false, true, false);
                case systemDates:
                    return Arrays.asList(PropertyNames.DATE_CREATED, PropertyNames.DATE_LAST_MODIFIED);
            }
            throw new RuntimeException("Unknown wildcard " + wildcard);
        }
    }

    @Override
    public List<CmParameter> constructCmParameters() {
        List<CmParameter> params = new ArrayList<CmParameter>();
        params.addAll(super.constructCmParameters());
        params.add(CMPARM_OUTPUT);
        params.add(CMPARM_FIELDS);
        params.add(CMPARM_DELIMITER);
        params.add(CMPARM_QUOTE);
        params.add(CMPARM_RECORD_SEPARATOR);
        params.add(CMPARM_CHARSET);
        return params;
    }

    @Override
    protected void initCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException {
        String sql = parms.get(CMPARM_SQL);
        if (sql == null) {
            CmParameter wrongParm = null;
            String className = parms.get(CMPARM_CLASSNAME);
            String where = parms.get(CMPARM_WHERE);
            String fields = parms.get(CMPARM_FIELDS);
            if (className == null) {
                wrongParm = CMPARM_CLASSNAME;
            } else if (where == null) {
                wrongParm = CMPARM_WHERE;
            } else if (fields == null) {
                wrongParm = CMPARM_FIELDS;
            }
            if (wrongParm != null) {
                throw new InvalidParametersException(wrongParm, String.format("You must provide '%s' parameter or three parameters together: '%s','%s' and '%s'", CMPARM_SQL, CMPARM_CLASSNAME, CMPARM_WHERE, CMPARM_FIELDS));
            }
            if (!FIELDS_MATCH_PATTERN.matcher(fields).matches()) {
                throw new InvalidParametersException(CMPARM_FIELDS, "You must provide a comma separated list of required properties to export");
            }

            for (String fldStr : fields.split(FIELDS_SEPARATOR)) {
                fldStr = fldStr.trim();
                this.fields.add(fldStr);
            }
        }

        String outfile = parms.get(CMPARM_OUTPUT);
        try {
            if (outfile != null) {
                try {
                    output = new PrintWriter(new File(outfile), parms.get(CMPARM_CHARSET));
                } catch (FileNotFoundException e) {
                    throw new InvalidParametersException(CMPARM_OUTPUT, "Unable to write to " + outfile);
                }
            } else {
                output = new PrintWriter(System.out);
                output2Stdout = true;
                if (DEFAULT_DELIMITER_4_FILE.equals(parms.get(CMPARM_DELIMITER))) {
                    parms.put(CMPARM_DELIMITER, DEFAULT_DELIMITER_4_STDOUT);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new InvalidParametersException(CMPARM_CHARSET, "Unsupported charset: " + parms.get(CMPARM_CHARSET));
        }

        csvFormat = CSVFormat.RFC4180.withDelimiter(parms.get(CMPARM_DELIMITER).charAt(0))
                .withRecordSeparator(parms.get(CMPARM_RECORD_SEPARATOR))
                .withQuote(parms.get(CMPARM_QUOTE).charAt(0));
        recordSeparator = parms.get(CMPARM_RECORD_SEPARATOR);
    }

    private void closeOutput() {
        if (output != null) {
            output.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeOutput();
        super.finalize();
    }

    @Override
    protected List<String> getFieldSelectList(ObjectStore objectStore, Map<CmParameter, String> parms) {
        FieldWildCardResolver resolver = new FieldWildCardResolver(objectStore, parms.get(CMPARM_CLASSNAME));
        Set<String> fnFields = new LinkedHashSet<String>();
        for (String fldOrWildcard : fields) {
            FieldWildcard fieldWildcard = FieldWildcard.find(fldOrWildcard);
            if (fieldWildcard == null) {
                fnFields.add(fldOrWildcard);
            } else {
                fnFields.addAll(resolver.resolveWildcard(fieldWildcard));
            }
        }
        return new ArrayList<String>(fnFields);
    }

    @Override
    protected FetchType getFetchType() {
        return FetchType.repositoryRows;
    }


    private void doRowOutput(String outputStr) {
        output.print(outputStr);
        output.print(recordSeparator);
        output.flush();
    }


    @Override
    protected boolean processObject(Object object, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception {

        RepositoryRow row = (RepositoryRow) object;
        if (!headStringWritten) {
            outputHeaderString(row);
        }
        List<String> values = new ArrayList<String>();
        Iterator iterator = row.getProperties().iterator();
        while (iterator.hasNext()) {
            Property property = (Property) iterator.next();
            values.add(toString(property));
        }
        doRowOutput(csvFormat.format(values.toArray()));
        return true;
    }

    private void outputHeaderString(RepositoryRow row) {
        List<String> values = new ArrayList<String>();
        Iterator iterator = row.getProperties().iterator();
        while (iterator.hasNext()) {
            Property property = (Property) iterator.next();
            values.add(property.getPropertyName());
        }
        doRowOutput(csvFormat.format(values.toArray()));
        headStringWritten = true;
    }

    private String toString(Object objectValue) {

        if (objectValue instanceof Property) {
            Property property = (Property) objectValue;
            objectValue = property.getObjectValue();
        }

        if (objectValue instanceof EngineObject) {
            EngineObject eo = (EngineObject) objectValue;
            String name = "";
            try {
                name = eo.getProperties().getStringValue(PropertyNames.NAME);
            } catch (EngineRuntimeException e) {
            }
            try {
                name += eo.getProperties().getIdValue(PropertyNames.ID);
            } catch (EngineRuntimeException e) {
                if (name.equals("")) {
                    name = "?";
                }
            }
            objectValue = name;
        } else if (objectValue instanceof EngineCollection) {
            List<String> values = new ArrayList<String>();
            Iterator iterator = ( (EngineCollection) objectValue ).iterator();
            while (iterator.hasNext()) {
                values.add(toString(iterator.next()));
            }
            objectValue = values.toString();
        } else if (objectValue instanceof Date) {
            objectValue = DATE_FORMAT.format(objectValue);
        }
        return objectValue != null ? objectValue.toString() : "null";
    }

    @Override
    protected String getBeforeProcessMessage(int counter) {
        return null;
    }

    @Override
    protected String getUpdateBatchMessage() {
        return null;
    }

    @Override
    protected String getPostProcessVerb() {
        return "Exported";
    }

    @Override
    protected PageIterator getPageIterator(ObjectStore objectStore, SearchSQL sql, int pageSize) {
        doOutput = !output2Stdout;
        return super.getPageIterator(objectStore, sql, pageSize);
    }

    @Override
    protected void printInfo(String info) {
        if (doOutput) {
            super.printInfo(info);
        }
    }
}

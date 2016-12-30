package ru.blogic.fn.utils;

import com.filenet.api.collection.EngineSet;
import com.filenet.api.collection.PageIterator;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Domain;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.UpdatingBatch;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by pkupershteyn on 07.06.2016.
 */
public abstract class FnSelectExecutor extends FnExecutor {
    protected static final CmParameter CMPARM_PAGESIZE = new CmParameter("pageSize", "P", true, "Size of chunks to select", false, "500");
    protected static final CmParameter CMPARM_CLASSNAME = new CmParameter("className", "c", true, "FileNet class to search for", false, null);
    protected static final CmParameter CMPARM_WHERE = new CmParameter("where", "w", true, "Where clause to filter object of given class", false, null);
    protected static final CmParameter CMPARM_SQL = new CmParameter("sql", "q", true, "A whole sql to execute. Use instead of combination of "+CMPARM_CLASSNAME+" and "+CMPARM_WHERE, false, null);

    protected static final NumberFormat NUM_FORMAT = new DecimalFormat("#.#");

    protected enum FetchType {
        repositoryRows,
        engineObjects
    }

    public FnSelectExecutor() {
        super();
    }

    @Override
    public List<CmParameter> constructCmParameters() {
        return Arrays.asList(CMPARM_SQL, CMPARM_CLASSNAME, CMPARM_WHERE, CMPARM_PAGESIZE);
    }

    private static String join(String delimiter, Iterable<String> iterable) {
        StringBuilder sb = new StringBuilder();
        for (String s : iterable) {
            sb.append(s);
            sb.append(delimiter);
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    @Override
    protected void initCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException {
        String sql = parms.get(CMPARM_SQL);
        if (sql == null) {
            String className = parms.get(CMPARM_CLASSNAME);
            String where = parms.get(CMPARM_WHERE);

            if (className == null) {
                throw new InvalidParametersException(CMPARM_CLASSNAME, "You must provide 'sql' parameter or both 'className' with 'where'");
            }
            if (where == null) {
                throw new InvalidParametersException(CMPARM_WHERE, "You must provide 'sql' parameter or both 'className' with 'where'");
            }
        }
    }

    protected SearchSQL constructSearchSQL(ObjectStore objectStore, Map<CmParameter, String> parms) throws InvalidParametersException {
        SearchSQL result;
        String sql = parms.get(CMPARM_SQL);
        if (sql != null) {
            result = new SearchSQL(sql);
        } else {
            String className = parms.get(CMPARM_CLASSNAME);
            String where = parms.get(CMPARM_WHERE);

            result = new SearchSQL();
            result.setFromClauseInitialValue(className, null, true);

            result.setSelectList(join(",", getFieldSelectList(objectStore, parms)));
            result.setWhereClause(where);
        }
        return result;
    }

    protected abstract FetchType getFetchType();

    protected PageIterator getPageIterator(ObjectStore objectStore, SearchSQL sql, int pageSize) {
        SearchScope scope = new SearchScope(objectStore);
        EngineSet set = null;
        switch (getFetchType()) {
            case repositoryRows:
                set = scope.fetchRows(sql, pageSize, null, true);
                break;
            case engineObjects:
                set = scope.fetchObjects(sql, pageSize, null, true);
                break;
        }
        return set.pageIterator();
    }

    @Override
    protected void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception {

        long overallMillis = System.currentTimeMillis();
        long overallCount = 0;
        long overallProcessed = 0;

        int pageSize = Integer.parseInt(parms.get(CMPARM_PAGESIZE));

        SearchSQL sql = constructSearchSQL(objectStore, parms);


        SearchScope scope = new SearchScope(objectStore);


        printInfo("Executing SQL:\n" + sql);
        PageIterator pageIterator = getPageIterator(objectStore, sql, pageSize);

        int page = 0;
        while (pageIterator.nextPage()) {
            page++;
            printInfo(getBeforeProcessPageMessage(sql.toString(), overallCount, pageIterator.getElementCount(), page));
            UpdatingBatch batch = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.NO_REFRESH);

            int counter = 0;
            int processedCounter = 0;
            Object[] currentPage = pageIterator.getCurrentPage();
            for (Object obj : currentPage) {
                if (processObject(obj, batch, parms)) {
                    processedCounter++;
                }
                counter++;
            }
            printInfo(getBeforeProcessMessage(counter));

            long millis = System.currentTimeMillis();
            if (batch.hasPendingExecute()) {
                printInfo(getUpdateBatchMessage());
                batch.updateBatch();
            }
            overallCount += counter;
            overallProcessed += processedCounter;
            printInfo(getPostProcessVerb() + " " + summaryString(counter, processedCounter, millis) + " (overall: " + summaryString(overallCount, overallProcessed, overallMillis) + ")");

        }

        if (page == 0) {
            printInfo("No results found.");
        }

    }

    protected void printInfo(String info) {
        if(info !=null) {
            System.out.println(info);
        }
    }

    private static String summaryString(long counter, long processedCounter, long startMillis) {
        long duration = System.currentTimeMillis() - startMillis;
        double average = (double) counter * 1000 / duration;
        return processedCounter + " from " + counter + ", " + NUM_FORMAT.format(duration / 1000) + " sec, avg:" + NUM_FORMAT.format(average) + " pcs/sec";
    }

    protected abstract boolean processObject(Object ipo, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception;

    protected String getBeforeProcessPageMessage(String sql, long overallCount, int currentPageElementCount, int page) {
        return "Processing page " + page + " ( from " + ( overallCount + 1 ) + " to " + ( overallCount + currentPageElementCount + ") of SQL " + sql );
    }

    protected abstract String getBeforeProcessMessage(int counter);

    protected abstract String getUpdateBatchMessage();

    protected abstract String getPostProcessVerb();


    protected List<String> getFieldSelectList(ObjectStore objectStore, Map<CmParameter, String> mparms) {
        return Collections.singletonList(PropertyNames.ID);
    }
}

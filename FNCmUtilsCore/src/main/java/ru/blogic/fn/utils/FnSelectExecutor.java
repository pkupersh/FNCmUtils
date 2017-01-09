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
 * Abstract FileNet SELECT executor
 */
public abstract class FnSelectExecutor extends FnExecutor {
    protected static final CmParameter CMPARM_PAGESIZE = new CmParameter("pageSize", "P", true, "Size of chunks to select", false, "500");
    protected static final CmParameter CMPARM_CLASSNAME = new CmParameter("className", "c", true, "FileNet class to search for", false, null);
    protected static final CmParameter CMPARM_WHERE = new CmParameter("where", "w", true, "Where clause to filter object of given class", false, null);
    protected static final CmParameter CMPARM_SQL = new CmParameter("sql", "q", true, "A whole sql to execute. Use instead of combination of "+CMPARM_CLASSNAME+" and "+CMPARM_WHERE, false, null);

    protected static final NumberFormat NUM_FORMAT = new DecimalFormat("#.#");

    /**
     * Type of values that are fethed from FileNet
     */
    protected enum FetchType {
        repositoryRows,
        engineObjects
    }

    public FnSelectExecutor() {
        super();
    }

    @Override
    public List<CmParameter> getImmediateCmParameters() {
        return Arrays.asList(CMPARM_SQL, CMPARM_CLASSNAME, CMPARM_WHERE, CMPARM_PAGESIZE);
    }


    /**
     * Performs checking of paramters necessary for SELECT.
     * Must be called by descendants in case of overriding
     * @param parms map of parameter-value pairs
     * @throws InvalidParametersException
     */
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

    /**
     * Constructs SQL query by given parameters
     * @param objectStore Object store
     * @param parms Mmap of parameter-value pairs
     * @return SQL query
     */
    protected SearchSQL constructSearchSQL(ObjectStore objectStore, Map<CmParameter, String> parms) {
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

    /**
     * Descendants must define what kind of objects will be fetched.
     * @return
     */
    protected abstract FetchType getFetchType();


    /**
     * Performs processing of each fetched object.
     * @param object FileNet Engine Object or Reprostory Row, depending on {@link #getFetchType()}
     * @param batch Updating batch to add the object to (in case of any modification), makes sense only if {@link #getFetchType()} returns {@link ru.blogic.fn.utils.FnSelectExecutor.FetchType#repositoryRows }
     * @param parms Map of paramter-value pairs
     * @return True if object was "processed" false otherwise. Each "processed" object will be counted by separate counter. Usually it is intended for updated items, however each descendant class may decide that does it mean.
     * @throws Exception
     */
    protected abstract boolean processObject(Object object, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception;

    /**
     * A message that is displayed before each page of data is processed
     * @param sql SQL query that is currently executed
     * @param overallCount Overall items that are fetched at this moment (excluding current page)
     * @param currentPageElementCount Size of a page
     * @param page Page number
     * @return Descriptive message for current page processing
     */
    protected String getBeforeProcessPageMessage(String sql, long overallCount, int currentPageElementCount, int page) {
        return "Processing page " + page + " ( from " + ( overallCount + 1 ) + " to " + ( overallCount + currentPageElementCount + ") of SQL " + sql );
    }

    /**
     * A message that is displayed immediately before attempting of batch update (if it has any changes)
     * The main purpose is to display appropriate message that informs user about how many items to update
     * @param counter Number of items to update
     * @return Descriptive message
     */
    protected abstract String getBeforeProcessMessage(int counter);

    /**
     * A message that is displayed imemdiately before batch update start.
     * @return
     */
    protected abstract String getUpdateBatchMessage();

    /**
     * A verb that describes processing purpose (in past tense) E.g. "Processed", "Deleted", "Updated" etc.
     * @return
     */
    protected abstract String getPostProcessVerb();


    /**
     * Returns a list of fields to include in SELECT query. This method is NOT executed if {@link #CMPARM_SQL} is passed in parameters
     * @param objectStore An Object Store
     * @param parms Map if parameter-values pairs
     * @return A list of fields that will be included to SELECT statement.
     */
    protected List<String> getFieldSelectList(ObjectStore objectStore, Map<CmParameter, String> parms) {
        return Collections.singletonList(PropertyNames.ID);
    }


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


    private static String summaryString(long counter, long processedCounter, long startMillis) {
        long duration = System.currentTimeMillis() - startMillis;
        double average = (double) counter * 1000 / duration;
        return processedCounter + " from " + counter + ", " + NUM_FORMAT.format(duration / 1000) + " sec, avg:" + NUM_FORMAT.format(average) + " pcs/sec";
    }


    private static String join(String delimiter, Iterable<String> iterable) {
        StringBuilder sb = new StringBuilder();
        for (String s : iterable) {
            sb.append(s);
            sb.append(delimiter);
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
}

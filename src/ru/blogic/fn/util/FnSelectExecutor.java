package ru.blogic.fn.util;

import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.collection.PageIterator;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Domain;
import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.UpdatingBatch;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by pkupershteyn on 07.06.2016.
 */
public abstract class FnSelectExecutor extends FnExecutor {
    protected static CmParameter CMPARM_PAGESIZE = new CmParameter("pageSize", "P", true, "Size of chunks to delete", false, "500");
    protected static CmParameter CMPARM_CLASSNAME = new CmParameter("className", "c", true, "FileNet class to search for", false, null);
    protected static CmParameter CMPARM_WHERE = new CmParameter("where", "w", true, "Where clause to filter object of given class", false, null);
    protected static CmParameter CMPARM_SQL = new CmParameter("sql", "q", true, "A whole sql to execute", false, null);

    protected static final NumberFormat NUM_FORMAT = new DecimalFormat("#.#");


    public FnSelectExecutor() {
        super();
    }

    @Override
    public List<CmParameter> constructCmParameters() {
        return Arrays.asList(new CmParameter[]{CMPARM_SQL, CMPARM_CLASSNAME, CMPARM_WHERE, CMPARM_PAGESIZE});
    }

    private static String join(String delimiter, Iterable<String> iterable) {
        StringBuilder sb = new StringBuilder();
        for (String s : iterable) {
            sb.append(s);
            sb.append(delimiter);
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }


    protected SearchSQL constructSearchSQL(Map<CmParameter, String> parms) throws InvalidParametersException {
        SearchSQL result;
        String sql = parms.get(CMPARM_SQL);
        if (sql != null) {
            result = new SearchSQL(sql);
        } else {
            String className = parms.get(CMPARM_CLASSNAME);
            String where = parms.get(CMPARM_WHERE);

            if (className == null) {
                throw new InvalidParametersException(CMPARM_CLASSNAME, "You must provide 'sql' parameter or both 'className' with 'where'");
            }
            if (where == null) {
                throw new InvalidParametersException(CMPARM_WHERE, "You must provide 'sql' parameter or both 'className' with 'where'");
            }

            result = new SearchSQL();
            result.setFromClauseInitialValue(className, null, true);

            result.setSelectList(join(",", getFieldSelectList()));
            result.setWhereClause(where);
        }
        return result;
    }

    @Override
    protected void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception {

        long overallMillis = System.currentTimeMillis();
        long overallCount = 0;
        long overallProcessed = 0;

        int pageSize = Integer.parseInt(parms.get(CMPARM_PAGESIZE));

        SearchSQL sql = constructSearchSQL(parms);


        SearchScope scope = new SearchScope(objectStore);


        System.out.println("Executing SQL:\n" + sql);
        IndependentObjectSet independentObjectSet = scope.fetchObjects(sql, pageSize, null, true);
        PageIterator pageIterator = independentObjectSet.pageIterator();
        int page = 0;
        while (pageIterator.nextPage()) {
            page++;
            System.out.println("Processing page " + page + " ( from " + ( overallCount + 1 ) + " to " + ( overallCount + pageIterator.getElementCount() + ") of SQL " + sql ));
            UpdatingBatch batch = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.NO_REFRESH);

            int counter = 0;
            int processedCounter = 0;
            Object[] currentPage = pageIterator.getCurrentPage();
            for (Object obj : currentPage) {
                IndependentlyPersistableObject ipo = (IndependentlyPersistableObject) obj;
                if (processObject(ipo, batch, parms)) {
                    processedCounter++;
                }
                counter++;
            }
            System.out.println(getBeforeProcessMessage(counter));

            long millis = System.currentTimeMillis();
            if (batch.hasPendingExecute()) {
                System.out.println(getUpdateBatchMessage());
                batch.updateBatch();
            }
            overallCount += counter;
            overallProcessed += processedCounter;
            System.out.println(getPostProcessVerb() + " " + summaryString(counter, processedCounter, millis) + " (overall: " + summaryString(overallCount, overallProcessed, overallMillis) + ")");

        }
        if (page == 0) {
            System.out.println("NO results found.");
        }
    }

    private static String summaryString(long counter, long processedCounter, long startMillis) {
        long duration = System.currentTimeMillis() - startMillis;
        double average = (double) counter * 1000 / duration;
        return processedCounter + " from " + counter + ", " + NUM_FORMAT.format(duration / 1000) + " sec, avg:" + NUM_FORMAT.format(average) + " pcs/sec";
    }

    protected abstract boolean processObject(IndependentlyPersistableObject ipo, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception;

    protected abstract String getBeforeProcessMessage(int counter);

    protected abstract String getUpdateBatchMessage();

    protected abstract String getPostProcessVerb();


    protected List<String> getFieldSelectList() {
        return Arrays.asList(new String[]{PropertyNames.ID});
    }
}

package ru.blogic.fn.utils;

import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.ClassNames;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.UpdatingBatch;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import ru.blogic.fn.utils.annotations.Utility;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Utility("folderStructureDeleter")
public class FNFolderStuctureDeleter extends FnExecutor {

    static private final Options OPTIONS = new Options();
    protected static final CmParameter CMPARM_FOLDERSTRUCTURE = new CmParameter("folderStructure", "f", true, "Folder to delete (with substructure)", true, null);

    @Override
    public List<CmParameter> constructCmParameters() {
        return Arrays.asList(new CmParameter[]{CMPARM_FOLDERSTRUCTURE, FnSelectExecutor.CMPARM_PAGESIZE});
    }

    @Override
    protected void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception {

        String folderStructure = parms.get(CMPARM_FOLDERSTRUCTURE);


        int top_count = Integer.parseInt(parms.get(FnSelectExecutor.CMPARM_PAGESIZE));

        deleteFolderStructure(objectStore, folderStructure, domain, top_count);

    }

    private static final NumberFormat NUM_FORMAT = new DecimalFormat("#.#");

    private static String summaryString(long counter, long startMillis) {
        long duration = System.currentTimeMillis() - startMillis;
        double average = (double) counter * 1000 / duration;
        return counter + ", " + NUM_FORMAT.format(duration / 1000) + " sec, avg:" + NUM_FORMAT.format(average) + " pcs/sec";
    }


    private static void deleteFolderStructure(ObjectStore os, String folderStructure, Domain domain, int top_count) {
        //1 delete all documents


        SearchSQL sql = new SearchSQL();
        sql.setSelectList(PropertyNames.ID);
        sql.setFromClauseInitialValue(ClassNames.DOCUMENT, null, true);
        sql.setFolderRestriction(ClassNames.DOCUMENT, folderStructure);

        deleteSQL(sql, os, domain, top_count);


        //2 delete all custom objects
        sql = new SearchSQL();
        sql.setSelectList(PropertyNames.ID);
        sql.setFromClauseInitialValue(ClassNames.CUSTOM_OBJECT, null, true);
        sql.setFolderRestriction(ClassNames.CUSTOM_OBJECT, folderStructure);

        deleteSQL(sql, os, domain, top_count);


        //2 delete folders
        UpdatingBatch batch = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.NO_REFRESH);
        Folder folder = Factory.Folder.fetchInstance(os, folderStructure, null);

        int folders2Delete = processSubfolders(folder, batch, true);
        if (folders2Delete > 0) {
            System.out.println("Deleting " + folders2Delete + " folders...");
            batch.updateBatch();
        } else {
            System.out.println("Nothing to delete");
        }

    }

    private static int processSubfolders(Folder folder, UpdatingBatch batch, boolean parentFolder) {

        SearchSQL sql = new SearchSQL();
        sql.setSelectList(PropertyNames.ID + "," + PropertyNames.PATH_NAME);
        sql.setFromClauseInitialValue(ClassNames.FOLDER, null, true);
        sql.setFolderRestriction(ClassNames.FOLDER, folder.get_Id().toString());

        SearchScope scope = new SearchScope(folder.getObjectStore());
        IndependentObjectSet independentObjectSet = scope.fetchObjects(sql, null, null, false);
        Iterator itr = independentObjectSet.iterator();
        int deleteCounter = 0;
        while (itr.hasNext()) {
            Folder subFolder = (Folder) itr.next();
            deleteCounter = processSubfolders(subFolder, batch, false);
        }
        if (!parentFolder) {
            System.out.println("Folder " + folder.get_PathName() + " added to deletion batch");
            folder.delete();
            batch.add(folder, null);
            deleteCounter++;
        }
        return deleteCounter;
    }

    private static void deleteSQL(SearchSQL sql, ObjectStore os, Domain domain, int top_count) {

        SearchScope scope = new SearchScope(os);
        boolean allDeleted = false;
        long overallMillis = System.currentTimeMillis();
        long overallCount = 0;

        sql.setMaxRecords(top_count);

        while (!allDeleted) {

            System.out.println("Deleting according SQL:\n" + sql);
            IndependentObjectSet independentObjectSet = scope.fetchObjects(sql, null, null, false);


            UpdatingBatch batch = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.NO_REFRESH);

            int counter = 0;
            Iterator itr = independentObjectSet.iterator();
            while (itr.hasNext()) {
                IndependentlyPersistableObject ipo = (IndependentlyPersistableObject) itr.next();
                ipo.delete();
                batch.add(ipo, null);
                counter++;
            }
            System.out.println("Objects to delete: " + counter);
            allDeleted = counter < top_count;
            long millis = System.currentTimeMillis();
            System.out.println("Deleting...");
            if (counter > 0) {
                batch.updateBatch();
            }
            overallCount += counter;
            System.out.println("Deleted " + summaryString(counter, millis) + " (overall: " + summaryString(overallCount, overallMillis) + ")");

        }
    }
}

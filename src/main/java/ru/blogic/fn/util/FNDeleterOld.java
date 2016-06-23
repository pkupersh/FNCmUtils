package ru.blogic.fn.util;

import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.ClassNames;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.UpdatingBatch;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.ConfigurationParameters;
import com.filenet.api.util.UserContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Deprecated
public class FNDeleterOld {

    static private final Options OPTIONS = new Options();

    enum Arg {

        uri("U", true, "An url to connect to filenet"),
        user("u", true, "User name"),
        password("p", true, "Usr password"),
        objectStore("os", true, "An object store to connect to"),
        className("c", true, "FileNet class to search for", false, null),
        where("w", true, "Where clause to filter object of given class", false, null),
        pageSize("s", true, "Size of chunks to delete", false, "500"),
        folderStructure("f", true, "Folder to delete (with substructure)", false, null);

        private String shortName;
        private final boolean hasArgs;
        private final String descr;
        private final boolean mandatory;
        private final String defaultValue;

        Arg(String shortName, boolean hasArgs, String descr) {
            this(shortName, hasArgs, descr, true, null);
        }

        Arg(String shortName, boolean hasArgs, String descr, boolean mandatory, String defaultValue) {
            this.shortName = shortName;
            this.hasArgs = hasArgs;
            this.descr = descr;
            this.mandatory = mandatory;
            this.defaultValue = defaultValue;
        }

        public boolean isHasArgs() {
            return hasArgs;
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
            return new Option(shortName, name(), hasArgs, descr);
        }
    }

    static {

        for (Arg arg : Arg.values()) {
            OPTIONS.addOption(arg.toOption());
        }
    }

    static private final CommandLineParser PARSER = new DefaultParser();

    private static final NumberFormat NUM_FORMAT = new DecimalFormat("#.#");

    public static void main(String[] args) {
        try {

            Map<Arg, String> parms = checkParams(args);
            doDelete(parms);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<Arg, String> checkParams(String[] args) {
        CommandLine parms = null;
        Map<Arg, String> result = new HashMap<Arg,String>();
        try {
            parms = PARSER.parse(OPTIONS, args);
            for (Arg arg : Arg.values()) {
                if (arg.isMandatory() && !parms.hasOption(arg.name())) {
                    System.out.println("No parameter --" + arg.name() + " (-" + arg.getShortName() + ") specified");
                    help();
                }
                result.put(arg, parms.hasOption(arg.name()) ? parms.getOptionValue(arg.name()) : arg.getDefaultValue());
            }
        } catch (ParseException e) {
            help();
        }

        if (!( parms.hasOption(Arg.where.name()) && parms.hasOption(Arg.className.name()) ) || !parms.hasOption(Arg.folderStructure.name())) {
            System.out.println("You must supply folderStructure or className + where parameters");
        }

        return result;
    }

    private static void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(FNDeleterOld.class.getSimpleName(), OPTIONS);
        System.exit(0);
    }


    private static void doDelete(Map<Arg, String> parms) {

        String uri = parms.get(Arg.uri);
        String username = parms.get(Arg.user);
        String password = parms.get(Arg.password);


        String OS = parms.get(Arg.objectStore);
        String class2Delete = parms.get(Arg.className);
        String where2Delete = parms.get(Arg.where);
        String folderStructure = parms.get(Arg.folderStructure);


        int top_count = Integer.parseInt(parms.get(Arg.pageSize));

        // Get the connection
        ConfigurationParameters parameters = new ConfigurationParameters();

        Connection conn = Factory.Connection.getConnection(uri, parameters);

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


            if (folderStructure == null) {

                SearchSQL sql = new SearchSQL();
                sql.setFromClauseInitialValue(class2Delete, null, true);
                sql.setSelectList("ID");
                sql.setWhereClause(where2Delete);
                //sql.setMaxRecords(top_count);

                System.out.println("SQL to delete:\n" + sql);

                String confirm = System.console().readLine("Please confirm deletion (press 'y')");
                if (!confirm.equals("y")) {
                    return;
                }

                deleteSQL(sql, os, domain, top_count);
            } else {
                deleteFolderStructure(os, folderStructure, domain, top_count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Pop the subject off the UserContext stack
            uc.popSubject();
        }

    }

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

        int folders2Delete=processSubfolders(folder, batch, true);
        if(folders2Delete>0) {
            System.out.println("Deleting "+folders2Delete+" folders...");
            batch.updateBatch();
        }else{
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
        int deleteCounter=0;
        while (itr.hasNext()) {
            Folder subFolder = (Folder) itr.next();
            deleteCounter=processSubfolders(subFolder, batch, false);
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
            if(counter>0) {
                batch.updateBatch();
            }
            overallCount += counter;
            System.out.println("Deleted " + summaryString(counter, millis) + " (overall: " + summaryString(overallCount, overallMillis) + ")");

        }
    }
}

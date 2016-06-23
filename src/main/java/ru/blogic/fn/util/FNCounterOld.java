package ru.blogic.fn.util;

import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.ConfigurationParameters;
import com.filenet.api.util.UserContext;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

/**
 * Created by pkupershteyn on 03.06.2016.
 */
@Deprecated
public class FNCounterOld {
    private static final NumberFormat NUM_FORMAT = new DecimalFormat("#.#");

    public static void main(String[] args) {

        // Set the constants
        // Use /FileNet/Engine for EJB
        //String uri = "iiop://aeroflot-dev.it.main.java.ru:9810/FileNet/Engine";
        //String uri = "http://172.19.68.37:9080/wsi/FNCEWS40MTOM/";
        //String uri = "http://aeroflot-dev.it.ru:9080/wsi/FNCEWS40MTOM/";
        String uri = "http://172.19.68.252:9080/wsi/FNCEWS40MTOM/";
        String username = "gcd_admin";
        String password = "o9p0[-]=";


        String OS = "OS2";
        String class2count = "LbDfwImportDominoIndexDocument";//"LbLogEntry";
        String where2count = "IsCurrentVersion=true";//"LbLogJobId = 'SubstitutionsImport#2016-05-20T16:01:35.574' AND LbLogLevel<>'success'";//"This INSUBFOLDER '/index results'";
        int pageSize = 5000;


        // Get the connection
        ConfigurationParameters parameters = new ConfigurationParameters();
        //parameters.setParameter(ConfigurationParameter.CONNECTION_PARTICIPATES_IN_TRANSACTION, Boolean.TRUE);

        Connection conn = Factory.Connection.getConnection(uri, parameters);

        // Get the user context
        UserContext uc = UserContext.get();
        // Build the subject using the FileNetP8 stanza
        // Use FileNetP8 for the EJB transport (also the default)
        //System.setProperty("com.ibm.CORBA.securityServerHost","aeroflot-dev.it.main.java.ru");
        uc.pushSubject(
                //UserContext.createSubject(conn, username, password, "FileNetP8")
                UserContext.createSubject(conn, username, password, "FileNetP8WSI")
        );
        try {
            // Get the default domain
            Domain domain = Factory.Domain.getInstance(conn, null);
            // Get an object store
            ObjectStore os = Factory.ObjectStore.fetchInstance(domain,
                    OS, null);

            System.out.println("OS name '" + os.get_Name() + "' (id: '" + os.get_Id().toString() + "') descriptive text: " + os.get_DescriptiveText());


            SearchSQL sql = new SearchSQL();
            sql.setFromClauseInitialValue(class2count, null, true);
            sql.setSelectList("ID");
            sql.setWhereClause(where2count);

            System.out.println("SQL to count: "+sql);

            SearchScope scope = new SearchScope(os);
            IndependentObjectSet independentObjectSet = scope.fetchObjects(sql, pageSize, null, true);
            int counter = 0;
            Iterator itr = independentObjectSet.iterator();
            while (itr.hasNext()) {
                itr.next();
                counter++;
            }
            System.out.println("Found records: " + counter + "");


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Pop the subject off the UserContext stack
            uc.popSubject();
        }

    }
}

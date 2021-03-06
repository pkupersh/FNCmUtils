package ru.blogic.fn.utils;

import com.filenet.api.admin.AddOn;
import com.filenet.api.collection.AddOnSet;
import com.filenet.api.core.Domain;
import com.filenet.api.core.ObjectStore;
import ru.blogic.fn.utils.annotations.Utility;

import java.util.*;

/**
 * Created by sam158 on 09.12.2016.
 * Search of string in addon's content and return addon's name
 */
@Utility("searchInAddons")
public class FnSearchInAddons extends FnExecutor {

    protected static final CmParameter CMPARM_SEARCHTEMPLATE = new CmParameter("searchTemplate", "T", true, "Search pattern. Regex", true, null);

    @Override
    public List<CmParameter> getAppliedCmParameters() {
        return Arrays.asList(CMPARM_SEARCHTEMPLATE);
    }

    @Override
    protected void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception {

        AddOnSet addons = domain.get_AddOns();
        Iterator<AddOn> it = addons.iterator();
        Map<String, String> addonNames = new HashMap<String, String>();
        while (it.hasNext()) {
            AddOn addOn = it.next();
            Scanner s = new Scanner(addOn.accessImportDataStream());
            String nextMatch = s.findWithinHorizon(parms.get(CMPARM_SEARCHTEMPLATE), 0);
            if (nextMatch != null) {
                addonNames.put(addOn.get_DisplayName(), nextMatch);
            }
        }
        if (addonNames.size() > 0) {
            printInfo("The following matches are found:");

            for (Map.Entry<String,String> entry: addonNames.entrySet()){
                printInfo("addon: " + entry.getKey() + "   math: " + entry.getValue());
            }
        }else{
            printInfo("No matches found. ");
        }
    }

    @Override
    protected void checkCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException {
        //todo
    }

    @Override
    public String getExecutorDescription() {
        return "Utility to search addons existing in FileNet domain by regular expression pattern.";
    }
}

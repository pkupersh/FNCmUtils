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

    protected static final CmParameter CMPARM_SEARCHTEMPALTE = new CmParameter("searchTemplate", "T", true, "Search pattern. Regex", true, null);

    @Override
    public List<CmParameter> constructCmParameters() {
        return Arrays.asList(new CmParameter[]{CMPARM_SEARCHTEMPALTE});
    }

    @Override
    protected void doFnWork(Domain domain, ObjectStore objectStore, Map<CmParameter, String> parms) throws Exception {

        AddOnSet addons = domain.get_AddOns();
        Iterator<AddOn> it = addons.iterator();
        Map<String, String> addonNames = new HashMap<String, String>();
        while (it.hasNext()) {
            AddOn addOn = it.next();
            Scanner s = new Scanner(addOn.accessImportDataStream());
            String nextMatch = s.findWithinHorizon(parms.get(CMPARM_SEARCHTEMPALTE), 0);
            if (nextMatch != null) {
                addonNames.put(addOn.get_DisplayName(), nextMatch);
            }
        }
        if (addonNames.size() > 0) {
            System.out.println("The following matches are found:");

            for (Map.Entry<String,String> entry: addonNames.entrySet()){
                System.out.println("addon: "+entry.getKey()+ "   math: "+entry.getValue());
            }
        }else{
            System.out.println("No matches found. ");
        }
    }
}

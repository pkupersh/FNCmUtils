package ru.blogic.fn.utils;

import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.core.UpdatingBatch;
import ru.blogic.fn.utils.annotations.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pkupershteyn on 07.06.2016.
 */

@Utility("bulkUpdater")
public class FnBulkUpdater extends FnSelectExecutor {

    protected static final CmParameter PARM_FIELDVALUES = new CmParameter("fieldValues", "v", true, "Field values to update, 'field1=value,filed2=value ...'", true, null);

    private Map<String, Object> paramValues = new HashMap<String, Object>();


    public static void main(String[] args) {
        new FnBulkUpdater().execute(args);
    }

    public FnBulkUpdater() {
        super();
    }

    @Override
    public List<CmParameter> constructCmParameters() {
        List<CmParameter> params=new ArrayList<CmParameter>();
        params.addAll(super.constructCmParameters());
        params.add(PARM_FIELDVALUES);

        return params;
    }

    @Override
    protected void initCmParameterValues(Map<CmParameter, String> parms) throws InvalidParametersException {
        String[] fldPairs = parms.get(PARM_FIELDVALUES).split(",");
        for (String fldPair : fldPairs) {
            String[] fldPairArr = fldPair.trim().split("=");
            if (fldPairArr.length != 2) {
                throw new InvalidParametersException(PARM_FIELDVALUES, "invalid field-value pair :" + fldPair);
            }
            String fldName = fldPairArr[0];
            String fldValueStr = fldPairArr[1];

            String[] fldValueStrArr = fldValueStr.split(":");
            if (fldValueStrArr.length != 2) {
                throw new InvalidParametersException(PARM_FIELDVALUES, "invalid field value :" + fldValueStr + ", shold be '[int|string|boolean|double]:<value>|null'");
            }
            String fldType = fldValueStrArr[0].toLowerCase();
            String fldValue = fldValueStrArr[1];
            if ("null".equals(fldValue)) {
                fldValue = null;
            }
            Object fldValueObj;
            if ("string".equals(fldType)) {
                fldValueObj = fldValue;
            } else if ("int".equals(fldType)) {
                try {
                    fldValueObj = fldValue != null ? Integer.parseInt(fldValue) : null;
                } catch (NumberFormatException e) {
                    throw new InvalidParametersException(PARM_FIELDVALUES, "invalid field value :" + fldValueStr + ", expected integer");
                }
            } else if ("boolean".equals(fldType)) {
                fldValueObj = fldValue != null ? Boolean.parseBoolean(fldValue) : null;
            } else if ("double".equals(fldType)) {
                try {
                    fldValueObj = fldValue != null ? Double.parseDouble(fldValue) : null;
                } catch (NumberFormatException e) {
                    throw new InvalidParametersException(PARM_FIELDVALUES, "invalid field value :" + fldValueStr + ", expexted double");
                }
            } else {
                throw new InvalidParametersException(PARM_FIELDVALUES, "invalid field type for '" + fldValueStr + "', shold be one from: int|string|boolean|double");
            }

            paramValues.put(fldName, fldValueObj);
        }
    }

    @Override
    protected boolean processObject(IndependentlyPersistableObject ipo, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception {
        boolean hasChanges = false;
        for (Map.Entry<String, Object> entry : paramValues.entrySet()) {
            Object curValue = ipo.getProperties().getObjectValue(entry.getKey());
            Object newValue = entry.getValue();
            if (objectsDiffer(curValue, newValue)) {
                ipo.getProperties().putObjectValue(entry.getKey(), entry.getValue());
                hasChanges = true;
            }
        }
        if (hasChanges) {
            batch.add(ipo, null);
        }
        return hasChanges;
    }

    private static boolean objectsDiffer(Object object1, Object object2) {
        return ( object1 != null && !object1.equals(object2) ) || ( object2 != null && !object2.equals(object1) );
    }


    @Override
    protected String getBeforeProcessMessage(int counter) {
        return "Processing...";
    }

    @Override
    protected String getUpdateBatchMessage() {
        return "Updating batch...";
    }

    @Override
    protected String getPostProcessVerb() {
        return "Processed";
    }

    @Override
    protected List<String> getFieldSelectList() {
        List<String> flds = new ArrayList<String>();
        flds.addAll(super.getFieldSelectList());
        flds.addAll(paramValues.keySet());
        return flds;
    }
}

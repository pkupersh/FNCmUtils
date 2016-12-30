package ru.blogic.fn.utils;

import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.core.UpdatingBatch;
import ru.blogic.fn.utils.annotations.Utility;

import java.util.Map;

/**
 * Created by pkupershteyn on 07.06.2016.
 */
@Utility("counter")
public class FnCounter extends FnSelectExecutor {


    @Override
    protected FetchType getFetchType() {
        return FetchType.repositoryRows;
    }

    @Override
    protected boolean processObject(Object ipo, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception {
        return true;
    }

    @Override
    protected String getBeforeProcessMessage(int counter) {
        return "";
    }

    @Override
    protected String getUpdateBatchMessage() {
        return "";
    }

    @Override
    protected String getPostProcessVerb() {
        return "Counted";
    }
}

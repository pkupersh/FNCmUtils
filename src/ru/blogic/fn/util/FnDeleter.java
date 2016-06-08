package ru.blogic.fn.util;

import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.core.UpdatingBatch;

import java.util.Map;

/**
 * Created by pkupershteyn on 07.06.2016.
 */
public class FnDeleter extends FnSelectExecutor {

    public FnDeleter(){
        super();
    }

    public static void main(String[] args){
        new FnDeleter().execute(args);
    }

    @Override
    protected boolean processObject(IndependentlyPersistableObject ipo, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception {
        ipo.delete();
        batch.add(ipo,null);
        return true;
    }

    @Override
    protected String getBeforeProcessMessage(int counter) {
        return counter!=0 ? "Deleting "+counter+" objects..." : "No objects to delete";
    }

    @Override
    protected String getUpdateBatchMessage() {
        return "Performing batch deletion...";
    }

    @Override
    protected String getPostProcessVerb() {
        return "Deleted";
    }
}

package ru.blogic.fn.utils;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.UpdatingBatch;
import ru.blogic.fn.utils.annotations.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.nio.file.Files;
/**
 * Created by smasalykin on 17.02.2017.
 * Upload files to FileNet documents
 */
@Utility("uploader")
public class FnUploadToDocument  extends FnSelectExecutor {


    protected static final CmParameter CMPARM_LIST_OF_FILES = new CmParameter("files", "FS", true, "List of path to files for upload");


    public FnUploadToDocument() {
        super();

    }


    @Override
    protected FetchType getFetchType() {
        return FetchType.engineObjects;
    }

    @Override
    public List<CmParameter> getImmediateCmParameters() {
        List<CmParameter> parameters = new ArrayList<CmParameter>(super.getImmediateCmParameters());
        parameters.add(CMPARM_LIST_OF_FILES);
        return parameters;
    }

    @Override
    protected boolean processObject(Object object, UpdatingBatch batch, Map<CmParameter, String> parms) throws Exception {
        //Get/prepare list of files
        Document doc = (Document)object;
        doc.checkout(ReservationType.EXCLUSIVE, null, null, null);
        doc.save(RefreshMode.REFRESH);
        Document res = (Document)doc.get_Reservation();
        ContentElementList contentList =  Factory.ContentElement.createList();
        for(String fileName: parms.get(CMPARM_LIST_OF_FILES).split(",|\\s")){
            FileInputStream  fileInputStream = new FileInputStream(new File(fileName.trim()));
            ContentTransfer element = Factory.ContentTransfer.createInstance();
            element.setCaptureSource(fileInputStream);
            element.set_RetrievalName(fileName);
            contentList.add(element);
        }
        res.set_ContentElements(contentList);
        res.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
        batch.add(res,null);
        return true;
    }

    @Override
    protected String getBeforeProcessMessage(int counter) {
        return counter!=0 ? "Upload  content to "+counter+" objects..." : "No objects for upload content";
    }

    @Override
    protected String getUpdateBatchMessage() {
        return "Performing batch content upload...";
    }

    @Override
    protected String getPostProcessVerb() {
        return "Content was uploaded";
    }
}


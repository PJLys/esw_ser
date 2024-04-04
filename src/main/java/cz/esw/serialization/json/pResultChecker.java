package cz.esw.serialization.json;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.handler.ProtoDataHandler;
import cz.esw.serialization.proto.*;

public class pResultChecker extends ProtoDataHandler implements ResultConsumer {
    private static final double DEFAULT_DELTA = 0.000001;
    private pDataset currentDataset = null;
    public pResultChecker(){super(null,null);}
    public void getResults(ResultConsumer consumer){throw new UnsupportedOperationException();}
    @Override
    public void acceptMeasurementInfo(int resultId, long timestamp, String measurerName) {
        checkCurrentDataset();
    }

    @Override
    public void acceptResult(DataType type, double result) {

    }

    private void checkCurrentDataset() {
        if (currentDataset != null && !currentDataset.getRecordsList().isEmpty()) {
            throw new IllegalStateException("Results for previous dataset not complete. Missing data types:  " + currentDataset.getRecordsList());
        }
    }
}

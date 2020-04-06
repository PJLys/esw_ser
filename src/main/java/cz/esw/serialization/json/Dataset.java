package cz.esw.serialization.json;

import java.util.List;
import java.util.Map;

/**
 * @author Marek Cuchý
 */
public class Dataset {

    private MeasurementInfo info;
    private Map<DataType, List<Double>> records;

    public Dataset() {
    }

    public MeasurementInfo getInfo() {
        return info;
    }

    public void setInfo(MeasurementInfo info) {
        this.info = info;
    }

    public Map<DataType, List<Double>> getRecords() {
        return records;
    }

    public void setRecords(Map<DataType, List<Double>> records) {
        this.records = records;
    }
}

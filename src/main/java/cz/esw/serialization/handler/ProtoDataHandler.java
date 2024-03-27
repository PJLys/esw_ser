package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import cz.esw.serialization.proto.*;
/**
 * @author Marek Cuchý (CVUT)
 */
public class ProtoDataHandler implements DataHandler {
	private final InputStream is;
	private final OutputStream os;
	protected Map<Integer, pDataset> datasets;


	public ProtoDataHandler(InputStream is, OutputStream os) {
		this.is = is; this.os = os;
	}

	@Override
	public void initialize() {
		this.datasets = new HashMap<>();
	}

	@Override
	public void handleNewDataset(int datasetId, long timestamp, String measurerName) {

		pMeasurementInfo pinfo = pMeasurementInfo.newBuilder()
				.setId(datasetId)
				.setTimestamp(timestamp)
				.setMeasurerName(measurerName)
				.build();

		pDataset pdataset = pDataset.newBuilder()
				.setInfo(pinfo)
				.build();

		this.datasets.put(datasetId, pdataset);
	}

	@Override
	public void handleValue(int datasetId, DataType type, double value) {
		pDataset pdataset = datasets.get(datasetId);

		if (pdataset == null) {
			throw new IllegalArgumentException("Dataset with id " + datasetId + " not initialized.");
		}

		pDataset.pRecord newrecord = null;
		// If there are records
		if (pdataset.getRecordsCount()>0) {
			// Update value
			for (pDataset.pRecord.Builder rb : pdataset.toBuilder().getRecordsBuilderList()) {
				if (rb.getDataTypeValue() == type.ordinal()) {
					rb.addValues(value);

					newrecord = rb.build();

					break;
				}
			}
		}

		// If no records found, or records are empty, create new one
        if (newrecord == null) {
			newrecord = pDataset.pRecord.newBuilder()
					.addValues(value)
					.build();
		}

		// Update the dataset
		pDataset newdataset = pdataset.toBuilder()
				.setRecords(type.ordinal(), newrecord)
				.build();

		// Add the new dataset to the list
		this.datasets.put(datasetId, newdataset);
	}

	@Override
	public void getResults(ResultConsumer consumer) {

	}
}

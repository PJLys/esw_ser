package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;

import java.io.*;
import java.util.*;

import cz.esw.serialization.proto.*;
import org.apache.avro.JsonProperties;
import org.apache.commons.lang3.NotImplementedException;

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
	public void getResults(ResultConsumer consumer) throws IOException {
		// Write datasets to os
		for (pDataset ds : datasets.values()) {
			os.write(ds.getSerializedSize()); // For C-based frameworks
			ds.writeTo(os);
			os.flush();
		}

		// Receive results on is and store in array
		List<pResult> results = new ArrayList<>();
		while (true) {
			pResult res = pResult.parseDelimitedFrom(is);
			if (res== null) break;
			results.add(res);
		}

		for (pResult res : results)
			System.out.println(res.toString());

	}
}

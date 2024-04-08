package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;

import java.io.*;
import java.rmi.UnexpectedException;
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

		List<pDataset.pRecord> recordList = new ArrayList<>();

		for (int i=0; i<3; i++){
			recordList.add(pDataset.pRecord.newBuilder()
					.setDataTypeValue(i)
					.build()
			);
		}

		pDataset pdataset = pDataset.newBuilder()
				.setInfo(pinfo)
				.addAllRecords(recordList)
				.build();

		this.datasets.put(datasetId, pdataset);
	}

	/**
	 * Adds a value to the specified dataset for a specific record type
	 * @param datasetId id of the dataset to which the value belongs
	 * @param type      type of the record: 0=DL, 1=UL, 2=PING
	 * @param value     float to be added in the right field
	 */
	@Override
	public void handleValue(int datasetId, DataType type, double value) {
		pDataset dataset = this.datasets.get(datasetId);

		if (dataset==null) {
			throw new IllegalArgumentException("There's no such ID: "+datasetId);
		}

		for (pDataset.pRecord.Builder rb : dataset.toBuilder().getRecordsBuilderList()) {
			if (rb.getDataTypeValue() == type.ordinal()) {
				this.datasets.put(datasetId,
						dataset.toBuilder()
								.setRecords(type.ordinal(), rb.addValues(value))
								.build());
				return;
			}
		}

		System.out.println("This line shouldn't be printed");
	}





	@Override
	public void getResults(ResultConsumer consumer) throws IOException {
		// Write datasets to os
			for (pDataset ds : datasets.values()) {
				System.out.println("Sending dataset: " + ds);
				//os.write(ds.getSerializedSize()); // For C-based frameworks
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

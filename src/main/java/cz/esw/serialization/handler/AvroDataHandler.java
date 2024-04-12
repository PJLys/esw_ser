package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;

import java.io.*;
import java.net.SocketException;
import java.rmi.UnexpectedException;
import java.util.*;

import cz.esw.serialization.avro.*;
import org.apache.avro.JsonProperties;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.lang3.NotImplementedException;

/**
 * @author Marek Cuchý (CVUT)
 */
public class AvroDataHandler implements DataHandler {
	private final InputStream is;
	private final OutputStream os;
	protected Map<Integer, ADataset> datasets;

	public AvroDataHandler(InputStream is, OutputStream os) {
		this.is = is; this.os = os;
	}

	@Override
	public void initialize() {
		this.datasets = new HashMap<>();
	}

	@Override
	public void handleNewDataset(int datasetId, long timestamp, String measurerName) {

		AMeasurementInfo info = AMeasurementInfo.newBuilder()
				.setId(datasetId)
				.setTimestamp(timestamp)
				.setMeasurerName(measurerName)
				.build();

		List<ARecord> recordList = new ArrayList<>();

		for (ADataType dt: ADataType.values()){
			recordList.add(ARecord.newBuilder()
					.setDataType(dt)
					.build()
			);
		}

		ADataset dataset = ADataset.newBuilder()
				.setInfo(info)
				.setRecords(recordList)
				.build();

		this.datasets.put(datasetId, dataset);
	}

	/**
	 * Adds a value to the specified dataset for a specific record type
	 * @param datasetId id of the dataset to which the value belongs
	 * @param type      type of the record: 0=DL, 1=UL, 2=PING
	 * @param value     float to be added in the right field
	 */
	@Override
	public void handleValue(int datasetId, DataType type, double value) {
		ADataset dataset = this.datasets.get(datasetId);

		if (dataset==null) {
			throw new IllegalArgumentException("There's no such ID: "+datasetId);
		}

		List<ARecord> newRecordList = new ArrayList<>();


		for (ARecord record : dataset.getRecords()) {
			if (record.getDataType().name().equals(type.name())) {

				List<Double> newValues = record.getValues();
				newValues.add(value);

				ARecord.Builder newRecordBuilder = ARecord.newBuilder()
						.setDataType(record.getDataType())
						.setValues(newValues);

				newRecordList.add(newRecordBuilder.build());
			} else {
				newRecordList.add(record);
			}
		}

		ADataset.Builder dsBuilder = ADataset.newBuilder();
		ADataset newds = dsBuilder.setRecords(newRecordList)
				.setInfo(dataset.getInfo())
				.build();

		if (datasets.replace(datasetId, newds)==null)
			System.out.println("Something went seriously wrong updating the records");
	}

	@Override
	public void getResults(ResultConsumer consumer) throws IOException {
		DatumWriter<ADataset> datasetDatumWriter = new SpecificDatumWriter<ADataset>(ADataset.class);
		DataFileWriter<ADataset> dataFileWriter = new DataFileWriter<ADataset>(datasetDatumWriter);
		boolean first = true;
		try {
			// Write datasets to os
			for (ADataset ds : datasets.values()) {
				if (first) {
					// Instantiate datafilewriter
					dataFileWriter.create(ds.getSchema(), os);
					first = false;
				}
				dataFileWriter.append(ds);
			}
			System.out.println("Sent " + datasets.size() + " datasets!");
		} catch (SocketException se){
			System.out.println(se);
			return;
		}

		dataFileWriter.close();

		// Receive results on is and store in array
		DatumReader<AResult> datumReader = new SpecificDatumReader<>(AResult.class);
		DataFileReader<AResult> dataFileReader = new DataFileReader<>((SeekableInput) is, datumReader);
        while (dataFileReader.hasNext()) {
            AResult result = dataFileReader.next();
            for (AAverage average : result.getAverages()){
                DataType t;
                switch (average.getDataType()) {
                    case DOWNLOAD -> t = DataType.DOWNLOAD;
                    case UPLOAD -> t = DataType.UPLOAD;
                    case PING -> t = DataType.PING;
                    default -> throw new UnexpectedException("Detected other datatype then expected in reply!");
                }
                consumer.acceptResult(t, average.getValue());
            }
        }
    }
}

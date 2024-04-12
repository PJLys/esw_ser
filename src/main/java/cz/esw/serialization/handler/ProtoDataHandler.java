package cz.esw.serialization.handler;

import cz.esw.serialization.ResultConsumer;
import cz.esw.serialization.json.DataType;

import java.io.*;
import java.net.SocketException;
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
		try {
			// Write datasets to os
			for (pDataset ds : datasets.values()) {
				int size = ds.getSerializedSize();
				os.write(intToByteArray(size));
				os.flush();
				ds.writeTo(os);
				os.flush();
			}
			System.out.println("Sent " + datasets.size() + " datasets!");
		} catch (SocketException se){
			System.out.println(se);
			return;
		}

		// Receive results on is and store in array

		for (int i = 0; i<datasets.size(); i++) {
			try {
				int size = extractSize(is);
				byte[] rec = is.readNBytes(size);
				pResult result = pResult.parseFrom(rec);
				pMeasurementInfo info = result.getInfo();
				consumer.acceptMeasurementInfo(info.getId(), info.getTimestamp(), info.getMeasurerName());
				List<pResult.pAverage> averageList = result.getAveragesList();
				for (pResult.pAverage average : averageList) {
					DataType t;
					switch (average.getDataType()) {
						case DOWNLOAD -> t = DataType.DOWNLOAD;
						case UPLOAD -> t=DataType.UPLOAD;
						case PING -> t=DataType.PING;
						default -> throw new UnexpectedException("Detected other datatype then expected in reply!");
					}
					consumer.acceptResult(t, average.getValue());
				}
			} catch (SocketException e) {
				System.out.println(e);
			} catch (EOFException e) {
				break;
			}
		}
		os.close();
	}

	private int extractSize(InputStream is) throws IOException {
		byte[] sizeBytes = new byte[4]; // Assuming the size is represented as a 4-byte integer
		int bytesRead = is.readNBytes(sizeBytes,0, sizeBytes.length);
		return switch (bytesRead) {
            case -1 -> throw new EOFException("End of the results!");
            case 4 -> byteArrayToInt(sizeBytes);
            default -> throw new IOException("Failed to read the size bytes");
        };
	}

	private byte[] intToByteArray(int value) {
		return new byte[] {
				(byte)(value >> 24),
				(byte)(value >> 16),
				(byte)(value >> 8),
				(byte)value
		};
	}

	private int byteArrayToInt(byte[] bytes) {
		return ((bytes[0] & 0xFF) << 24) |
				((bytes[1] & 0xFF) << 16) |
				((bytes[2] & 0xFF) << 8) |
				(bytes[3] & 0xFF);
	}
}

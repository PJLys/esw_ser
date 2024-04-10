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

	public void printSerializedDataSizes() throws IOException, ClassNotFoundException {
		// Open the "output.txt" file for reading
		FileInputStream fis = new FileInputStream("output.txt");
		ObjectInputStream ois = new ObjectInputStream(fis);

		try {
			while (true) {
				// Read the size of the serialized data from the file
				byte[] sizeBytes = new byte[4];
				ois.readFully(sizeBytes);
				int size = byteArrayToInt(sizeBytes);
				// Print the size
				System.out.println("Size of serialized data: " + size);

				// Read and discard the serialized data
				byte[] data = new byte[size];
				ois.readFully(data); // Read the serialized data and discard it
			}
		} catch (IOException e) {
			// End of file reached
			System.out.println("End of file reached");
		} finally {
			// Close the input stream
			ois.close();
		}
	}


	@Override
	public void getResults(ResultConsumer consumer) throws IOException {
		FileOutputStream fos = new FileOutputStream("output.txt");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		try {
			// Write datasets to os
			for (pDataset ds : datasets.values()) {
				int size = ds.getSerializedSize();
				System.out.println("ds.getSerializedSize= " + size);
				os.write(intToByteArray(size));
				oos.write(intToByteArray(size));
				os.flush();
				oos.flush();
				ds.writeTo(os);
				ds.writeTo(oos);
				os.flush();
				oos.flush();
				//System.out.println(ds);
			}
			System.out.println("Sent data");
		} finally {
			oos.close();
			fos.close();
		}

		try {
			printSerializedDataSizes();
		} catch (ClassNotFoundException e) {
			System.out.println(e);
		}

		// Receive results on is and store in array
		List<pResult> results = new ArrayList<>();
		System.out.println("Receiving results!");
		while (true) {
			try {
				int size = extractSize(is);
				byte[] rec = is.readNBytes(size);
				pResult res = pResult.parseFrom(rec);
				if (res.toString().isEmpty()) break;
				System.out.println("Received " + res);
				results.add(res);
			}
			catch (SocketException e) {
				System.out.println(e);
			}
		}

		System.out.println("Hi");

		for (pResult res : results)
			System.out.println(res.toString());
	}

	private int extractSize(InputStream is) throws IOException {
		byte[] sizeBytes = new byte[4]; // Assuming the size is represented as a 4-byte integer
		if (is.read(sizeBytes) != 4) {
			throw new IOException("Failed to read size bytes");
		}
		return byteArrayToInt(sizeBytes);
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

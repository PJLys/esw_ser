package cz.esw.serialization;

import java.io.IOException;

/**
 * @author Marek Cuchý (CVUT)
 */
public class AppTest {

	public static void main(String[] args) throws IOException {
		new App(0, 3, 10).run("localhost", 12346, ProtocolType.PROTO	, 10);
	}
}

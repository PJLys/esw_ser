package cz.esw.serialization;

import java.io.IOException;

/**
 * @author Marek Cuchý (CVUT)
 */
public class AppTest {

	public static void main(String[] args) throws IOException {
		new App(0, 100, 10000).run("localhost", 12345, ProtocolType.PROTO, 10);
	}
}

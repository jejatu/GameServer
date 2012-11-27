package gameserver;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Client extends Protocol {
	public void connect(InetSocketAddress serverAddress) {
		// connect to the given server
		Connection serverConnection = new Connection(serverAddress);
		serverConnection.hasFlowControl = false;
		connections.add(serverConnection);
		queue(serverConnection, "init");
	}
	
	protected void respond(Connection connection, ByteBuffer buffer) {
		byte[] dataBytes = new byte[buffer.remaining()];
		buffer.get(dataBytes);
		String data = new String(dataBytes).trim();
		if (data.equals("hello")) {
			queue(connection, "hi");
		} else {
			queue(connection, "hello");
		}
	}
}

package gameserver;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Server extends Protocol {
	protected void handleConnection(InetSocketAddress address, ByteBuffer buffer) {
		// add a new connection
		connections.add(new Connection(address));
		handlePacket(connections.get(connections.size() - 1), buffer);
	}
	
	protected void respond(Connection connection, ByteBuffer buffer) {
		byte[] dataBytes = new byte[buffer.remaining()];
		buffer.get(dataBytes);
		String data = new String(dataBytes).trim();
		if (data.equals("hello")) {
			queue(connection, "hello");
		} else {
			queue(connection, "good morning");
		}
	}
}

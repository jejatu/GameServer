package gameserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
	private static final int BUFFER_SIZE = 256;
	private int protocolId = 0;
	private DatagramChannel channel;
	private List<Client> clients = new ArrayList<Client>();

	public int timeout = 10000;
	
	public void start(int id, int port) throws IOException {
		protocolId = id;
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(port));
	}
	
	public void end() throws IOException {
		channel.disconnect();
		channel.close();
	}
	
	public void tick() throws IOException {
		listen();
		send();
		checkConnections();
	}
	
	private void handlePacket(InetSocketAddress address, ByteBuffer buffer) {
		for (Client client : clients) {
			if (address.getHostName().equals(client.address.getHostName()) &&
					address.getPort() == client.address.getPort()) {
				handleClient(client, buffer);
				return;
			}
		}
		acceptConnection(address, buffer);
	}
	
	private void handleClient(Client client, ByteBuffer buffer) {
		client.lastPacketTime = System.currentTimeMillis();
	}
	
	private void acceptConnection(InetSocketAddress address, ByteBuffer buffer) {
		Client newClient = new Client();
		newClient.address = address;
		clients.add(newClient);
		handleClient(newClient, buffer);
	}
	
	private void checkConnections() {
		Iterator<Client> i = clients.iterator();
		while (i.hasNext()) {
			Client client = i.next();
			if (System.currentTimeMillis() - client.lastPacketTime > timeout) {
				i.remove();
			}
		}
	}
	
	private void listen() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.clear();
		
		InetSocketAddress address = (InetSocketAddress) channel.receive(buffer);
		buffer.flip();
		if (address != null) {
			int id = buffer.getInt();
			if (id == protocolId) {
				handlePacket(address, buffer);
			}
		}
	}
	
	private void send() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.clear();
		
		String data = "Time: " + System.currentTimeMillis();
		buffer.putInt(protocolId);
		buffer.put(data.getBytes());
		buffer.flip();
		
		channel.send(buffer, new InetSocketAddress("localhost", 4444));
	}
}

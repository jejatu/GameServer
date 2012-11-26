package gameserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
	public static final int PORT = 4444;
	public static final int BUFFER_SIZE = 256;
	public static final int PROTOCOL_ID = 145501337;
	DatagramChannel channel;
	List<Client> clients = new ArrayList<Client>();
	
	public void connect() throws IOException {
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(PORT));
	}
	
	public void disconnect() throws IOException {
		channel.disconnect();
		channel.close();
	}
	
	public void tick() throws IOException {
		listen();
		send();
		checkConnections();
		System.out.println(clients.size());
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
		System.out.println(new String(buffer.array()).trim());
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
			if (System.currentTimeMillis() - client.lastPacketTime > 10000) {
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
			System.out.println(id);
			if (id == PROTOCOL_ID) {
				handlePacket(address, buffer);
			}
		}
	}
	
	private void send() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.clear();
		
		String data = "Time: " + System.currentTimeMillis();
		buffer.putInt(PROTOCOL_ID);
		buffer.put(data.getBytes());
		buffer.flip();
		
		channel.send(buffer, new InetSocketAddress("localhost", 4444));
	}
}

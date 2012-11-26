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
	public boolean connected = false;
	
	public void start(int id, int port) throws IOException {
		// start the server by opening non-blocking udp port
		protocolId = id;
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(port));
		connected = true;
	}
	
	public void end() throws IOException {
		// disconnect from the udp port
		channel.disconnect();
		channel.close();
	}
	
	public void tick() throws IOException {
		// do this once every loop
		listen();
		resendLostPackets();
		checkConnections();
	}
	
	private void acceptConnection(InetSocketAddress address, ByteBuffer buffer) {
		// add a new client
		Client newClient = new Client();
		newClient.address = address;
		clients.add(newClient);
		handleClient(newClient, buffer);
	}
	
	private void checkConnections() {
		// iterate through clients and delete clients that have not send messages for some time
		Iterator<Client> i = clients.iterator();
		while (i.hasNext()) {
			Client client = i.next();
			if (System.currentTimeMillis() - client.lastPacketTime > timeout) {
				i.remove();
			}
		}
	}
	
	private void handlePacket(InetSocketAddress address, ByteBuffer buffer) {
		// check if packet sent from known client
		// if not, accept the new client
		// if yes, handle request
		for (Client client : clients) {
			if (address.equals(client.address)) {
				handleClient(client, buffer);
				return;
			}
		}
		acceptConnection(address, buffer);
	}
	
	private void handleClient(Client client, ByteBuffer buffer) {
		// respond to clients message
		client.lastPacketTime = System.currentTimeMillis();
		int sequence = buffer.getInt();
		int ack = buffer.getInt();
		int ackBitField = buffer.getInt();
		// take the packet id client send and check if it's the most recent
		// if yes, save it and update the queue of packets ids received
		// with the queue we can tell to the client which packets we have received
		if (isMostRecentPacket(sequence, client.remotePacketNumber, Integer.MAX_VALUE)) {
			client.packetNumberQueue.add(client.remotePacketNumber);
			client.remotePacketNumber = sequence;
			client.ack = ack;
			client.remoteAckBitfield = ackBitField;
			checkPackets(client);
			Iterator<Integer> i = client.packetNumberQueue.iterator();
			while (i.hasNext()) {
				int packetNumber = i.next().intValue();
				client.localAckBitfield = client.localAckBitfield | (1 << (client.remotePacketNumber - packetNumber - 1));
				if (client.remotePacketNumber - 32 > packetNumber) {
					i.remove();
				}
			}
		}
		
		send(client, "pong " + System.currentTimeMillis());
	}
	
	private boolean isMostRecentPacket(int p1, int p2, int maxNumber) {
		return (p1 > p2) && (p1 - p2 <= maxNumber / 2) || (p2 > p1) && (p2 - p1 > maxNumber / 2);
	}
	
	private void checkPackets(Client client) {
		// checks the packet header for confirmations of past packets
		Iterator<Packet> i = client.packets.iterator();
		while (i.hasNext()) {
			int packetNumber = i.next().id;
			if (client.ack == packetNumber) {
				i.remove();
			} else {
				if ((client.remoteAckBitfield & (1 << (client.ack - packetNumber - 1))) != 0) {
					i.remove();
				}
			}
		}
	}
	
	private void resendLostPackets() {
		// if client hasn't confirmed a packet in a second it is resend
		for (Client client : clients) {
			List<String> lostMessages = new ArrayList<String>();
			Iterator<Packet> i = client.packets.iterator();
			while (i.hasNext()) {
				Packet packet = i.next();
				if (System.currentTimeMillis() - packet.sentTime > 1000) {
					lostMessages.add(new String(packet.data));
					i.remove();
				} 
			}
			for (String message : lostMessages) {
				send(client, message);
			}
		}
	}
	
	private void listen() throws IOException {
		// listen for new messages
		// ignore messages that do not identify themselves with protocol id
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
	
	private void send(Client client, String data) {
		// send a packet containing a header and the data
		try {
			ByteBuffer buffer = ByteBuffer.allocate(256);
			buffer.clear();
			buffer.putInt(1337); // protocol id
			buffer.putInt(client.localPacketNumber); // current known sent packet number
			buffer.putInt(client.remotePacketNumber); // current known received packet number
			buffer.putInt(client.localAckBitfield);
			buffer.put(data.getBytes());
			buffer.flip();
			channel.send(buffer, client.address);
			Packet packet = new Packet();
			packet.id = client.localPacketNumber;
			packet.data = new String(data);
			packet.sentTime = System.currentTimeMillis();
			client.packets.add(packet);
			
			client.localPacketNumber++;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

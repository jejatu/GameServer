package gameserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
	private DatagramChannel channel;
	private long millis = 0;
	
	public List<Client> clients = new ArrayList<Client>();
	public int protocolId = 0;
	public int timeout = 10000;
	public boolean connected = false;
	public int bufferSize = 256;
	
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
		millis = System.currentTimeMillis();
		flowControl();
		listen();
		checkPackets();
		send();
		checkConnections();
	}
	
	private void flowControl() {
		for (Client client : clients) {
			client.simpleBinaryFlowControl(millis);
		}
	}
	
	private void acceptConnection(InetSocketAddress address, ByteBuffer buffer) {
		// add a new client
		clients.add(new Client(address));
		handleClient(clients.get(clients.size() - 1), buffer);
	}
	
	private void checkConnections() {
		// iterate through clients and delete clients that have not send messages for some time
		Iterator<Client> i = clients.iterator();
		while (i.hasNext()) {
			Client client = i.next();
			if (millis - client.lastArrivalTime > timeout) {
				// client hasn't sent messages for (timeout) seconds
				i.remove();
			} else if (client.unsentPackets.size() > (1000.0f / client.dispatchDelay) * timeout) {
				// client hasn't confirmed (timeout) seconds worth of packets
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
		client.lastArrivalTime = millis;
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
			Iterator<Integer> i = client.packetNumberQueue.iterator();
			while (i.hasNext()) {
				int packetNumber = i.next().intValue();
				client.localAckBitfield = client.localAckBitfield | (1 << (client.remotePacketNumber - packetNumber - 1));
				if (client.remotePacketNumber - 32 > packetNumber) {
					i.remove();
				}
			}
		}
		
		queue(client, "pong");
	}
	
	private boolean isMostRecentPacket(int s1, int s2, int maxNumber) {
		long p1 = (long)s1 & 0xffffffffL;
		long p2 = (long)s2 & 0xffffffffL;
		return (p1 > p2) && (p1 - p2 <= maxNumber / 2) || (p2 > p1) && (p2 - p1 > maxNumber / 2);
	}
	
	private void checkPackets() {
		// checks the packet header for confirmations of past packets
		for (Client client : clients) {
			Iterator<Packet> i = client.unconfirmedPackets.iterator();
			while (i.hasNext()) {
				Packet packet = i.next();
				if (millis - packet.timeSent >= 1000) {
					// if packet isn't confirmed in a second resend it
					queue(client, packet.buffer);
					i.remove();
				} else {
					if (client.ack == packet.id) {
						client.roundTripTime = client.roundTripTime * 0.9f + (millis - packet.timeSent) * 0.1f;
						i.remove();
					} else if (packet.id < client.ack && packet.id > client.ack - 33
							&& (client.remoteAckBitfield & (1 << (client.ack - packet.id - 1))) != 0) {
						client.roundTripTime = client.roundTripTime * 0.9f + (millis - packet.timeSent) * 0.1f;
						i.remove();
					}
				}
			}
		}
	}
	
	private void listen() throws IOException {
		// listen for new messages
		// ignore messages that do not identify themselves with protocol id
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
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
	
	private void queue(Client client, String data) {
		// queue a packet containing a header and the data
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.clear();
		buffer.putInt(1337); // protocol id
		buffer.putInt(client.localPacketNumber); // current known sent packet number
		buffer.putInt(client.remotePacketNumber); // current known received packet number
		buffer.putInt(client.localAckBitfield);
		buffer.put(data.getBytes());
		
		client.unsentPackets.add(new Packet(client.localPacketNumber, buffer));
		client.localPacketNumber++;
	}
	
	private void queue(Client client, ByteBuffer buffer) {
		// queue a packet with a predone buffer
		client.unsentPackets.add(new Packet(client.localPacketNumber, buffer));
	}
	
	private void send() {
		// send queued messages to each client one per call
		for (Client client : clients) {
			if (millis - client.lastDispatchTime >= client.dispatchDelay && client.unsentPackets.size() > 0) {
				client.lastDispatchTime = millis;
				client.unconfirmedPackets.add(new Packet(client.unsentPackets.get(0).id, client.unsentPackets.get(0).buffer, millis));
				client.unsentPackets.get(0).buffer.flip();
				try {
					channel.send(client.unsentPackets.get(0).buffer, client.address);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				client.unsentPackets.remove(0);
			}
		}
	}
}

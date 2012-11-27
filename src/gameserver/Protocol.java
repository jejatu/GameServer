package gameserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Protocol {
	private DatagramChannel channel;
	private long millis = 0;
	
	public List<Connection> connections = new ArrayList<Connection>();
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
		for (Connection connection : connections) {
			connection.simpleBinaryFlowControl(millis);
		}
	}
	
	private void checkConnections() {
		// iterate through connections and delete connections that have not send messages for some time
		Iterator<Connection> i = connections.iterator();
		while (i.hasNext()) {
			Connection connection = i.next();
			if (millis - connection.lastArrivalTime > timeout) {
				// connection hasn't sent messages for (timeout) seconds
				i.remove();
			} else if (connection.unsentPackets.size() > (1000.0f / connection.dispatchDelay) * timeout) {
				// connection hasn't confirmed (timeout) seconds worth of packets
				i.remove();
			}
		}
	}
	
	protected void handleConnection(InetSocketAddress address, ByteBuffer buffer) {
		// connection handling left for server and client
	}
	
	protected void handlePacket(Connection connection, ByteBuffer buffer) {
		// respond to connections message
		connection.lastArrivalTime = millis;
		int sequence = buffer.getInt();
		int ack = buffer.getInt();
		int ackBitField = buffer.getInt();
		// take the packet id connection send and check if it's the most recent
		// if yes, save it and update the queue of packets ids received
		// with the queue we can tell to the connection which packets we have received
		if (isMostRecentPacket(sequence, connection.remotePacketNumber, Integer.MAX_VALUE)) {
			connection.packetNumberQueue.add(connection.remotePacketNumber);
			connection.remotePacketNumber = sequence;
			connection.ack = ack;
			connection.remoteAckBitfield = ackBitField;
			Iterator<Integer> i = connection.packetNumberQueue.iterator();
			while (i.hasNext()) {
				int packetNumber = i.next().intValue();
				connection.localAckBitfield = connection.localAckBitfield | (1 << (connection.remotePacketNumber - packetNumber - 1));
				if (connection.remotePacketNumber - 32 > packetNumber) {
					i.remove();
				}
			}
		}
		
		respond(connection, buffer);
	}
	
	private boolean isMostRecentPacket(int s1, int s2, int maxNumber) {
		long p1 = (long)s1 & 0xffffffffL;
		long p2 = (long)s2 & 0xffffffffL;
		return (p1 > p2) && (p1 - p2 <= maxNumber / 2) || (p2 > p1) && (p2 - p1 > maxNumber / 2);
	}
	
	private void checkPackets() {
		// checks the packet header for confirmations of past packets
		for (Connection connection : connections) {
			Iterator<Packet> i = connection.unconfirmedPackets.iterator();
			while (i.hasNext()) {
				Packet packet = i.next();
				if (millis - packet.timeSent >= 1000) {
					// if packet isn't confirmed in a second resend it
					queue(connection, packet.buffer);
					i.remove();
				} else {
					if (connection.ack == packet.id) {
						connection.roundTripTime = connection.roundTripTime * 0.9f + (millis - packet.timeSent) * 0.1f;
						i.remove();
					} else if (packet.id < connection.ack && packet.id > connection.ack - 33
							&& (connection.remoteAckBitfield & (1 << (connection.ack - packet.id - 1))) != 0) {
						connection.roundTripTime = connection.roundTripTime * 0.9f + (millis - packet.timeSent) * 0.1f;
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
				// check if packet sent from known connection
				// if not, accept the new connection
				// if yes, handle request
				for (Connection connection : connections) {
					if (address.equals(connection.address)) {
						handlePacket(connection, buffer);
						return;
					}
				}
				handleConnection(address, buffer);
			}
		}
	}
	
	protected void queue(Connection connection, String data) {
		// queue a packet containing a header and the data
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.clear();
		buffer.putInt(1337); // protocol id
		buffer.putInt(connection.localPacketNumber); // current known sent packet number
		buffer.putInt(connection.remotePacketNumber); // current known received packet number
		buffer.putInt(connection.localAckBitfield);
		buffer.put(data.getBytes());
		buffer.flip();
		
		connection.unsentPackets.add(new Packet(connection.localPacketNumber, buffer));
		connection.localPacketNumber++;
	}
	
	private void queue(Connection connection, ByteBuffer buffer) {
		// queue a packet with a predone buffer
		connection.unsentPackets.add(new Packet(connection.localPacketNumber, buffer));
	}
	
	protected void respond(Connection connection, ByteBuffer buffer) {
		// client and server handle responses
	}
	
	private void send() {
		// send queued messages to each connection one per call
		for (Connection connection : connections) {
			if ((!connection.hasFlowControl || 
					millis - connection.lastDispatchTime >= connection.dispatchDelay) && 
					connection.unsentPackets.size() > 0) {
				connection.lastDispatchTime = millis;
				connection.unconfirmedPackets.add(new Packet(connection.unsentPackets.get(0).id, connection.unsentPackets.get(0).buffer, millis));
				try {
					channel.send(connection.unsentPackets.get(0).buffer, connection.address);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				connection.unsentPackets.remove(0);
			}
		}
	}
}

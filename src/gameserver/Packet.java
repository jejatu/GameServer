package gameserver;

import java.nio.ByteBuffer;

public class Packet {
	int id;
	ByteBuffer buffer;
	long timeSent;
	
	public Packet(int id, ByteBuffer buffer) {
		this.id = id;
		this.buffer = buffer;
	}
	
	public Packet(int id, ByteBuffer buffer, long timeSent) {
		this.id = id;
		this.buffer = buffer;
		this.timeSent = timeSent;
	}
}

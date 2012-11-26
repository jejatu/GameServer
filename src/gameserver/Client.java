package gameserver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Client {
	InetSocketAddress address;
	long lastPacketTime = 0;
	long lastSentTime = 0;
	int localPacketNumber = 0;
	int remotePacketNumber = 0;
	int ack = 0;
	int localAckBitfield = 0;
	int remoteAckBitfield = 0;
	List<Integer> packetNumberQueue = new ArrayList<Integer>();
	List<Packet> packets = new ArrayList<Packet>();
	List<QueueData> queue = new ArrayList<QueueData>();
}

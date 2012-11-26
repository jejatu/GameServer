package gameserver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Client {
	InetSocketAddress address;
	long lastPacketTime = 0;
	int localPacketNumber = 0;
	int remotePacketNumber = 0;
	List<Integer> packetNumberQueue = new ArrayList<Integer>();
}

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
	float roundTripTime = 0;
	int queueDelay = 33;
	boolean goodMode = true;
	long lastHighLatencyTime = 0;
	long modeChangeTime = 0;
	long lastModeChange = 0;
	List<Integer> packetNumberQueue = new ArrayList<Integer>();
	List<Packet> packets = new ArrayList<Packet>();
	List<QueueData> queue = new ArrayList<QueueData>();
	
	public void simpleBinaryFlowControl(long millis) {
		// a simple flow control from
		// http://gafferongames.com/networking-for-game-programmers/reliability-and-flow-control/
		if (goodMode) {
			queueDelay = 33;
			if (roundTripTime > 250) {
				if (millis - lastModeChange < 10000) {
					if (modeChangeTime * 2 < 60000) modeChangeTime *= 2;
				}
				lastModeChange = millis;
				goodMode = false;
			}
			if (millis - lastModeChange >= 10000) {
				if (modeChangeTime / 2 >= 1000) { 
					modeChangeTime /= 2;
				}
			}
		} else {
			queueDelay = 100;
			if (roundTripTime > 250) {
				lastHighLatencyTime = millis;
			}
			if (millis - lastHighLatencyTime > modeChangeTime) {
				lastModeChange = millis;
				goodMode = true;
			}
		}
	}
}

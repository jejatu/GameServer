package gameserver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Connection {
	InetSocketAddress address;
	long lastArrivalTime = 0;
	long lastDispatchTime = 0;
	int localPacketNumber = 0;
	int remotePacketNumber = 0;
	int ack = 0;
	int localAckBitfield = 0;
	int remoteAckBitfield = 0;
	float roundTripTime = 0;
	int dispatchDelay = 33;
	int highDelay = 100;
	int lowDelay = 33;
	boolean hasFlowControl = true;
	boolean goodMode = true;
	long lastHighLatencyTime = 0;
	long modeChangeTime = 0;
	long lastModeChange = 0;
	long highLatency = 250;
	List<Integer> packetNumberQueue = new ArrayList<Integer>();
	List<Packet> unconfirmedPackets = new ArrayList<Packet>();
	List<Packet> unsentPackets = new ArrayList<Packet>();
	
	public Connection(InetSocketAddress address) {
		this.address = address;
		lastArrivalTime = System.currentTimeMillis();
	}
	
	public void simpleBinaryFlowControl(long millis) {
		// a simple flow control from
		// http://gafferongames.com/networking-for-game-programmers/reliability-and-flow-control/
		if (hasFlowControl) {
			if (goodMode) {
				dispatchDelay = lowDelay;
				if (roundTripTime > highLatency) {
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
				dispatchDelay = highDelay;
				if (roundTripTime > highLatency) {
					lastHighLatencyTime = millis;
				}
				if (millis - lastHighLatencyTime > modeChangeTime) {
					lastModeChange = millis;
					goodMode = true;
				}
			}
		}
	}
}

package gameserver;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class Main {
	public static void main(String[] args) {
		JFrame frame = new JFrame("Server");
		frame.setSize(640, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel debugLabel = new JLabel();
		frame.getContentPane().add(debugLabel, BorderLayout.CENTER);
		frame.setVisible(true);
		Server server = new Server();
		try {
			long lastTime = System.currentTimeMillis();
			server.start(1337, 4444);
			while (server.connected) {
				server.tick();
				if (System.currentTimeMillis() - lastTime > 1000) {
					String info = new String();
					info += "<html>";
					for (Client client : server.clients) {
						info += client.address.getHostName() + ":" + client.address.getPort() + "<br>";
						info += "RTT: " + client.roundTripTime + "<br>";
						info += "Local packet: " + client.localPacketNumber + "<br>";
						info += "Remote packet: " + client.remotePacketNumber + "<br>";
						info += "Packets per second: " + (1000.0f / client.dispatchDelay) + "<br>";
						info += "Unconfirmed packets: " + client.unconfirmedPackets.size() + "<br>";
						info += "Unsent packets: " + client.unsentPackets.size() + "<br>";
						info += "<br>";
					}
					info += "</html>";
					debugLabel.setText(info);
					lastTime = System.currentTimeMillis();
				}
			}
			server.end();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
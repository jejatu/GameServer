package gameserver;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.InetSocketAddress;

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
		Client client = new Client();
		try {
			long lastTime = System.currentTimeMillis();
			server.start(1337, 4444);
			client.start(1337, 0);
			client.connect(new InetSocketAddress("192.168.1.66", 4444));
			while (server.connected) {
				server.tick();
				client.tick();
				if (System.currentTimeMillis() - lastTime > 1000) {
					String info = new String();
					info += "<html>";
					for (Connection connection : server.connections) {
						info += connection.address.getHostName() + ":" + connection.address.getPort() + "<br>";
						info += "RTT: " + connection.roundTripTime + "<br>";
						info += "Local packet: " + connection.localPacketNumber + "<br>";
						info += "Remote packet: " + connection.remotePacketNumber + "<br>";
						info += "Packets per second: " + (1000.0f / connection.dispatchDelay) + "<br>";
						info += "Unconfirmed packets: " + connection.unconfirmedPackets.size() + "<br>";
						info += "Unsent packets: " + connection.unsentPackets.size() + "<br>";
						info += "<br>";
					}
					info += "</html>";
					debugLabel.setText(info);
					lastTime = System.currentTimeMillis();
				}
			}
			server.end();
			client.end();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
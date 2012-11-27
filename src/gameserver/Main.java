package gameserver;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class Main {
	public static void main(String[] args) {
		JFrame frame = new JFrame("Server");
		frame.setSize(640, 480);
		JLabel debugLabel = new JLabel();
		frame.getContentPane().add(debugLabel, BorderLayout.CENTER);
		frame.setVisible(true);
		Server server = new Server();
		List<Client> clients = new ArrayList<Client>();
		clients.add(new Client());
		clients.add(new Client());
		clients.add(new Client());
		try {
			long lastTime = System.currentTimeMillis();
			server.start(1337, 4444);
			for (Client client : clients) {
				client.start(1337, 0);
				client.connect(new InetSocketAddress("192.168.1.66", 4444));
			}
			while (frame.isFocused()) {
				server.tick();
				for (Client client : clients) {
					client.tick();
				}
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
			for (Client client : clients) {
				client.end();
			}
			frame.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
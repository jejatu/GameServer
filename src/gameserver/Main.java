package gameserver;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		Server server = new Server();
		try {
			server.start(1337, 4444, 128);
			while (server.connected) {
				server.tick();
			}
			server.end();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
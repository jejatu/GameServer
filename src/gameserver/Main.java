package gameserver;

import java.io.IOException;


public class Main {
	public static void main(String[] args) {
		Server server = new Server();
		try {
			server.connect();
			for (int i = 0; i < 100; i++) {
				server.tick();
			}
			server.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}

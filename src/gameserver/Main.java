package gameserver;

import java.io.IOException;


public class Main {
	public static void main(String[] args) {
		Server server = new Server();
		try {
			server.start(1337, 4444);
			for (int i = 0; i < 100; i++) {
				server.tick();
			}
			server.end();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

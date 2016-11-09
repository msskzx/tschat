package tschat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class Server {

	private ServerSocket server;
	static ArrayList<ServerRunnable> serverRunnables;

	public Server() {
		serverRunnables = new ArrayList<>();
		
		try {
			// The maximum queue length for incoming connection indications (a
			// request to connect) is set to 50
			server = new ServerSocket(6000);
			
			System.out.println("Waiting for someone to connect...");
			
			waitingForConnection();
		
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	void waitingForConnection() {
		while (true) {
			try {
				ServerRunnable serverRunnable = new ServerRunnable(server.accept());
				serverRunnables.add(serverRunnable);
				new Thread(serverRunnable).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new Server();
	}
}

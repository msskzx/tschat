package tschat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

	private ServerSocket server;
	private ArrayList<ServerRunnable> serverRunnables;
	private int port;
	private SpecialServerRunnable specialServer;

	public Server(int port) {
		serverRunnables = new ArrayList<>();
		this.port = port;
		try {
			server = new ServerSocket(this.port);

			if (port == 6000)
				waitingForServer();
			else
				connectToServer();

			System.out.println("Waiting for someone to connect...");
			waitingForConnection();

		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	void waitingForConnection() {
		while (true) {
			try {
				ServerRunnable serverRunnable = new ServerRunnable(server.accept(), this);
				serverRunnables.add(serverRunnable);
				new Thread(serverRunnable).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void waitingForServer() {
		try {
			specialServer = new SpecialServerRunnable(server.accept(), this);
			new Thread(specialServer).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<ServerRunnable> getServerRunnables() {
		return serverRunnables;
	}

	void connectToServer() {
		try {
			Socket connection = new Socket(InetAddress.getByName("127.0.0.1"), 6000);
			specialServer = new SpecialServerRunnable(connection, this);
			new Thread(specialServer).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SpecialServerRunnable getSpecialServer() {
		return specialServer;
	}

	public static void main(String[] args) {
		new Server(6000);
	}
}

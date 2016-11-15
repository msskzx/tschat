package tschat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Server {

	private ServerSocket server;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Socket connection;
	private ArrayList<ServerRunnable> serverRunnables;
	private int port;

	public Server(int port) {
		serverRunnables = new ArrayList<>();
		this.port = port;
		try {
			// The maximum queue length for incoming connection indications (a
			// request to connect) is set to 50
			server = new ServerSocket(port);

			System.out.println("Waiting for someone to connect...");
			if (port == 6000)
				waitingForServer();
			else
				connectToServer(6000);
			new Thread(new Runnable() {
				@Override
				public void run() {
					chatWithServer();
				}
			}).start();

			new Thread(new Runnable() {
				@Override
				public void run() {
					waitingForConnection();
				}
			}).start();

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
			connection = server.accept();
			setupStreams();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void connectToServer(int port) {
		try {
			connection = new Socket(InetAddress.getByName("127.0.0.1"), port);
			setupStreams();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		port = port == 6000 ? 6001 : 6000;
	}

	void setupStreams() {
		try {
			output = new ObjectOutputStream(connection.getOutputStream());
			output.flush();
			input = new ObjectInputStream(connection.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<ServerRunnable> getServerRunnables() {
		return serverRunnables;
	}

	String getMemberList() {
		try {
			output.writeObject("\\getMemberListOfMyServer");
			String inp = null;
			do {
				try {
					inp = (String) input.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} while (inp == null);
			return inp;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	void sendMemberList(String message) {
		if (message == null)
			return;

		if (message.equals("\\getMemberListOfMyServer")) {
			String members = "";
			for (ServerRunnable x : serverRunnables)
				members += (" - " + x.getClientName());
			try {
				output.writeObject(members);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void chatWithServer() {
		do
			try {
				String message;
				try {
					message = (String) input.readObject();
					if (message != null && !message.equals(""))
						sendMemberList(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
			}
		while (true);
	}

	public static void main(String[] args) {
		new Server(6000);

	}
}

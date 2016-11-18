package tschat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SpecialServerRunnable implements Runnable {

	private Server server;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Socket connection;
	private String message;

	public SpecialServerRunnable(Socket connection, Server server) {
		this.connection = connection;
		this.server = server;
		message = "Fetching information... click again...";
	}

	@Override
	public void run() {
		setupStreams();
		chatWithServer();
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

	private void chatWithServer() {
		while (true) {
			try {
				message = (String) input.readObject();
				if (message.equals("\\getMemberListOfMyServer"))
					sendMyMemberList();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	String getMemberList() {
		try {
			output.writeObject("\\getMemberListOfMyServer");
			output.flush();
		} catch (Exception e) {
		}
		return message;
	}

	void sendMyMemberList() {
		if (server.getServerRunnables().size() > 0) {
			message = "Active users:\n";
			for (ServerRunnable x : server.getServerRunnables())
				message += " - " + x.getClientName() + "\n";
		} else
			message = "No active users on this server.\n";
		sendMessage(message);
	}

	public void sendMessage(String message) {
		try {
			output.writeObject(message);
			output.flush();
		} catch (IOException e) {
		}
	}

}

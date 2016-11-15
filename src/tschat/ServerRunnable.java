package tschat;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerRunnable implements Runnable {

	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Socket conncetion;
	private String clientName;
	private Server server;

	public ServerRunnable(Socket connection, Server server) {
		this.conncetion = connection;
		this.server = server;
		clientName = "192595315646546256165465";
	}

	public void run() {
		try {
			while (true) {
				try {
					setupStreams();
					whileChatting();
				} catch (EOFException eofException) {
					System.out.println("Server ended the connection!\n");
				} finally {
					close();
					removeUser();
				}
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void removeUser() {
		Iterator<ServerRunnable> iterator = server.getServerRunnables().iterator();
		while (iterator.hasNext()) {
			ServerRunnable x = iterator.next();
			if (x.clientName != null && x.clientName.equals(clientName))
				iterator.remove();
		}
	}

	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(conncetion.getOutputStream());
		output.flush();
		input = new ObjectInputStream(conncetion.getInputStream());
	}

	private void whileChatting() throws IOException {
		String message = "\nYou are now connected!\n---\n";
		joinResponse();
		sendMessage(message + "Enter username of the person you would to chat with in the first text field\n"
				+ "and your messege in the second text field.\n---\n");
		do {
			try {
				String encodedMessage = (String) input.readObject();

				message = getMessage(encodedMessage);

				if (getMemberList(message))
					continue;

				String destination = getDestination(encodedMessage);
				String source = getSource(encodedMessage);
				getTTL(encodedMessage);

				ServerRunnable destinationServerRunnable = null;
				ServerRunnable sourceServerRunnable = null;

				for (ServerRunnable serverRunnable : server.getServerRunnables())
					if (serverRunnable.clientName.equals(destination))
						destinationServerRunnable = serverRunnable;

				for (ServerRunnable serverRunnable : server.getServerRunnables())
					if (serverRunnable.clientName.equals(source))
						sourceServerRunnable = serverRunnable;

				if (destinationServerRunnable != null) {
					String s = source + " says" + ": " + message;
					destinationServerRunnable.output.writeObject(s);
					sourceServerRunnable.output.writeObject(s);
					System.out.println(s + "\n");
				} else
					sendMessage("Destination Username: " + destination + " doesn't Exist.");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} while (!message.equals("BYE") && !message.equals("QUIT"));
	}

	private static String getSource(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); ++i)
			if (s.charAt(i) == '$')
				return sb.toString();
			else
				sb.append(s.charAt(i));
		return "";
	}

	private static String getDestination(String s) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (s.charAt(i) != '$')
			i++;
		i++;
		for (; i < s.length(); ++i)
			if (s.charAt(i) == '$')
				return sb.toString();
			else
				sb.append(s.charAt(i));
		return "";
	}

	private static int getTTL(String s) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (s.charAt(i) != '$')
			i++;
		i++;
		while (s.charAt(i) != '$')
			i++;
		i++;
		for (; i < s.length(); ++i)
			if (s.charAt(i) == '$')
				return Integer.parseInt(sb.toString());
			else
				sb.append(s.charAt(i));
		return 1;
	}

	private static String getMessage(String s) {
		System.out.println(s);
		int i = 0;
		while (s.charAt(i) != '$')
			i++;
		i++;
		while (s.charAt(i) != '$')
			i++;
		i++;
		while (s.charAt(i) != '$')
			i++;
		i++;
		return s.substring(i);
	}

	private void joinResponse() {
		String message = "";
		while (true) {
			try {
				try {
					message = (String) input.readObject();
				} catch (IOException e) {
					e.printStackTrace();
				}
				boolean found = false;
				ArrayList<ServerRunnable> serverRunnables = server.getServerRunnables();
				synchronized (serverRunnables) {
					for (ServerRunnable serverRunnable : serverRunnables)
						if (serverRunnable.clientName != null && serverRunnable != this
								&& serverRunnable.clientName.equals(message)) {
							found = true;
							break;
						}
					if (!found) {
						clientName = message;
						sendMessage("Username accepted!!");
						break;
					}
				}
				sendMessage("Username already exists , Please choose another name.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
	}

	private void close() {
		System.out.println("Closing connections...\n---\n");
		try {
			output.close();
			input.close();
			conncetion.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	boolean getMemberList(String message) {
		if (message.equals("\\getAllMembers")) {
			sendMessage(getMemberListThis() + server.getMemberList());
		} else if (message.equals("\\getMemberListOfMyServer")) {
			sendMessage(getMemberListThis());
			return true;
		} else if (message.equals("\\getMemberListOfOtherServer")) {
			sendMessage(server.getMemberList());
		}
		return false;
	}

	String getMemberListThis() {
		String members = "";
		if (server.getServerRunnables().size() > 1) {
			sendMessage("Active users:");
			for (ServerRunnable x : server.getServerRunnables())
				if (x != this)
					if (valid(x.clientName))
						members += (" - " + x.clientName);
					else
						sendMessage(" - connecting this user...");
		} else
			sendMessage("No online users but you");
		return members;
	}

	public void sendMessage(String message) {
		try {
			output.writeObject(message);
			output.flush();
		} catch (IOException e) {
		}
	}

	private boolean valid(String userName) {
		for (int i = 0; i < userName.length(); ++i)
			if (!Character.isAlphabetic(userName.charAt(i)))
				return false;
		return true;
	}

	public String getClientName() {
		return clientName;
	}

}

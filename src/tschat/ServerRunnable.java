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

	public ServerRunnable(Socket connection) {
		this.conncetion = connection;
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
		Iterator<ServerRunnable> iterator = Server.serverRunnables.iterator();
		while (iterator.hasNext()) {
			ServerRunnable x = iterator.next();
			if (x.clientName != null && x.clientName.equals(clientName))
				iterator.remove();
		}
	}

	// get stream to send and receive data
	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(conncetion.getOutputStream());
		output.flush();
		input = new ObjectInputStream(conncetion.getInputStream());
	}

	// during the chat conversation
	private void whileChatting() throws IOException {
		String message = "\nYou are now connected!\n---\n";
		joinResponse();
		sendMessage(message + "Please Enter Destination User name in the first Text field\n"
				+ "Please Enter Your Messege in the Second Text field.\n---\n");
		do {
			// have a conversation
			try {
				String encodedMessage = (String) input.readObject();
				if (getMemberList(encodedMessage))
					continue;

				message = getMessage(encodedMessage);
				String destination = getDestination(encodedMessage);
				String source = getSource(encodedMessage);
				int TTL = getTTL(encodedMessage);

				ServerRunnable destinationServerRunnable = null;
				ServerRunnable sourceServerRunnable = null;

				System.out.println(source);
				System.out.println(destination);

				for (ServerRunnable serverRunnable : Server.serverRunnables)
					if (serverRunnable.clientName.equals(destination))
						destinationServerRunnable = serverRunnable;

				for (ServerRunnable serverRunnable : Server.serverRunnables)
					if (serverRunnable.clientName.equals(source))
						sourceServerRunnable = serverRunnable;

				if (destinationServerRunnable != null) {
					String mess = source + " says" + ": " + message;
					destinationServerRunnable.output.writeObject(mess);
					sourceServerRunnable.output.writeObject(mess);
				} else
					sendMessage("Destination Username : " + destination + " doesn't Exist.");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("Client says: " + message + "\n");
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
		ArrayList<ServerRunnable> serverRunnables = Server.serverRunnables;
		String message = "";
		while (true) {
			try {
				try {
					message = (String) input.readObject();
				} catch (IOException e) {
					e.printStackTrace();
				}
				boolean found = false;
				for (ServerRunnable serverRunnable : serverRunnables)
					if (serverRunnable.clientName != null && serverRunnable != this
							&& serverRunnable.clientName.equals(message))
						found = true;
				if (!found) {
					clientName = message;
					sendMessage("Username accepted!!");
					break;
				}
				sendMessage("Username already exists , Please choose another name.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
	}

	// close streams and sockets after you are done
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
		if (message.equals("\\getMemberList")) {
			if (Server.serverRunnables.size() > 1) {
				sendMessage("Active users:");
				for (ServerRunnable x : Server.serverRunnables)
					if (x != this)
						sendMessage(" - " + x.clientName);
			} else
				sendMessage("No online users but you");

			return true;
		}
		return false;
	}

	// send a message to the client
	public void sendMessage(String message) {
		try {
			output.writeObject(message);
			output.flush();
		} catch (IOException e) {
		}
	}

}

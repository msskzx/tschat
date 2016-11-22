package m3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ServerRunnable implements Runnable {

	private ObjectOutputStream output, outputToOther;
	private ObjectInputStream input, inputToOther;
	private Socket conncetion;
	private String clientName;
	private Server myServer;

	public ServerRunnable(Server myServer, Socket connection, ObjectOutputStream outputToOther,
			ObjectInputStream inputToOther) {
		this.conncetion = connection;
		this.myServer = myServer;
		this.outputToOther = outputToOther;
		this.inputToOther = inputToOther;
	}

	public void run() {
		while (true) {
			try {
				setupStreams();
				whileChatting();
			} catch (Exception e) {
				System.out.println("Server ended the connection!\n");
			} finally {
				close();
				for (ServerRunnable x : Server.serverRunnables)
					if (x.clientName != null && x.clientName.equals(clientName))
						Server.serverRunnables.remove(x);
			}
		}
	}

	// get stream to send and receive data
	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(conncetion.getOutputStream());
		output.flush();
		input = new ObjectInputStream(conncetion.getInputStream());
	}

	// during the chat conversation
	private void whileChatting() throws Exception {
		String message = "You are now connected!\n---\n";
		joinResponse();
		sendMessage("Please Enter Destination User name in the first Text field\n"
				+ "Please Enter Your Messege in the Second Text field.\n");
		do {
			// have a conversation
			try {
				String encodedMessage = (String) input.readObject();

				if (getMemberList(encodedMessage))
					continue;
				message = getMessage(encodedMessage);
				String destination = getDestination(encodedMessage);
				String source = getSource(encodedMessage);
				getTTL(encodedMessage);
				ServerRunnable destinationServerRunnable = null;
				ServerRunnable sourceServerRunnable = null;
				boolean found = false;
				for (ServerRunnable serverRunnable : Server.serverRunnables)
					if (serverRunnable.clientName.equals(destination)) {
						destinationServerRunnable = serverRunnable;
						found = true;
					}
				for (ServerRunnable serverRunnable : Server.serverRunnables)
					if (serverRunnable.clientName.equals(source))
						sourceServerRunnable = serverRunnable;
				if (found) {
					String mess = source + " says" + ": " + message;
					destinationServerRunnable.output.writeObject(mess);
					sourceServerRunnable.output.writeObject(mess);
				} else {
					outputToOther.writeObject("&&SendThis" + encodedMessage);
					String response = (String) inputToOther.readObject();
					if (response.equals("&&Not_There"))
						sendMessage("Destination Username : " + destination + " doesn't Exist.");
					else {
						String mess = source + " says" + ": " + message;
						sourceServerRunnable.output.writeObject(mess);
					}
				}

				// Server.s.sendToAllClients(message , 2);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("Client says: " + message + "\n");
		} while (!message.equals("BYE") && !message.equals("QUIT"));

		for (ServerRunnable x : Server.serverRunnables)
			if (x.clientName.equals(clientName))
				Server.serverRunnables.remove(x);
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

	public ObjectOutputStream getOutput() {
		return output;
	}

	private void joinResponse() throws Exception {
		String message = "";
		while (true) {
			try {
				try {
					message = (String) input.readObject();
				} catch (IOException e) {
					e.printStackTrace();
				}
				boolean found = false;
				ArrayList<ServerRunnable> serverRunnables = Server.serverRunnables;
				synchronized (serverRunnables) {
					for (ServerRunnable serverRunnable : serverRunnables)
						if (serverRunnable.clientName != null && serverRunnable != this
								&& serverRunnable.clientName.equals(message)) {
							found = true;
							break;
						}
					outputToOther.writeObject("&&DoYouHave" + message);
					outputToOther.flush();
					String response = (String) inputToOther.readObject();
					if (!found && response.equals("&&GoAhead")) {
						clientName = message;
						sendMessage("Username accepted !!\n");
						break;
					}
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

	boolean getMemberList(String message) throws Exception {
		if (message.equals("getAllMembers")) {
			sendMessage("users:");
			String members = "";
			for (ServerRunnable x : myServer.serverRunnables)
				members += x.clientName + "\n";
			outputToOther.writeObject("getYourMembers");
			outputToOther.flush();
			members += (String) inputToOther.readObject();
			sendMessage(members);
			return true;
		} else if (message.equals("getMyServerMembers")) {
			sendMessage("users:");
			String members = "";
			for (ServerRunnable x : myServer.serverRunnables)
				members += x.clientName + "\n";
			sendMessage(members);

			return true;
		} else if (message.equals("getOtherServerMembers")) {
			sendMessage("users:");
			String members = "";
			outputToOther.writeObject("getYourMembers");
			members += (String) inputToOther.readObject();
			sendMessage(members);
			return true;
		}
		return false;
	}

	public String getClientName() {
		return clientName;
	}

	public void sendMessage(String message) {
		try {
			output.writeObject(message);
			output.flush();
		} catch (IOException e) {
		}
	}

}
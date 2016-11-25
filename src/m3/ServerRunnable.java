package m3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ServerRunnable implements Runnable {

	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ArrayList<ObjectOutputStream> outputToOther;
	private ArrayList<ObjectInputStream> inputToOther;
	private Socket conncetion;
	private String clientName;
	private Server myServer;

	public ServerRunnable(Server myServer, Socket connection, ArrayList<ObjectOutputStream> outputToOther,
			ArrayList<ObjectInputStream> inputToOther) {
		this.conncetion = connection;
		this.myServer = myServer;
		clientName = "125395415871";

		for (ObjectOutputStream x : outputToOther)
			this.outputToOther.add(x);

		System.out.println(outputToOther.size());

		for (ObjectInputStream x : inputToOther)
			this.inputToOther.add(x);
		System.out.println(inputToOther.size());

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
				for (ServerRunnable x : myServer.serverRunnables)
					if (x.clientName != null && x.clientName.equals(clientName))
						myServer.serverRunnables.remove(x);
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

				if (destination.equals("125395415871")) {
					sendMessage("This user is still trying to connect.");
					continue;
				}

				ServerRunnable destinationServerRunnable = null;
				ServerRunnable sourceServerRunnable = null;
				boolean found = false;
				for (ServerRunnable serverRunnable : myServer.serverRunnables)
					if (serverRunnable.clientName.equals(destination)) {
						destinationServerRunnable = serverRunnable;
						found = true;
					}
				for (ServerRunnable serverRunnable : myServer.serverRunnables)
					if (serverRunnable.clientName.equals(source))
						sourceServerRunnable = serverRunnable;
				if (found) {
					String mess = source + " says" + ": " + message;
					destinationServerRunnable.output.writeObject(mess);
					sourceServerRunnable.output.writeObject(mess);
				} else {

					boolean flag = false;

					for (int i = 0; i < outputToOther.size(); i++) {
						ObjectOutputStream oos = outputToOther.get(i);
						ObjectInputStream ois = inputToOther.get(i);

						oos.writeObject("&&SendThis" + encodedMessage);
						String response = (String) ois.readObject();

						if (response.equals("&&Not_There"))
							flag = false;
						else {
							String mess = source + " says" + ": " + message;
							sourceServerRunnable.output.writeObject(mess);
							flag = true;
							break;
						}
					}
					if (!flag)
						sendMessage("Destination Username : " + destination + " doesn't Exist.");

				}

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("Client says: " + message + "\n");
		} while (!message.equals("BYE") && !message.equals("QUIT"));

		for (ServerRunnable x : myServer.serverRunnables)
			if (x.clientName.equals(clientName))
				myServer.serverRunnables.remove(x);
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
				ArrayList<ServerRunnable> serverRunnables = myServer.serverRunnables;
				synchronized (serverRunnables) {
					for (ServerRunnable serverRunnable : serverRunnables)
						if (serverRunnable.clientName != null && serverRunnable != this
								&& serverRunnable.clientName.equals(message)) {
							found = true;
							break;
						}

					boolean flag = false;

					for (int i = 0; i < outputToOther.size(); i++) {
						ObjectOutputStream oos = outputToOther.get(i);
						ObjectInputStream ois = inputToOther.get(i);

						oos.writeObject("&&DoYouHave" + message);
						oos.flush();
						String response = (String) ois.readObject();

						if (response.equals("&&GoAhead")) {
							flag = true;

							break;
						}
					}

					if (!found && flag) {
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

			for (int i = 0; i < outputToOther.size(); i++) {
				ObjectOutputStream oos = outputToOther.get(i);
				ObjectInputStream ois = inputToOther.get(i);

				oos.writeObject("getYourMembers");
				oos.flush();
				members += (String) ois.readObject();
			}

			sendMessage(members);
			return true;
		} else if (message.equals("getMyServerMembers")) {
			sendMessage("users:");
			String members = "";
			for (ServerRunnable x : myServer.serverRunnables)
				members += x.clientName + "\n";
			sendMessage(members);

			return true;
		} else {
			if (message.equals("getOtherServerMembers0")) {
				sendMessage("users:");
				String members = "";
				outputToOther.get(0).writeObject("getYourMembers");
				members += (String) inputToOther.get(0).readObject();
				sendMessage(members);
				return true;
			} else {
				if (message.equals("getOtherServerMembers1")) {
					sendMessage("users:");
					String members = "";
					outputToOther.get(0).writeObject("getYourMembers");
					members += (String) inputToOther.get(1).readObject();
					sendMessage(members);
					return true;
				} else {
					if (message.equals("getOtherServerMembers2")) {
						sendMessage("users:");
						String members = "";
						outputToOther.get(0).writeObject("getYourMembers");
						members += (String) inputToOther.get(2).readObject();
						sendMessage(members);
						return true;
					}
				}
			}
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
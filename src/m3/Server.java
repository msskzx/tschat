package m3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

	private ServerSocket server;
	private Socket connection1, connection2;

	// 2 for the runnables
	private ObjectInputStream input1, input2;

	private ObjectOutputStream output1, output2;
	private int port;
	private String serverIP;
	public static ArrayList<ServerRunnable> serverRunnables;

	public Server(String serverIP, int port) throws Exception {
		this.port = port;
		this.serverIP = serverIP;
		serverRunnables = new ArrayList<>();
		server = new ServerSocket(port);
		if (port == 6000)
			connectToServer(6001);
		else
			waitForServerConnection();

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						waitForConnection();
					} catch (Exception e) {
					}
				}
			}
		}).start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						waitForServerRequests();
					} catch (Exception e) {
					}
				}
			}
		}).start();
	}

	private void waitForServerRequests() throws Exception {
		String request = (String) input1.readObject();
		if (request.equals("getYourMembers")) {
			String members = "";
			for (ServerRunnable x : serverRunnables)
				members += x.getClientName() + "\n";
			output2.writeObject(members);
			output2.flush();
		} else if (request.length() >= 10 && request.substring(0, 10).equals("&&SendThis")) {
			String encodedMessage = request.substring(10);
			String message = getMessage(encodedMessage);
			String destination = getDestination(encodedMessage);
			String source = getSource(encodedMessage);
			getTTL(encodedMessage);

			ServerRunnable destinationServerRunnable = null;
			boolean found = false;
			for (ServerRunnable serverRunnable : this.serverRunnables)
				if (serverRunnable.getClientName().equals(destination)) {
					destinationServerRunnable = serverRunnable;
					found = true;
				}

			if (found) {
				String mess = source + " says" + ": " + message;
				output2.writeObject("found");
				output2.flush();
				destinationServerRunnable.getOutput().writeObject(mess);
			} else {
				output2.writeObject("&&Not_There");
				output2.flush();
			}
		} else if (request.length() >= 11 && request.substring(0, 11).equals("&&DoYouHave")) {
			String name = request.substring(11);
			boolean found = false;
			for (ServerRunnable serverRunnable : this.serverRunnables)
				if (serverRunnable.getClientName().equals(name))
					found = true;
			if (found) {
				output2.writeObject("don'tAccept");
				output2.flush();
			} else {
				output2.writeObject("&&GoAhead");
				output2.flush();
			}
		}
	}

	private void connectToServer(int portTo) throws IOException {
		connection1 = new Socket(InetAddress.getByName(serverIP), portTo);
		connection2 = new Socket(InetAddress.getByName(serverIP), portTo);
		setupStreams();
	}

	private void waitForServerConnection() throws IOException {
		connection1 = server.accept();
		connection2 = server.accept();
		setupStreams();
	}

	private void setupStreams() throws IOException {
		output1 = new ObjectOutputStream(connection1.getOutputStream());
		output1.flush();
		input1 = new ObjectInputStream(connection1.getInputStream());
		output2 = new ObjectOutputStream(connection2.getOutputStream());
		output2.flush();
		input2 = new ObjectInputStream(connection2.getInputStream());
	}

	private void waitForConnection() throws IOException {

		ServerRunnable serverRunnable = new ServerRunnable(this, server.accept(), output1, input2);
		serverRunnables.add(serverRunnable);
		new Thread(serverRunnable).start();
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

	public static void main(String[] args) throws Exception {
		new Server("127.0.0.1", 6001);
	}
}
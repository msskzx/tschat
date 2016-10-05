package tschat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Client extends JFrame {
	private JTextField userText;
	private JTextArea chatWindow;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private String message = "";
	private String serverIP;
	private Socket connection;

	public Client(String host) {
		// host is the IP address of the server that we
		// want to connect to
		super("Client's Window");
		serverIP = host;
		userText = new JTextField();
		userText.setEditable(false);
		userText.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				sendMessage(event.getActionCommand());
				userText.setText("");
			}
		});

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(userText, BorderLayout.NORTH);
		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow), BorderLayout.CENTER);
		setSize(300, 300);
		setVisible(true);
	}

	public void startRunning() {
		try {
			connectToServer();
			setupStreams();
			whileChatting();
		} catch (EOFException e) {
			showMessage("Client Terminated Connection\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	// connect to server
	private void connectToServer() throws IOException {
		showMessage("Attempting connection...\n");
		connection = new Socket(InetAddress.getByName(serverIP), 6000);
		showMessage("Connected to " + connection.getInetAddress().getHostName() + "\n");
	}

	// set up Streams to send and receive messages
	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("Streams are now setup!\n");
	}

	// during the chat conversation
	private void whileChatting() throws IOException {
		showMessage("You are now connected!\n---\n");
		ableToType(true);
		do
			try {
				message = (String) input.readObject();
				showMessage("Server says: " + message + "\n");
			} catch (ClassNotFoundException e) {
				showMessage("There is a problem with the message\n");
			}
		while (true);
	}

	private void close() {
		showMessage("Closing connections...\n---\n");
		ableToType(false);
		try {
			output.close();
			input.close();
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessage(String message) {
		try {
			output.writeObject(message);
			output.flush();
			showMessage("Client says: " + message + "\n");
		} catch (IOException e) {
			chatWindow.append("Can't send that message\n");
		}
	}

	private void showMessage(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chatWindow.append(message);
			}
		});
	}

	private void ableToType(final boolean flag) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userText.setEditable(flag);
			}
		});
	}
}

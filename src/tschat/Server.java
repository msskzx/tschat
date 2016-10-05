package tschat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Server extends JFrame {

	private JTextField userText;
	private JTextArea chatWindow;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ServerSocket server;
	private Socket conncetion;

	public Server() {
		super("Server's Window");
		userText = new JTextField();
		userText.setEditable(false);
		userText.addActionListener(new ActionListener() {
			// after typing then hitting Enter
			public void actionPerformed(ActionEvent e) {
				sendMessage(e.getActionCommand());
				// e.getActionCommand() is the text in the textField
				userText.setText("");
			}
		});

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(userText, BorderLayout.NORTH);
		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow));
		setSize(300, 300);
		setVisible(true);
	}

	// set up and run the server
	public void startRunning() {
		try {
			// The maximum queue length for incoming connection indications (a
			// request to connect) is set to 50
			server = new ServerSocket(6000);
			while (true) {
				try {
					waitForConnection();
					setupStreams();
					whileChatting();
				} catch (EOFException eofException) {
					showMessage("Server ended the connection!\n");
				} finally {
					close();
				}
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void waitForConnection() throws IOException {
		showMessage("Waiting for someone to connect...\n");
		conncetion = server.accept();
		showMessage("Connected to " + conncetion.getInetAddress().getHostName() + "\n");
	}

	// get stream to send and receive data
	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(conncetion.getOutputStream());
		output.flush();
		input = new ObjectInputStream(conncetion.getInputStream());
		showMessage("Streams are now setup!\n");
	}

	// during the chat conversation
	private void whileChatting() throws IOException {
		String message = "You are now connected!\n---\n";
		showMessage(message);
		ableToType(true);
		do {
			// have a conversation
			try {
				message = (String) input.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			showMessage("Client says: " + message + "\n");
		} while (!message.equals("BYE") && !message.equals("QUIT"));
	}

	// close streams and sockets after you are done
	private void close() {
		showMessage("Closing connections...\n---\n");
		ableToType(false);
		try {
			output.close();
			input.close();
			conncetion.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// send a message to the client
	private void sendMessage(String message) {
		try {
			output.writeObject(message);
			output.flush();
			showMessage("Server says: " + message + "\n");
		} catch (IOException e) {
			chatWindow.append("Can't Send this Message\n");
		}
	}

	// updates chatWindow
	private void showMessage(final String text) {
		// to update only a part of the GUI not to draw it again
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chatWindow.append(text);
			}
		});
	}

	// User typing permission
	private void ableToType(final boolean flag) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userText.setEditable(flag);
			}
		});
	}
}

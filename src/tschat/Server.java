package tschat;

import java.awt.BorderLayout;
import java.awt.Event;
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
				sendMessage(e.getActionCommand() + "\n", true); // e.getActionCommand()
																// the
																// Text that is
																// written in
																// the textField
				userText.setText("");
			}
		});

		add(userText, BorderLayout.NORTH);
		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow));
		setSize(300, 150);
		setVisible(true);
	}

	// set up and run the server
	public void startRunning() {
		try {
			server = new ServerSocket(6000, 100); // 6789 is the port number,100
													// is how many people can
													// wait to connect
			while (true) {
				try {
					waitForConnection();
					setupStreams();
					whileChatting();
				} catch (EOFException eofException) {
					showMessage("Server ended the connection! \n");
				} finally {
					close();
				}
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void waitForConnection() throws IOException {
		showMessage("Waiting for someone to connect ... \n");
		conncetion = server.accept();
		showMessage("Now connected to " + conncetion.getInetAddress().getHostName() + "\n");
	}

	// get stream to send and receive data
	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(conncetion.getOutputStream());
		output.flush();
		input = new ObjectInputStream(conncetion.getInputStream());
		showMessage("Streams are now setup ! \n");
	}

	// during the chat conversation
	private void whileChatting() throws IOException {
		String message = "You are now connected! \n";
		sendMessage(message + "\n", false);
		ableToType(true);
		do {
			// have a conversation

			try {
				message = (String) input.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			showMessage("Client says : " + message + "\n");
		} while (!message.equals("BYE") && !message.equals("QUIT"));
	}

	// close streams ans sockets after you are done chatting
	private void close() {
		showMessage("Closing connections... \n");
		ableToType(false);
		try {
			output.close();
			input.close();
			conncetion.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// send a message to client
	private void sendMessage(String message, boolean isCon) {
		try {
			output.writeObject(message);
			output.flush();
			if (isCon)
				showMessage("Server says: " + message);
			else
				showMessage(message);
		} catch (IOException e) {
			chatWindow.append("Can't Sent this Message\n");
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

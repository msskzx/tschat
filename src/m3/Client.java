package m3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Client extends JFrame {

	private JTextField destinationName;
	private JTextField userText;
	private JTextArea chatWindow;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private String message = "";
	private String serverIP;
	private Socket connection;
	private Chat chat;
	private int toPort;

	public Client(String host, int toPort) {
		// host is the IP address of the server that we
		// want to connect to
		super("Client's Window");
		this.toPort = toPort;
		serverIP = host;
		userText = new JTextField();
		destinationName = new JTextField();
		userText.setEditable(false);
		destinationName.setEditable(false);
		destinationName = new JTextField();
		userText.setEditable(false);
		chat = new Chat();
		JLabel to = new JLabel("Destination Username: ");
		JLabel urMessage = new JLabel("Your Message: ");
		to.setForeground(new Color(204, 51, 0));
		urMessage.setForeground(new Color(204, 51, 0));

		userText.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				message = event.getActionCommand();
				chat.message = message;
				chat.destination = destinationName.getText();
				try {
					String s = encode(chat);
					System.out.println(chat.destination + " " + chat.source + " " + chat.TTL + " " + chat.message);
					output.writeObject(s);
					String tmpS = chat.source;
					chat = new Chat();
					chat.source = tmpS;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JButton btn = new JButton("Get All Active Users");
		btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getMemberList("getAllMembers");
			}
		});

		JButton btn2 = new JButton("Get My Server's Active Users");
		btn2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getMemberList("getMyServerMembers");
			}
		});

		JButton btn3 = new JButton("Get The Other Server's Users");
		btn3.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getMemberList("getOtherServerMembers");
			}
		});
		btn.setBackground(new Color(19, 38, 57));
		btn.setForeground(new Color(204, 51, 0));
		btn2.setBackground(new Color(19, 38, 57));
		btn2.setForeground(new Color(204, 51, 0));
		btn3.setBackground(new Color(19, 38, 57));
		btn3.setForeground(new Color(204, 51, 0));

		this.setBackground(new Color(51, 102, 153));
		JPanel x = new JPanel(new GridLayout(3, 0));
		x.add(to);
		x.add(urMessage);
		JPanel y = new JPanel(new BorderLayout());
		userText.setBackground(new Color(230, 230, 250));
		destinationName.setBackground(new Color(188, 210, 238));
		y.add(destinationName, BorderLayout.NORTH);
		y.add(userText, BorderLayout.CENTER);
		JPanel z = new JPanel(new BorderLayout());
		z.add(x, BorderLayout.WEST);
		z.add(y, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new BorderLayout());
		buttons.add(btn, BorderLayout.NORTH);
		buttons.add(btn2, BorderLayout.CENTER);
		buttons.add(btn3, BorderLayout.SOUTH);
		z.add(buttons, BorderLayout.SOUTH);
		add(z, BorderLayout.SOUTH);
		chatWindow = new JTextArea();
		chatWindow.setBackground(new Color(19, 38, 57));
		chatWindow.setForeground(new Color(236, 242, 248));
		add(new JScrollPane(chatWindow), BorderLayout.CENTER);
		setSize(700, 700);
		setVisible(true);
	}

	public void startRunning() {
		try {
			connectToServer();
			setupStreams();
			setupUsername();
			whileChatting();
		} catch (EOFException e) {
			showMessage("Client Terminated Connection\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	private static String encode(Chat c) {
		return c.source + "$" + c.destination + "$" + c.TTL + "$" + c.message;
	}

	public void setupUsername() {
		String ob = "";
		try {
			do {
				String s = JOptionPane.showInputDialog("Choose Username");
				if (!valid(s)) {
					showMessage("You can use only letters. (A - Z)\n");
					continue;
				}
				chat.source = s;
				output.writeObject(s);
				ob = (String) input.readObject();
				showMessage(ob);
				System.out.println(ob);
			} while (!ob.equals("Username accepted !!\n"));
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		this.setTitle(chat.source);
	}

	private boolean valid(String userName) {
		for (int i = 0; i < userName.length(); ++i)
			if (!Character.isAlphabetic(userName.charAt(i)))
				return false;
		return true;
	}

	// connect to server
	private void connectToServer() throws IOException {
		showMessage("Attempting connection...\n");
		connection = new Socket(InetAddress.getByName(serverIP), toPort);
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
		showMessage("Please choose a Username!!\n");
		ableToType(true);
		do
			try {
				message = (String) input.readObject();
				showMessage(message + "\n");
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
				destinationName.setEditable(flag);
			}
		});
	}

	private void getMemberList(String message) {
		try {
			output.writeObject(message);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Client("127.0.0.1", 6000).startRunning();
	}
}
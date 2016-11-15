package tschat;

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

import tschat.WindowDestroyer;

@SuppressWarnings("serial")
public class Client extends JFrame {

	private JTextField destinationName;
	private JTextField userText;
	private JTextArea chatWindow;

	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Socket connection;

	private String message = "";
	private String serverIP;
	private static int port = 6000;
	private Chat chat;

	/**
	 * @param host
	 *            IP address of the server
	 */
	public Client(String host , int port) {
		super("Chat");
		this.port = port;
		serverIP = host;

		userText = new JTextField();
		userText.setBackground(new Color(230, 230, 250));

		destinationName = new JTextField();
		destinationName.setBackground(new Color(188, 210, 238));

		chat = new Chat();

		JLabel toLabel = new JLabel("To: ");
		toLabel.setForeground(new Color(204, 51, 0));

		JLabel messageLabel = new JLabel("Message: ");
		messageLabel.setForeground(new Color(204, 51, 0));

		userText.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				chat.destination = destinationName.getText();
				chat.message = event.getActionCommand();
				userText.setText("");
				try {
					output.writeObject(encode(chat));
					chat.TTL = 0;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		JButton btn1 = new JButton("Get active users on this server");
		btn1.setBackground(new Color(19, 38, 57));
		btn1.setForeground(new Color(204, 51, 0));

		btn1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getMemberList("\\getMemberListOfMyServer");
			}
		});
		
		JButton btn2 = new JButton("Get active users of other server");
		btn2.setBackground(new Color(19, 38, 57));
		btn2.setForeground(new Color(204, 51, 0));

		btn2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getMemberList("\\getMemberListOfOtherServer");
			}
		});
		
		JButton btn3 = new JButton("Get all active users");
		btn3.setBackground(new Color(19, 38, 57));
		btn3.setForeground(new Color(204, 51, 0));

		btn3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getMemberList("\\getAllMembers");
			}
		});

		JPanel x = new JPanel(new GridLayout(3, 0));
		x.add(toLabel);
		x.add(messageLabel);

		JPanel y = new JPanel(new BorderLayout());
		y.add(destinationName, BorderLayout.NORTH);
		y.add(userText, BorderLayout.CENTER);

		JPanel w = new JPanel(new GridLayout(3, 0));
		w.add(btn1);
		w.add(btn2);
		w.add(btn3);
		
		JPanel z = new JPanel(new BorderLayout());
		z.add(x, BorderLayout.WEST);
		z.add(y, BorderLayout.CENTER);
		z.add(w, BorderLayout.SOUTH);


		
		chatWindow = new JTextArea();
		chatWindow.setBackground(new Color(19, 38, 57));
		chatWindow.setForeground(new Color(236, 242, 248));
		chatWindow.setEditable(false);

		add(new JScrollPane(chatWindow), BorderLayout.CENTER);
		add(z, BorderLayout.SOUTH);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowDestroyer());
		setBackground(new Color(51, 102, 153));
		setSize(500, 600);
		setBounds(150, 50, 500, 600);
		setVisible(true);
	}

	public void startRunning() {
		try {
			connectToServer();
			setupStreams();
			setupUsername();
			whileChatting();
		} catch (EOFException e) {
			showMessage("Client terminated connection\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();

		}
	}

	private String encode(Chat c) {
		return c.source + "$" + c.destination + "$" + c.TTL + "$" + c.message;
	}

	public void setupUsername() {
		String ob = "";
		try {
			do {
				String s = JOptionPane.showInputDialog("Choose a username");
				if (s == null)
					System.exit(0);
				if (!valid(s)) {
					showMessage("You can use only letters[A - Z].\n");
					continue;
				}
				chat.source = s;
				output.writeObject(s);
				ob = (String) input.readObject();
				showMessage(ob);
			} while (!ob.equals("Username accepted!!"));
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		this.setTitle("Chat | " + chat.source);
	}

	private boolean valid(String userName) {
		for (int i = 0; i < userName.length(); ++i)
			if (!Character.isAlphabetic(userName.charAt(i)))
				return false;
		return true;
	}

	private void connectToServer() throws IOException {
		showMessage("Attempting connection...\n");
		connection = new Socket(InetAddress.getByName(serverIP), port);
		port = port == 6000 ? 6001 : 6000;
		showMessage("Connected to " + connection.getInetAddress().getHostName() + "\n");
	}

	private void setupStreams() throws IOException {
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("Streams are now setup!\nPlease choose a username!\n");
	}

	private void whileChatting() throws IOException {
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
		try {
			output.close();
			input.close();
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	private void showMessage(final String message) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				chatWindow.append(message);
			}
		});
	}

	private void getMemberList(String cmd) {
		try {
			String message = "$$$" + cmd;
			output.writeObject(message);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Client client = new Client("127.0.0.1" , 6000);
		client.startRunning();
	}
}

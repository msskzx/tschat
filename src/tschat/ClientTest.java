package tschat;

public class ClientTest {

	public static void main(String[] args) {
		Client client = new Client("127.0.0.1" , 6001);
		client.startRunning();
	}

}

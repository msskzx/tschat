package m3;

public class Chat{
	String source, destination, message;
	int TTL;
	
	Chat() {
		source = null;
		destination = null;
		message = null;
		TTL = 0;
	}
	
	Chat (String source , String destination , int TTL , String message){
		this.source = source;
		this.destination = destination;
		this.TTL = TTL;
		this.message = message;
	}
}

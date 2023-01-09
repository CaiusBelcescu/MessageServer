public class PrivateMessage extends Message {

    private String receiver;
    private String sender;

    public PrivateMessage(String sender, String receiver, String message) {
        super(message);
        this.receiver = receiver;
        this.sender = sender;
    }
    public String getReceiver(){
        return this.receiver;
    }
    public String getSender() {
        return this.sender;
    }
}

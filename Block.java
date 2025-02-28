import java.util.Date;
import java.util.ArrayList;

public class Block {
    private String MessageHash;
    private String previousMessageHash;
    private String data;
    private long timeStamp;
    private int nonce;

    public Block(String data, String previousMessageHash) {

        this.data = data;
        this.previousMessageHash = previousMessageHash;
        this.timeStamp = new Date().getTime();
        this.MessageHash = cryptoClass.applySha256(previousMessageHash + Long.toString(timeStamp) + Integer.toString(nonce) + data);

    }

    public String getMessageHash() {
        return MessageHash;
    }

    public String getPreviousMessageHash() {
        return previousMessageHash;
    }

    public String getData() {
        return data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getNonce() {
        return nonce;
    }

    

}

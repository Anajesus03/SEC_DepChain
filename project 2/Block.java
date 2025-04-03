import java.util.ArrayList;
import java.util.List;

public class Block {
    private List<Transaction> transactions;
    private String PreviousHash;
    private String Hash;
    private long TimeStamp;

    public Block(String previousHash) {
        this.transactions = new ArrayList<>();
        this.PreviousHash = previousHash;
        this.TimeStamp = System.currentTimeMillis();
        this.Hash = calculateHash();
    }

    public final String calculateHash(){
        return String.valueOf((transactions + PreviousHash + TimeStamp).hashCode());
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> data) {
        this.transactions = data;
        this.Hash = calculateHash(); 

    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        this.Hash = calculateHash(); 
    }

    public String getPreviousHash() {
        return PreviousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.PreviousHash = previousHash;
        this.Hash = calculateHash(); 
    }

    public String getHash() {
        return Hash;
    }

    public void setHash(String hash) {
        this.Hash = hash;
    }

    public long getTimeStamp() {
        return TimeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.TimeStamp = timeStamp;
        this.Hash = calculateHash(); 
    }

}
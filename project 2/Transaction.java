import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class Transaction {
    private String sender; // utilizador
    private String receiver; // smart ou utilizador
    private String amount;
    private String data;
    private String hash; // hash of sender,receiver,amount and data
    private String signature; // signature of the hash


    public Transaction(String sender, String receiver, String amount,String data) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.data = data;
        this.hash = calculateHash();
    }

    private String calculateHash() {
        try {
            String input = this.sender + this.receiver + this.amount + this.data;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing hash", e);
        }
    }

    public void signTransaction(PrivateKey privateKey) {
        try {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privateKey);
            rsa.update(hash.getBytes());
            byte[] signedHash = rsa.sign();
            this.signature = Base64.getEncoder().encodeToString(signedHash);
        } catch (Exception e) {
            throw new RuntimeException("Error signing transaction", e);
        }
    }

    public boolean verifySignature(PublicKey publicKey) {
        try {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(publicKey);
            rsa.update(hash.getBytes());
            return rsa.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    // Getters and setters
    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getAmount() {
        return amount;
    }


    @Override
    public String toString() {
        return sender + " -> " + receiver + " : " + amount + " | " + data;
    }

    
 
   
    
}
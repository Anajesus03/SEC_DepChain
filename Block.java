import java.util.Date;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class Block {
    private String messageHash;          
    private String prevMessageHash;      
    private String data;                 
    private long timeStamp;              
    private String signature;            

    
    public Block(String data, String prevMessageHash, PrivateKey privateKey) throws Exception {
        this.data = data;
        this.prevMessageHash = prevMessageHash;
        this.timeStamp = new Date().getTime();
        this.messageHash = applySha256(prevMessageHash + Long.toString(timeStamp) + data);
        this.signature = signBlock(privateKey);  
    }

    
    public static String applySha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    private String signBlock(PrivateKey privateKey) throws Exception {
        cryptoClass crypto = new cryptoClass(privateKey, null);
        byte[] signatureBytes = crypto.signMessage(messageHash.getBytes());
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    
    public boolean verifyBlockSignature(PublicKey publicKey) throws Exception {
        cryptoClass crypto = new cryptoClass(null, publicKey);
        return crypto.verifySignature(messageHash.getBytes(), Base64.getDecoder().decode(signature));
    }

    
    public String getHash() {
        return messageHash;
    }

    public String getPreviousHash() {
        return prevMessageHash;
    }

    public String getData() {
        return data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getSignature() {
        return signature;
    }
}

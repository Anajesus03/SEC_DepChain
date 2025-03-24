import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class cryptoClass {
    private final KeyPair keyPair;
    private final int SIGNATURE_LENGTH = 256;

    public cryptoClass() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        this.keyPair = keyGen.generateKeyPair();
    }

    public byte[] signMessage(byte[] message) throws Exception {
        if (message == null || message.length == 0) {
            throw new IllegalArgumentException("Message must not be null or empty");
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(message);
        return signature.sign();
    }

    public boolean verifySignature(byte[] message, byte[] signatureBytes,PublicKey publicKey) throws Exception {
        if (message == null || message.length == 0 || signatureBytes == null || signatureBytes.length == 0) {
            throw new IllegalArgumentException("Message and signature must not be null or empty");
        }
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(publicKey);
        sign.update(message);
        return sign.verify(signatureBytes);
    }

    public int getSignatureLength() {
        return SIGNATURE_LENGTH;
    }

     public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public int getPublicKeyLength() {
        return keyPair.getPublic().getEncoded().length;
    }

    public PublicKey getPublicKeyFromBytes(byte[] keyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }
 
}

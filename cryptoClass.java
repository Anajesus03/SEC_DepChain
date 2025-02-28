import java.security.*;


public class cryptoClass {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final int SIGNATURE_LENGTH = 256;

    public cryptoClass(PrivateKey privatekey, PublicKey publickey) {
        if (privatekey == null || publickey == null) {
            throw new IllegalArgumentException("PrivateKey and PublicKey must not be null");
        }
        this.privateKey = privatekey;
        this.publicKey = publickey;
    }

    public byte[] signMessage(byte[] message) throws Exception {
        if (message == null || message.length == 0) {
            throw new IllegalArgumentException("Message must not be null or empty");
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message);
        return signature.sign();
    }

    public boolean verifySignature(byte[] message, byte[] signatureBytes) throws Exception {
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
}

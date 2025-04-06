import java.net.InetAddress;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class conditionalCollect {
    private AuthenticatedPerfectLink apl;
    private boolean isLeader;
    private int N;
    private int f;
    private int LEADERPORT = 5000;

    private boolean collected = false;
    private Map<Integer, String> messages;
    private Map<Integer, byte[]> signatures;
    private Map<Integer, PublicKey> publicKeys;

    private Predicate<Map<Integer, String>> predicateC;
    private Map<Integer, String> collectedMessages;
    

    public conditionalCollect(AuthenticatedPerfectLink apl, boolean isLeader, int N, int f,Predicate<Map<Integer, String>> predicateC) {
        this.apl = apl;
        this.isLeader = isLeader;
        this.N = N;
        this.f = f;
        this.predicateC = predicateC;

        this.messages = new ConcurrentHashMap<>();
        this.signatures = new ConcurrentHashMap<>();
        this.publicKeys = new ConcurrentHashMap<>();
        this.collectedMessages= new ConcurrentHashMap<>();

        
        for (int i = 1; i <= N; i++) {
            messages.put(i, "UNDEFINED");
        }
    }

    //Input from the nodes
    public void inputMessage(int Nodeid, String message) throws Exception {
        System.out.println("[Node " + Nodeid + "] Input message: " + message);
        cryptoClass crypto = apl.getCrypto();

        byte[] payload =("INPUT" + Nodeid + message).getBytes();
        String signature = Base64.getEncoder().encodeToString(crypto.signMessage(payload));
        String publicKey = Base64.getEncoder().encodeToString(crypto.getPublicKey().getEncoded());

        String finalMessage = "SEND," + Nodeid + "," + message + "," + signature + "," + publicKey;
        apl.sendMessage(finalMessage, InetAddress.getLocalHost(), LEADERPORT);

    }

    // Leader receive function
    public void receiveMessage() throws Exception{
        while(!collected){
            String msg = apl.receiveMessage();
            if(msg!=null){
                processMessage(msg);
            }
        }
    }

    public void processMessage(String msg) throws Exception {
        String[] parts = msg.split(",");
        String type = parts[0];
        if (type.equals("SEND")) {
            handleSendMessage(parts);
        } else if (type.equals("COLLECTED") && !collected) {
            if (verifyCollectedMessage(parts)) {
                collected = true;
                int currentNodeId = apl.getNodeId();
                System.out.println("[Node " + currentNodeId + "] " + "Collected messages successfully received.");
                triggerCollectedEvent(parts);
            }
        }
    }

    private void handleSendMessage(String[] parts) throws Exception {
        int nodeId = Integer.parseInt(parts[1]);
        String message = parts[2] + "," + parts[3] + "," + parts[4] +","+ parts[5];
        //System.out.println("[Node  ]" + message);
        String signature = parts[6];
        String publicKey = parts[7];
        //System.out.println("[Node  ]" + publicKey);

        cryptoClass crypto = apl.getCrypto();
        PublicKey publicKeyDecoded = crypto.getPublicKeyFromBytes(Base64.getDecoder().decode(publicKey));
        byte[] signatureDecoded = Base64.getDecoder().decode(signature);
        byte[] payload = ("INPUT" + nodeId + message).getBytes();

        if (crypto.verifySignature(payload, signatureDecoded, publicKeyDecoded)) {
            messages.put(nodeId, message);
            signatures.put(nodeId, signatureDecoded);
            publicKeys.put(nodeId, publicKeyDecoded);
            System.out.println("[Leader] Received message from Node " + nodeId + ": " + message);

            if (isLeader && messages.values().stream().filter(m -> !m.equals("UNDEFINED")).count() >= (N - f) && predicateC.test(messages)) {
                System.out.println("[Leader] Received enough messages to start Collected");
                broadcastCollected();
            }
        }
    }

  
    private void broadcastCollected() throws Exception {
        StringBuilder collectedMessage = new StringBuilder("COLLECTED");
        for (int i = 1; i <= N; i++) {
            if (!messages.get(i).equals("UNDEFINED")) {
                collectedMessage.append(",").append(i)
                        .append(",").append(messages.get(i))
                        .append(",").append(Base64.getEncoder().encodeToString(signatures.get(i)))
                        .append(",").append(Base64.getEncoder().encodeToString(publicKeys.get(i).getEncoded()));
            }
        }

        for (int i = 2; i <= (N); i++) {
            int nodePort = LEADERPORT + (i - 1);
            System.out.println("[Leader] Sending COLLECTED message to Node " + i + " on port " + nodePort);
            apl.sendMessage(collectedMessage.toString(), InetAddress.getLocalHost(), nodePort);
        }

        if(isLeader){
            triggerCollectedEvent(collectedMessage.toString().split(","));
            collected = true;
        }

        messages.clear();
        signatures.clear();
        publicKeys.clear();
    }

 
    private boolean verifyCollectedMessage(String[] parts) throws Exception {
        cryptoClass crypto = apl.getCrypto();
        
        Map<Integer, String> receivedMessages = new ConcurrentHashMap<>();
        for (int i = 1; i < parts.length; i += 7) {
            int nodeId = Integer.parseInt(parts[i]);
            String message = parts[i + 1] + "," + parts[i + 2] + "," + parts[i + 3] + "," + parts[i + 4];	
            byte[] signature = Base64.getDecoder().decode(parts[i + 5]);
            PublicKey publicKey = crypto.getPublicKeyFromBytes(Base64.getDecoder().decode(parts[i + 6]));

            byte[] payload = ("INPUT" + nodeId + message).getBytes();
            if (!crypto.verifySignature(payload, signature, publicKey)) {
                System.out.println("[Node] Invalid signature for Node " + nodeId);
                return false;
            }
            receivedMessages.put(nodeId, message);
        }
        return receivedMessages.size() >= (N - f) && predicateC.test(receivedMessages);
    }


    private void triggerCollectedEvent(String[] parts) {
        for (int i = 1; i < parts.length; i += 7) {
            int nodeId = Integer.parseInt(parts[i]);
            String message = parts[i + 1] + "," + parts[i + 2] + "," + parts[i + 3] + "," + parts[i + 4];
            collectedMessages.put(nodeId, message);
        }
        System.out.println("[Node] Triggering Collected event with messages: " + collectedMessages);
    }

    public boolean isCollected() {
        return collected;
    }

    public Map<Integer, String> getCollectMessages(){
        return this.collectedMessages;
    }

}


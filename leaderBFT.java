import java.net.InetAddress;
import java.security.PublicKey;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class leaderBFT implements Runnable{
    private List<AuthenticatedPerfectLink> apl;
    private String name;
    private List<cryptoClass> servers;
    private List<String> blockchain;
    private conditionalCollect cc;
    private List<InetAddress> serverAddresses;
    private List<Integer> serverPorts;

    private int valts=0; //Timestamp of the current value
    private String val=null;
    private Set<Map.Entry<Integer, String>> writeset;
    
    public leaderBFT(String name,networkClass network, List<cryptoClass> servers, List<InetAddress> serverAddresses, List<Integer> serverPorts) {
        this.serverAddresses = serverAddresses;
        this.serverPorts = serverPorts;
        this.servers = servers;
        this.apl = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            this.apl.add(new AuthenticatedPerfectLink(network, servers.get(i)));
        }
        this.name = name;
        this.blockchain = new ArrayList<>();
        this.writeset=new HashSet<>();

        Map<String, PublicKey> publicKeys = new HashMap<>();
        for(int i=0;i<servers.size(); i++){
            publicKeys.put("server"+i, servers.get(i).getPublicKey());
        }
        this.cc= new conditionalCollect((servers.size()/2) + 1,publicKeys);
        System.out.println("[BFT Leader] Initialized.");
    }

    @Override
    public void run() {
        System.out.println(name + " started.");
        try {
            
                // Collect messages from servers
                for (int i = 0; i < apl.size(); i++) {
                    String message = apl.get(i).receiveMessage();
                    byte[] signature = extractSignature(message); // Extract signature from message
                    String payload = extractPayload(message);    // Extract payload from message
                    String serverId = "server" + i;              // Identify the server
                    cc.collectMessage(serverId, "VALUE", payload, signature);
                }

                // Check if the condition is satisfied
                if (cc.isConditionSatisfied("VALUE")) {
                    Set<Map.Entry<String, byte[]>> messages = cc.getMessages("VALUE");
                    String tmpval = determineValue(messages);
                    if (tmpval != null) {
                        val = tmpval;
                        valts++;
                        blockchain.add(val);
                        writeset.add(new AbstractMap.SimpleEntry<>(valts, val));
                        System.out.println("[Leader] Appended value to blockchain: " + val + " (timestamp: " + valts + ")");
                        broadcastDecision(val);
                        cc.clearCondition("VALUE");
                    }
                }
        } catch (Exception e) {
            System.err.println("[Leader] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Extract signature from the received message
    private byte[] extractSignature(String message) throws Exception {
        String[] parts = message.split("::", 2);
        if (parts.length != 2) {
            throw new Exception("Invalid message format, expected 'payload::signature'");
        }
        return Base64.getDecoder().decode(parts[1]);
    }

    // Extract payload from the received message
    private String extractPayload(String message) throws Exception {
        String[] parts = message.split("::", 2);
        if (parts.length != 2) {
            throw new Exception("Invalid message format, expected 'payload::signature'");
        }
        return parts[0];
    }

    // Decide based on the majority of messages
    private String determineValue(Set<Map.Entry<String, byte[]>> messages) throws Exception {
        Map<String, Integer> frequency = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : messages) {
            String payload = entry.getKey();
            frequency.put(payload, frequency.getOrDefault(payload, 0) + 1);
        }
        String majorityPayload = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                majorityPayload = entry.getKey();
            }
        }
        int majorityThreshold = (servers.size() / 2) + 1;
        if (maxCount >= majorityThreshold) {
            return majorityPayload;
        } else {
            throw new Exception("No majority decision reached");
        }
    }

    // Broadcast the decision to all servers
    private void broadcastDecision(String decision) {
        System.out.println("[Leader] Broadcasting decision: " + decision);
        for (int i = 0; i < serverAddresses.size(); i++) {
            try {
                apl.get(i).sendMessage("DECIDE::" + decision + "::" + valts, serverAddresses.get(i), serverPorts.get(i));
            } catch (Exception e) {
                System.err.println("[Leader] Error broadcasting to server" + i + ": " + e.getMessage());
            }
        }
    }
    
    public List<String> getBlockchain() {
        return blockchain;
    }
    
}

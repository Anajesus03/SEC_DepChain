import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ByzantineEpochConsensus {
    private int nodeId;
    private int N;
    private int f;
    private int ets;
    private boolean isLeader;
    private AuthenticatedPerfectLink apl;
    private conditionalCollect cc;

    private int valts=-1;
    private String val=null;
    private Set<String> writeset;
    private Map<Integer, String> written;
    private Map<Integer, String> accepted;
    private boolean decided = false;
    private int waitAllNodes=0;

    public ByzantineEpochConsensus(int nodeId, int N, int f, int ets, boolean isLeader, AuthenticatedPerfectLink apl,conditionalCollect cc) {
        if (nodeId <= 0 || N <= 0 || f < 0 || ets < 0 || apl == null || cc == null) {
            throw new IllegalArgumentException("Invalid parameters passed to ByzantineEpochConsensus constructor.");
        }
        System.out.println("[Node " + nodeId + "] Initializing Byzantine Epoch Consensus with N=" + N + ", f=" + f + ", ets=" + ets + ", isLeader=" + isLeader);
       
        this.nodeId = nodeId;
        this.N = N;
        this.f = f;
        this.ets = ets;
        this.isLeader = isLeader;
        this.apl = apl;
        this.cc = cc;

        this.writeset = new HashSet<>();
        this.written = new ConcurrentHashMap<>();
        this.accepted = new ConcurrentHashMap<>();


        for (int i = 1; i <= N; i++) {
            written.put(i, "UNDEFINED");
            accepted.put(i, "UNDEFINED");
        }

    }

    //READ PHASE
    @SuppressWarnings("unchecked")
    public void init(Map<String, Object> epochState){
        if(epochState!=null){
           this.valts= (int) epochState.getOrDefault("valts", -1);
           this.val= (String) epochState.getOrDefault("val", null);
           this.writeset = new HashSet<>( (Set<String>) epochState.getOrDefault("writeset", new HashSet<>()));
        }
    }

    //READ PHASE
    public void propose(String v) throws Exception {
        if (!isLeader) {
            throw new IllegalStateException("Only the leader can propose a value.");
        }
        if (val == null) {
            val = v;
        }
        for (int q = 2; q <= N; q++) {
            int nodePort = 5000 + (q - 1);
            apl.sendMessage("READ,", InetAddress.getLocalHost(), nodePort);
        }
    }


    //READ PHASE
    public void handleReadMessage(int senderId) throws Exception {
        if (isLeader) {
            throw new IllegalStateException("Only the leader can propose a value.");
        }

        String stateMessage = String.format("STATE,%d,%s,%s", valts, val, writeset.toString());
        cc.inputMessage(senderId, stateMessage);
    }

    //READ PHASE
    public void handleCollectedStates(Map<Integer, String> states) throws Exception {
        String tmpval = null;

        // Check for a bound value in the collected states
        for (String state : states.values()) {
            if (state != null && state.startsWith("STATE")) {
                String[] parts = state.split(",");
                int ts = Integer.parseInt(parts[1]);
                String v = parts[2];
                if (ts >= 0 && !"null".equals(v)) {
                    tmpval = v;
                    break;
                }
            }
        }

        // If no bound value is found, check for an unbound value in the leader's state
        if (tmpval == null && isLeader) {
            
            String leaderState = states.get(nodeId);
            if (leaderState != null && leaderState.startsWith("STATE")) {
                String[] parts = leaderState.split(",");
                String v = parts[2];
                if (!"null".equals(v)) {
                    tmpval = v;
                }
            }
            
        }

        // If a valid tmpval is found, update writeset and send WRITE messages
        if(isLeader){
            for (int q = 1; q <= (N-1); q++) {
                apl.sendMessage("WRITE,"+ nodeId +"," + tmpval, InetAddress.getLocalHost(), 5000 + q);
                }
            } else{
                apl.sendMessage("WRITE,"+ nodeId +"," + tmpval, InetAddress.getLocalHost(), 5000);
            }
        }
    



    //WRITE PHASE
    public void handleWriteMessage(int senderId, String message) throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        written.put(senderId, message);
        if(isLeader){
            ++waitAllNodes;
        }
        System.out.println("[Node " + nodeId + "] Received WRITE message from Node " + senderId + ": " + message);
        System.out.println("[Node " + nodeId + "] Written messages: " + written);
        int nunmber = countOccurrences(written,message);
        System.out.println("[Node " + nodeId + "] Number of WRITE messages received for value: " + message + " is " + nunmber);
        

        if((countOccurrences(written,message)> ((N+f)/2))&&isLeader&&waitAllNodes==(N-1)){
            valts=ets;
            val=message;
            written.replaceAll((k,oldValue)->"UNDEFINED");
            
            for (int q = 1; q < N; q++) {
                System.out.println("[Node " + nodeId + "] Sending ACCEPT message for value: " + message);
                apl.sendMessage("ACCEPT,"+ nodeId +"," + message, InetAddress.getLocalHost(), 5000 + q);
            }
            //NodeBFT.blockchain.add(message);
            decided = true;
        } else{
            System.out.println("[Node " + nodeId + "] Not enough WRITE messages received for value: " + message);
        }
    }

    //WRITE PHASE
    public void handleAcceptMessage(int senderId, String message) throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }

        accepted.put(senderId, message);
        System.out.println("[Node " + nodeId + "] Received ACCEPT message from Node " + senderId + ": " + message);

        accepted.replaceAll((k,oldValue)->"UNDEFINED");
         for (int q = 1; q <= N; q++) {
                System.out.println("[Node " + nodeId + "] Decided on value: " + message);
        }
        
    }

    private int countOccurrences(Map<Integer, String> map, String value) {
        return (int) map.values().stream().filter(v -> !v.equals("UNDEFINED") && v.equals(value)).count();
    }

    // WRITE PHASE
    public void abort() {
        System.out.println("[Node " + nodeId + "] Aborting epoch " + ets);
        Map<String, Object> epochState = new HashMap<>();
        epochState.put("valts", valts);
        epochState.put("val", val);
        epochState.put("writeset", writeset);
        System.out.println("[Node " + nodeId + "] Aborted with state: " + epochState);
    }

    public String  getLeaderState() {
        if (isLeader) {
            String state = String.format("STATE,%d,%s,%s", valts, val, writeset.toString());
            return state;
        }
        return null;
    }

    public boolean isDecided() {
        return decided;
    }

  
}
class BFTConsensus {
    private Block block;

    public BFTConsensus(Block block) {
        this.block = block;
    }

    public void init() {
        System.out.println("[BFT] Initializing Consensus...");
    }

    public void propose(String request) {
        System.out.println("[BFT] Proposing request: " + request);
    }

    public void decide(String request) {
        System.out.println("[BFT] Consensus reached. Adding to blockchain...");
        this.block.getMessageHash(request);
    }
}
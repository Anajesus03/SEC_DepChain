import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.ArrayList;

public class BlockChain {
    private ArrayList<Block> blockchain;
    private PublicKey leaderPublicKey;

    public BlockChain(PublicKey leaderPublicKey) {
        this.blockchain = new ArrayList<>();
        this.leaderPublicKey = leaderPublicKey;
        blockchain.add(createGenesisBlock());
    }

    // Create Genesis Block
    private Block createGenesisBlock() {
        try {
            return new Block("Genesis Block", "0", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Add a new block to the blockchain
    public boolean appendBlock(Block newBlock) {
        if (isBlockValid(newBlock, blockchain.get(blockchain.size() - 1))) {
            blockchain.add(newBlock);
            System.out.println("Block successfully appended.");
            return true;
        } else {
            System.out.println("Invalid block!");
            return false;
        }
    }

    // Check if a block is valid
    public boolean isBlockValid(Block newBlock, Block previousBlock) {
        try {
            if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) {
                System.out.println("Invalid previous hash.");
                return false;
            }

            if (!newBlock.getHash().equals(Block.applySha256(newBlock.getPreviousHash() + Long.toString(newBlock.getTimeStamp()) + newBlock.getData()))) {
                System.out.println("Invalid block hash.");
                return false;
            }

            if (!newBlock.verifyBlockSignature(leaderPublicKey)) {
                System.out.println("Invalid block signature.");
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if a block exists in the blockchain
    public boolean checkBlock(String blockHash) {
        for (Block block : blockchain) {
            if (block.getHash().equals(blockHash)) {
                return true;
            }
        }
        return false;
    }

    
    public void printBlockchain() {
        for (Block block : blockchain) {
            System.out.println("Block Hash: " + block.getHash());
            System.out.println("Previous Hash: " + block.getPreviousHash());
            System.out.println("Data: " + block.getData());
            System.out.println("Timestamp: " + block.getTimeStamp());
            System.out.println("Signature: " + block.getSignature());
            System.out.println("=====================================");
        }
    }
}

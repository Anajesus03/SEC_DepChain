import java.util.ArrayList;
import java.util.Date;

public class BlockChain {
    public static ArrayList<Block> blockchain = new ArrayList<Block>();

    public static void main(String[] args) {
        blockchain.add(new Block("Genesis Block", "0"));
        System.out.println("Trying to Mine block 1... ");
        blockchain.get(0).mineBlock(5);

        blockchain.add(new Block("Second Block", blockchain.get(blockchain.size() - 1).getMessageHash()));
        System.out.println("Trying to Mine block 2... ");
        blockchain.get(1).mineBlock(5);

        blockchain.add(new Block("Third Block", blockchain.get(blockchain.size() - 1).getMessageHash()));
        System.out.println("Trying to Mine block 3... ");
        blockchain.get(2).mineBlock(5);

        System.out.println("\nBlockchain is Valid: " + isChainValid());

        String blockchainJson = StringUtil.getJson(blockchain);
        System.out.println("\nThe block chain: ");
        System.out.println(blockchainJson);
    }

    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[5]).replace('\0', '0');

        for (int i = 1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);

            if (!currentBlock.getMessageHash().equals(currentBlock.calculateHash())) {
                System.out.println("Current Hashes not equal");
                return false;
            }

            if (!previousBlock.getMessageHash().equals(currentBlock.getPreviousMessageHash())) {
                System.out.println("Previous Hashes not equal");
                return false;
            }

            if (!currentBlock.getMessageHash().substring(0, 5).equals(hashTarget)) {
                System.out.println("This block hasn't been mined");
                return false;
            }
        }
        return true;
    }
}

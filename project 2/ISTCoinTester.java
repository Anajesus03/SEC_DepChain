import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;


public class ISTCoinTester {
    public static void main(String[] args) {
        SimpleWorld world = new SimpleWorld();

        Address sender = Address.fromHexString("0x5B38Da6a701c568545dCfcB03FcB875f56beddC4");
        Address user = Address.fromHexString("0x0000000000000000000000000000000000000001");
        Address contract = Address.fromHexString("0x1234567891234567891234567891234567891234");

        world.createAccount(sender, 0, Wei.fromEth(100));
        world.createAccount(user, 0, Wei.fromEth(50));
        world.createAccount(contract, 0, Wei.ZERO);

        MutableAccount contractAccount = (MutableAccount) world.get(contract);

        // ✅ Manually set the contract's owner to `sender`
        String paddedSender = padHexStringTo256Bit(sender.toHexString());

        // Contract bytecode (ISTCoin)

        String bytecodeHex = "";
        try {
            bytecodeHex = new String(Files.readAllBytes(Paths.get("bytecode.txt"))).trim();
        } catch (IOException e) {
            System.err.println("❌ Error reading bytecode file: " + e.getMessage());
            return; // or System.exit(1); if you want to stop execution
        }

        Bytes bytecode = Bytes.fromHexString(bytecodeHex);

        // Setup Tracer for debugging
        ByteArrayOutputStream traceOutput = new ByteArrayOutputStream();
        PrintStream traceStream = new PrintStream(traceOutput);
        StandardJsonTracer tracer = new StandardJsonTracer(traceStream, true, true, true, true);

        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
                .tracer(tracer)
                .sender(sender)
                .receiver(contract)
                .worldUpdater(world.updater())
                .code(bytecode)
                .gas(5_000_000);

        executor.execute();          // ✅ Executes constructor code
        executor.commitWorldState(); // ✅ THEN commit state
        System.out.println("Owner slot: " + world.get(contract).getStorageValue(UInt256.ZERO));
        

        // ------------------------------
        // 1. Add user to blacklist
        // ------------------------------
        String addToBlacklistSelector = "0x" + getSelector("addToBlacklist(address)");
        String paddedUser = padHexStringTo256Bit(user.toHexString());
        Bytes addCall = Bytes.fromHexString(addToBlacklistSelector + paddedUser);
        executor.callData(addCall);
        executor.execute();
        executor.commitWorldState();

        // ------------------------------
        // 2. Try to transfer tokens to blacklisted user
        // ------------------------------
        String transferSelector = "0x" + getSelector("transfer(address,uint256)");
        String transferData = transferSelector + paddedUser + String.format("%064x", BigInteger.valueOf(100));
        Bytes transferCall = Bytes.fromHexString(transferData);
        executor.callData(transferCall);

        try {
            executor.execute();
            System.out.println("✅ Transfer executed (unexpected, blacklist failed)");
        } catch (Exception e) {
            System.out.println("❌ Transfer reverted as expected (blacklist worked)");
        }

        // ------------------------------
        // 3. Check blacklist mapping slot
        // ------------------------------
        String slot = computeMappingSlot(user.toHexString(), 1); // slot 1: blacklisted mapping
        UInt256 slotHash = UInt256.fromHexString(slot);
        var storageValue = world.get(contract).getStorageValue(slotHash);
        System.out.println("Blacklist mapping slot value: " + storageValue);

        // ------------------------------
        // 4. Balances
        // ------------------------------
        System.out.println("Sender balance: " + world.get(sender).getBalance());
        System.out.println("User balance: " + world.get(user).getBalance());
    }

    public static String getSelector(String functionSignature) {
        byte[] hash = Hash.sha3(functionSignature.getBytes());
        return Numeric.toHexStringNoPrefix(Arrays.copyOfRange(hash, 0, 4));
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) hexString = hexString.substring(2);
        return String.format("%064x", new BigInteger(hexString, 16));
    }

    public static String computeMappingSlot(String addressHex, int slotNumber) {
        String paddedAddress = padHexStringTo256Bit(addressHex);
        String slotHex = String.format("%064x", slotNumber);
        String combined = paddedAddress + slotHex;
        return Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(combined)));
    }
}


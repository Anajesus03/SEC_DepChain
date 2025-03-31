import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;

import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import java.io.*;
import java.nio.file.*;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;



import java.math.BigInteger;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

public class Blacklist {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("bytecode.txt");
        String bytecodeHex = Files.readString(path).trim();

        SimpleWorld simpleWorld = new SimpleWorld();

        Address senderAddress = Address.fromHexString("5B38Da6a701c568545dCfcB03FcB875f56beddC4");
        simpleWorld.createAccount(senderAddress,0, Wei.fromEth(100));
        MutableAccount senderAccount = (MutableAccount) simpleWorld.get(senderAddress);
        System.out.println("Sender Account");
        System.out.println("  Address: "+senderAccount.getAddress());
        System.out.println("  Balance: "+senderAccount.getBalance());
        System.out.println("  Nonce: "+senderAccount.getNonce());
        System.out.println();

        Address contractAddress = Address.fromHexString("1234567891234567891234567891234567891234");
        simpleWorld.createAccount(contractAddress,0, Wei.fromEth(0));
        MutableAccount contractAccount = (MutableAccount) simpleWorld.get(contractAddress);
        System.out.println("Contract Account");
        System.out.println("  Address: "+contractAccount.getAddress());
        System.out.println("  Balance: "+contractAccount.getBalance());
        System.out.println("  Nonce: "+contractAccount.getNonce());
        System.out.println("  Storage:");
        String paddedAddress = padHexStringTo256Bit(senderAddress.toHexString());
        String stateVariableIndex = convertIntegerToHex256Bit(0);
        String storageSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + stateVariableIndex)));
        System.out.println("    Slot SHA3[msg.sender||0] (mapping): "+simpleWorld.get(contractAddress).getStorageValue(UInt256.fromHexString(storageSlotMapping)));
        System.out.println();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        executor.code(Bytes.fromHexString(bytecodeHex));
        executor.sender(senderAddress);
        executor.receiver(contractAddress);
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();

        String addressHex = "feedfacefeedfacefeedfacefeedfacefeedface";
        String arg = String.format("%064x", new BigInteger(addressHex, 16));

        executor.callData(Bytes.fromHexString("71a2c180"+ arg));
        executor.execute();
        String isBlacklisted = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted(feedface...): " + isBlacklisted);

        executor.callData(Bytes.fromHexString("44337ea1" + arg));
        executor.execute();
        String enter = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("addToBlackList(feedface...): " + enter);

        System.out.println("Contract Account");
        System.out.println("  Address: "+contractAccount.getAddress());
        System.out.println("  Balance: "+contractAccount.getBalance());
        System.out.println("  Nonce: "+contractAccount.getNonce());
        System.out.println("  Storage:");
        System.out.println("    Slot SHA3[msg.sender||0] (mapping): "+simpleWorld.get(contractAddress).getStorageValue(UInt256.fromHexString(storageSlotMapping)));
        System.out.println();

        executor.callData(Bytes.fromHexString("71a2c180"+ arg));
        executor.execute();
        isBlacklisted = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted(feedface...): " + isBlacklisted);

        executor.callData(Bytes.fromHexString("44337ea1" + arg));
        executor.execute();
        enter = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("addToBlackList(feedface...): " + enter);

        executor.callData(Bytes.fromHexString("45773e4e"));

        executor.execute();

        String string = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("Output string of 'sayHelloWorld():' " + string);

    }

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length-1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size()-1).getAsString());
        int size = Integer.decode(stack.get(stack.size()-2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        int stringOffset = Integer.decode("0x"+returnData.substring(0, 32 * 2));
        int stringLength = Integer.decode("0x"+returnData.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
        String hexString = returnData.substring(stringOffset * 2 + 32 * 2, stringOffset * 2 + 32 * 2 + stringLength * 2);

        return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    public static boolean extractBooleanFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
    
        String memory = jsonObject.get("memory").getAsString().replace("0x", "");
    
        // Read the first 32 bytes of return data (expected for bool return)
        if (memory.length() < 64) {
            return false; // invalid memory output
        }
    
        String boolData = memory.substring(0, 64); // First 32 bytes
        return new BigInteger(boolData, 16).compareTo(BigInteger.ZERO) != 0;
    }
    

    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode("0x"+returnData);
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            int value = Integer.parseInt(hexString.substring(i, i + 2), 16);
            byteArray[i / 2] = (byte) value;
        }

        return byteArray;
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }


}
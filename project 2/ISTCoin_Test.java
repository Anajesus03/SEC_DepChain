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

public class ISTCoin_Test {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("deployBlackList.txt");
        String deployBlackList = Files.readString(path).trim();

        path = Paths.get("deployISTCoin.txt");
        String deployISTCoin = Files.readString(path).trim();

        path = Paths.get("runtimeBlackList.txt");
        String runtimeBlackList = Files.readString(path).trim();

        path = Paths.get("runtimeISTCoin.txt");
        String runtimeISTCoin = Files.readString(path).trim();

        SimpleWorld simpleWorld = new SimpleWorld();

        Address senderAddress = Address.fromHexString("5B38Da6a701c568545dCfcB03FcB875f56beddC4");
        simpleWorld.createAccount(senderAddress,0, Wei.fromEth(100));
        MutableAccount senderAccount = (MutableAccount) simpleWorld.get(senderAddress);
        System.out.println("Sender Account");
        System.out.println("  Address: "+senderAccount.getAddress());
        System.out.println("  Balance: "+senderAccount.getBalance());
        System.out.println("  Nonce: "+senderAccount.getNonce());
        System.out.println();

        Address receiverAddress = Address.fromHexString("feedfacefeedfacefeedfacefeedfacefeedface");
        simpleWorld.createAccount(receiverAddress,0, Wei.fromEth(0));
        MutableAccount receiverAccount = (MutableAccount) simpleWorld.get(receiverAddress);
        System.out.println("Receiver Account");
        System.out.println("  Address: "+receiverAccount.getAddress());
        System.out.println("  Balance: "+receiverAccount.getBalance());
        System.out.println("  Nonce: "+receiverAccount.getNonce());
        System.out.println();

        Address ISTCoinContractAddress = Address.fromHexString("9876543210987654321098765432109876543210");
        simpleWorld.createAccount(ISTCoinContractAddress,0, Wei.fromEth(0));
        MutableAccount contractAccountIST = (MutableAccount) simpleWorld.get(ISTCoinContractAddress);
        System.out.println("ISTCoin Contract Account");
        System.out.println("  Address: "+contractAccountIST .getAddress());
        System.out.println("  Balance: "+contractAccountIST .getBalance());
        System.out.println("  Nonce: "+contractAccountIST .getNonce());
        System.out.println();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        
        executor.code(Bytes.fromHexString(deployISTCoin));
        executor.sender(senderAddress);
        executor.receiver(receiverAddress);
        System.out.println("Deploying ISTCoin Contract");
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

        String receiverHex = receiverAddress.toHexString().substring(2);

        executor.code(Bytes.fromHexString(runtimeISTCoin));
        executor.callData(Bytes.fromHexString("71a2c180"+ padHexStringTo256Bit(receiverHex)));
        executor.execute();
        String isBlacklisted = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted(feedface...): " + isBlacklisted);

        executor.code(Bytes.fromHexString(runtimeISTCoin));
        executor.callData(Bytes.fromHexString("44337ea1" + padHexStringTo256Bit(receiverHex)));
        executor.execute();
        String enter = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("addToBlackList(feedface...): " + enter);

        executor.code(Bytes.fromHexString(runtimeISTCoin));
        executor.callData(Bytes.fromHexString("71a2c180"+ padHexStringTo256Bit(receiverHex)));
        executor.execute();
        isBlacklisted = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted(feedface...): " + isBlacklisted);

        executor.code(Bytes.fromHexString(runtimeISTCoin));;
        String paddedAmount = padHexStringTo256Bit("0x2710");
        String callData = "a9059cbb" + padHexStringTo256Bit(receiverHex) + paddedAmount;
        executor.callData(Bytes.fromHexString(callData));
        executor.execute();
        analyzeIstCoinResult(byteArrayOutputStream);

        executor.code(Bytes.fromHexString(runtimeISTCoin));
        executor.callData(Bytes.fromHexString("537df3b6" + padHexStringTo256Bit(receiverHex)));
        executor.execute();
        enter = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("removeFromBlacklist(address): " + enter);

        executor.code(Bytes.fromHexString(runtimeISTCoin));
        paddedAmount = padHexStringTo256Bit("0x2710");
        callData = "a9059cbb" + padHexStringTo256Bit(receiverHex)+ paddedAmount;
        executor.callData(Bytes.fromHexString(callData));
        executor.execute();
        analyzeIstCoinResult(byteArrayOutputStream);
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


    public static void analyzeIstCoinResult(ByteArrayOutputStream byteArrayOutputStream) {
        String output = byteArrayOutputStream.toString();
        String[] lines = output.trim().split("\n");
        if (lines.length == 0) {
            System.out.println("No output to analyze.");
        } else {
            String lastLine = lines[lines.length - 1];
            if (lastLine.contains("\"opName\":\"REVERT\"")) {
                System.out.println("Failed to do transaction, someone is on a Blacklist");
            } else if (lastLine.contains("\"opName\":\"RETURN\"")) {
                System.out.println("Transaction passed in ISTCoin, no one is in BlackList");
            } else {
                System.out.println("Unexpected EVM result");
            }
        }
    }
}
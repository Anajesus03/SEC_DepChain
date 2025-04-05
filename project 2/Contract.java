import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import org.apache.tuweni.bytes.Bytes;

import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

import java.math.BigInteger;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;


public class Contract {

    static Address senderAddress;
    static Address ISTCoinContractAddress;
    static Address recipientAddress;
    static SimpleWorld simpleWorld;
    static EVMExecutor executor;
    static MutableAccount istCoinAccount;
    static ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    static Block block;


    public Contract() throws IOException {
        block = new Block("7c41e7b1a3d8f9b4fc7286c58e4d5f7e8e7e2b1f9c4a7d3f0b1e6a9278c5d4ea\n");
        Path genesisPath = Paths.get("genesis.json");
        String genesisJson = Files.readString(genesisPath);
        JsonObject genesis = JsonParser.parseString(genesisJson).getAsJsonObject();

        simpleWorld = new SimpleWorld();
        JsonObject state = genesis.getAsJsonObject("state");

        for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
            String addr = entry.getKey();
            JsonObject account = entry.getValue().getAsJsonObject();
            Address address = Address.fromHexString(addr);

            Wei balance = Wei.of(new BigInteger(account.get("balance").getAsString())); 
            simpleWorld.createAccount(address, 0, balance);
            MutableAccount acc = (MutableAccount) simpleWorld.get(address);

            if (account.has("nonce")) {
                acc.setNonce(account.get("nonce").getAsInt());
            }

            if (account.has("code")) {
                acc.setCode(Bytes.fromHexString(account.get("code").getAsString()));
            }
            // Assign key addresses for later use
            if (addr.equalsIgnoreCase("0x5B38Da6a701c568545dCfcB03FcB875f56beddC4")) {
                senderAddress = address;
            } else if (addr.equalsIgnoreCase("0x9876543210987654321098765432109876543210")) {
                ISTCoinContractAddress = address;
            } else if (addr.equalsIgnoreCase("0xfeedfacefeedfacefeedfacefeedfacefeedface")) {
                recipientAddress = address;
            }
        }
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);

        String addressHex = recipientAddress.toHexString().substring(2);
        String arg = String.format("%064x", new BigInteger(addressHex, 16));

        Bytes deployISTCoin = simpleWorld.get(ISTCoinContractAddress).getCode();
        executor.code(deployISTCoin);
        executor.worldUpdater(simpleWorld.updater());
        executor.sender(senderAddress);
        executor.receiver(ISTCoinContractAddress);
        executor.execute();
 
        String runtimeISTCoin = extractRuntimeFromReturnData(byteArrayOutputStream);
        Bytes istCoinRuntime = Bytes.fromHexString(runtimeISTCoin);
        istCoinAccount = (MutableAccount) simpleWorld.get(ISTCoinContractAddress);
        istCoinAccount.setCode(istCoinRuntime);
    }

    public static void isBlacklisted(Address address) {
        MutableAccount recipientAccount = (MutableAccount) simpleWorld.get(address);
        String addressHex = recipientAccount.getAddress().toHexString().substring(2);
        String arg = String.format("%064x", new BigInteger(addressHex, 16));
        executor.code(istCoinAccount.getCode());
        Bytes call = Bytes.fromHexString("71a2c180" + arg);
        executor.callData(call);
        executor.execute();
        String result = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted: " + result);
    }

    public static void addToBlacklist(Address address) {
        MutableAccount recipientAccount = (MutableAccount) simpleWorld.get(address);
        String addressHex = recipientAccount.getAddress().toHexString().substring(2);
        String arg = String.format("%064x", new BigInteger(addressHex, 16));
        executor.code(istCoinAccount.getCode());
        Bytes call = Bytes.fromHexString("44337ea1" + arg);
        executor.callData(call);
        executor.execute();
        String result = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("addToBlacklist: " + result);
    }

    public static void removeFromBlacklist(Address address) {
        MutableAccount recipientAccount = (MutableAccount) simpleWorld.get(address);
        String addressHex = recipientAccount.getAddress().toHexString().substring(2);
        String arg = String.format("%064x", new BigInteger(addressHex, 16));
        executor.code(istCoinAccount.getCode());
        Bytes call = Bytes.fromHexString("537df3b6" + arg);
        executor.callData(call);
        executor.execute();
        String result = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("removeFromBlacklist: " + result);
    }

    public static void transfer(Address from, Address to, String hexAmount) {
        MutableAccount recipientAccount = (MutableAccount) simpleWorld.get(to);
        String paddedAmount = padHexStringTo256Bit(hexAmount);
        Address Recipient = recipientAccount.getAddress();
        String addressHex =  Recipient.toHexString().substring(2);
        String arg = String.format("%064x", new BigInteger(addressHex, 16));

        if (!recipientAccount.getCode().isEmpty()) {
            String callData = "a9059cbb" + arg + paddedAmount;
            executor.code(istCoinAccount.getCode());
            executor.sender(from);
            executor.receiver(to);
            executor.callData(Bytes.fromHexString(callData));
            executor.execute();

            if (analyzeIstCoinResult(byteArrayOutputStream)) {
                Transaction tx = new Transaction(from.toHexString(), to.toHexString(), paddedAmount, callData);
                block.addTransaction(tx);
                getSenderBalance();
                getISTCoinBalance();
                System.out.println(tx);
                System.out.println("ISTCoin transaction completed.");
            }
        } else {
            System.out.println("Not ISTCoin, using DeepCoin.");
            MutableAccount senderAcc = (MutableAccount) simpleWorld.get(from);
            senderAcc.setBalance(senderAcc.getBalance().subtract(Wei.of(new BigInteger(paddedAmount, 16))));
            recipientAccount.setBalance(recipientAccount.getBalance().add(Wei.of(new BigInteger(paddedAmount, 16))));
            Transaction tx = new Transaction(from.toHexString(), to.toHexString(), paddedAmount, "");
            block.addTransaction(tx);
            getSenderBalance();
            getReceiverBalance();
            System.out.println(tx);
            System.out.println("DeepCoin transaction completed.");
        }
    }

    public static String getClientAddress() {
        return senderAddress.toHexString();
    }
    public static String getISTCoinContractAddress() {
        return ISTCoinContractAddress.toHexString();
    }
    public static String getReceiverAddress() {
        return recipientAddress.toHexString();
    }

    public static String getData(String arg, String hexAmount){
        Address address = Address.fromHexString(arg);
        MutableAccount recipientAccount = (MutableAccount) simpleWorld.get(address);
        String paddedAmount = padHexStringTo256Bit(hexAmount);
        arg = String.format("%064x", new BigInteger(address.toHexString().substring(2), 16));
        if (!recipientAccount.getCode().isEmpty()) {
            String callData = "a9059cbb" + arg + paddedAmount;
            return callData;
        } else {
            System.out.println("Not ISTCoin, using DeepCoin.");
            return "";
        }
    }

    public static void getSenderBalance(){
        Address address = Address.fromHexString(senderAddress.toHexString());
        MutableAccount senderAccount = (MutableAccount) simpleWorld.get(address);
        System.out.println("Sender Account Balance "+ senderAccount.getBalance().toString());
    }
    public static void getReceiverBalance(){
        Address address = Address.fromHexString(recipientAddress.toHexString());
        MutableAccount recipientAccount = (MutableAccount) simpleWorld.get(address);
        System.out.println("Receiver Account Balance "+ recipientAccount.getBalance().toString());
    }
    public static void getISTCoinBalance(){
        Address address = Address.fromHexString(ISTCoinContractAddress.toHexString());
        MutableAccount istCoinAccount = (MutableAccount) simpleWorld.get(address);
        System.out.println("ISTCoin Account Balance "+ istCoinAccount.getBalance().toString());
    }

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\r?\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        int stringOffset = Integer.decode("0x" + returnData.substring(0, 64));
        int stringLength = Integer.decode("0x" + returnData.substring(stringOffset * 2, stringOffset * 2 + 64));
        String hexString = returnData.substring(stringOffset * 2 + 64, stringOffset * 2 + 64 + stringLength * 2);
        return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    public static boolean extractBooleanFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\r?\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        String memory = jsonObject.get("memory").getAsString().replace("0x", "");
        if (memory.length() < 64) return false;
        String boolData = memory.substring(0, 64);
        return new BigInteger(boolData, 16).compareTo(BigInteger.ZERO) != 0;
    }

    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\r?\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode("0x" + returnData);
    }

    public static String extractRuntimeFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\r?\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return returnData;
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
        if (length >= targetLength) return hexString.substring(0, targetLength);
        return "0".repeat(targetLength - length) + hexString;
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);
        return String.format("%064x", bigInt);
    }
    public static Boolean analyzeIstCoinResult(ByteArrayOutputStream byteArrayOutputStream) {
        String output = byteArrayOutputStream.toString();
        String[] lines = output.trim().split("\n");
        if (lines.length == 0) {
            System.out.println("No output to analyze.");
            return false;
        } else {
            String lastLine = lines[lines.length - 1];
            if (lastLine.contains("\"opName\":\"REVERT\"")) {
                System.out.println("Failed to do transaction, someone is on a Blacklist");
                return false;
            } else if (lastLine.contains("\"opName\":\"RETURN\"")) {
                System.out.println("Transaction passed in ISTCoin, no one is in BlackList");
                return true;
            } else {
                System.out.println("Unexpected EVM result");
                return false;
            }
        }
    }
}

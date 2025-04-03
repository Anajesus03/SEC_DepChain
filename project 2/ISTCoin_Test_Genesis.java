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

public class ISTCoin_Test_Genesis {

    static List<JsonObject> transactionLog = new ArrayList<>();
    static Set<Address> knownAddresses = new HashSet<>();

    public static void main(String[] args) throws IOException {
        Path genesisPath = Paths.get("genesis.json");
        String genesisJson = Files.readString(genesisPath);
        JsonObject genesis = JsonParser.parseString(genesisJson).getAsJsonObject();

        SimpleWorld simpleWorld = new SimpleWorld();
        JsonObject state = genesis.getAsJsonObject("state");

        Address senderAddress = null;
        Address ISTCoinContractAddress = null;
        Address recipientAddress = null;

        for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
            String addr = entry.getKey();
            JsonObject account = entry.getValue().getAsJsonObject();
            Address address = Address.fromHexString(addr);

            if (knownAddresses.contains(address)) continue;
            knownAddresses.add(address);

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

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);

        String addressHex = recipientAddress.toHexString().substring(2);
        String arg = String.format("%064x", new BigInteger(addressHex, 16));

        Path deployPath = Paths.get("deployISTCoin.txt");
        String deployISTCoin = Files.readString(deployPath).trim();
        executor.code(Bytes.fromHexString(deployISTCoin));
        executor.worldUpdater(simpleWorld.updater());
        executor.sender(senderAddress);
        executor.receiver(ISTCoinContractAddress);
        executor.execute();

        Path runtimePath = Paths.get("runtimeISTCoin.txt");
        String runtimeISTCoin = Files.readString(runtimePath).trim();
        Bytes istCoinRuntime = Bytes.fromHexString(runtimeISTCoin);


        executor.code(istCoinRuntime);
        Bytes call0 = Bytes.fromHexString("71a2c180" + arg);
        executor.callData(call0);
        executor.execute();
        String isBlacklisted = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted(feedface...): " + isBlacklisted);

        executor.code(istCoinRuntime);
        Bytes call1 = Bytes.fromHexString("44337ea1" + arg);
        executor.callData(call1);
        executor.execute();
        String enter = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("addToBlackList(feedface...): " + enter);

        executor.code(istCoinRuntime);
        Bytes call2 = Bytes.fromHexString("71a2c180" + arg);
        executor.callData(call2);
        executor.execute();
        isBlacklisted = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("isBlacklisted(feedface...): " + isBlacklisted);

        String paddedAmount = padHexStringTo256Bit("0x2710");
        String callData = "a9059cbb" + arg + paddedAmount;
        executor.code(istCoinRuntime);
        Bytes call3 = Bytes.fromHexString(callData);
        executor.callData(call3);
        executor.execute();
        analyzeIstCoinResult(byteArrayOutputStream);

        executor.code(istCoinRuntime);
        Bytes call4 = Bytes.fromHexString("537df3b6" + arg);
        executor.callData(call4);
        executor.execute();
        enter = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("removeFromBlacklist(address): " + enter);

        executor.code(istCoinRuntime);
        executor.callData(call3);
        executor.execute();
        analyzeIstCoinResult(byteArrayOutputStream);
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

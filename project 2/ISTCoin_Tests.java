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

public class ISTCoin_Tests {

    public static void main(String[] args) throws Exception {
        Contract contract = new Contract();
        String ISTCoinContractAddressHex = contract.getISTCoinContractAddress();
        Address ISTCoinContractAddress = Address.fromHexString(ISTCoinContractAddressHex);
        String senderAddressHex = contract.getClientAddress();
        Address senderAddress = Address.fromHexString(senderAddressHex);
        String recipientAddressHex = contract.getReceiverAddress();
        Address recipientAddress = Address.fromHexString(recipientAddressHex);

        System.out.println("ISTCoin Contract Address: " + ISTCoinContractAddress.toHexString());
        System.out.println("Owner Address: " + senderAddress.toHexString());
        System.out.println("Client Address: " + recipientAddress.toHexString());

        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Choose an action ===");
        System.out.println("1 - Transfer from Owner to Client");
        System.out.println("2 - Transfer from Client to Owner");
        System.out.println("3 - Transfer from Owner to Contract");
        System.out.println("4 - Check if Owner is Blacklisted");
        System.out.println("5 - Check if Client is Blacklisted");
        System.out.println("6 - Check if Contract is Blacklisted");
        System.out.println("7 - Add Owner to Blacklist");
        System.out.println("8 - Add Client to Blacklist");
        System.out.println("9 - Add Contract to Blacklist");
        System.out.println("10 - Remove Owner from Blacklist");
        System.out.println("11 - Remove Client from Blacklist");
        System.out.println("12 - Remove Contract from Blacklist");
        System.out.println("13 - Print Commands Lines");
        System.out.println("14 - Exit");

        while (true) {
            System.out.print("Enter your choice (1–14): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1 -> contract.transfer(senderAddress, recipientAddress, "0x100");
                    case 2 -> contract.transfer(recipientAddress, senderAddress, "0x200");
                    case 3 -> contract.transfer(senderAddress, ISTCoinContractAddress, "0x300");
                    case 4 -> contract.isBlacklisted(senderAddress);
                    case 5 -> contract.isBlacklisted(recipientAddress);
                    case 6 -> contract.isBlacklisted(ISTCoinContractAddress);
                    case 7 -> contract.addToBlacklist(senderAddress);
                    case 8 -> contract.addToBlacklist(recipientAddress);
                    case 9 -> contract.addToBlacklist(ISTCoinContractAddress);
                    case 10 -> contract.removeFromBlacklist(senderAddress);
                    case 11 -> contract.removeFromBlacklist(recipientAddress);
                    case 12 -> contract.removeFromBlacklist(ISTCoinContractAddress);
                    case 13 -> printCommandMenu();
                    case 14 -> {
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please enter 1 to 14.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printCommandMenu() {
        System.out.println("Command lines:");
        System.out.println("1 - Transfer from Owner to Client");
        System.out.println("2 - Transfer from Client to Owner");
        System.out.println("3 - Transfer from Owner to Contract");
        System.out.println("4 - Check if Owner is Blacklisted");
        System.out.println("5 - Check if Client is Blacklisted");
        System.out.println("6 - Check if Contract is Blacklisted");
        System.out.println("7 - Add Owner to Blacklist");
        System.out.println("8 - Add Client to Blacklist");
        System.out.println("9 - Add Contract to Blacklist");
        System.out.println("10 - Remove Owner from Blacklist");
        System.out.println("11 - Remove Client from Blacklist");
        System.out.println("12 - Remove Contract from Blacklist");
        System.out.println("13 - Print Commands Lines");
        System.out.println("14 - Exit");
    }
}

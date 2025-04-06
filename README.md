# DepChain Project

## Overview
DepChain is a permissioned blockchain system designed to ensure high dependability guarantees. This project focuses on implementing a reliable consensus mechanism based on the Byzantine Read/Write Epoch Consensus algorithm.

## Keywords
- **Authenticated Perfect Link**
- **Signed Conditional Collect**
- **Leader**
- **Byzantine Consensus**
- **Fault Tolerance**

## Project Structure
```
SEC_DepChain/
└── project_2/
|   ├── Contracts/
│   |   ├── BlackList.sol
│   |   └── ISTCoin.sol                
│   │   
│   ├── jars/                 # JARs you import
│   │   └── *.jar
|   |
|   |── Transaction/                 # Transactions occured
│   │   └── all_transactions.json
|   |
│   ├── AuthenticatedPerfectLink.java
│   ├── BFTests.java
│   ├── Block.java
│   ├── ByzantineEpochConsensus.java
│   ├── ClientBFT.java
│   ├── conditionalCollect.java
│   ├── Contract.java
│   ├── cryptoClass.java
│   ├── genesis.json
│   ├── ISTCoin_Tests.java
│   ├── networkClass.java
│   ├── NodeBFT.java
│   └── Transaction.java
└── project_1/
|   └── Project1 Logic.
├── README.md
```

## Prerequisites
Ensure you have the following installed before running the project:
- **Java 17**

## Dependencies
The project uses the following dependencies:
- **JUnit 5** for unit testing
- **Maven Compiler Plugin** for Java 17 compilation

## Project 2
- **Linux**:
    - Compile Code: javac -cp ".:<Path_to_File>/SEC_DepChain/project 2/jars/*" *.java 
    - Execute BFTest: java -cp ".:<Path_to_File>/SEC_DepChain/project 2/jars/*" BFTest  #(Inside BFTest when creating processes for NodeBFT, you have to put the path of jars in line 81.)
    - Execute ISTCoin_Tests: java -cp ".:<Path_to_File>/SEC_DepChain/project 2/jars/*" ISTCoin_Tests
- **Windows**:
    - Compile Code: javac -cp ".:<Path_to_File>/SEC_DepChain/project 2/jars/*;." *.java 
    - Execute BFTest: java -cp ".:<Path_to_File>/SEC_DepChain/project 2/jars/*;." BFTest  #(Inside BFTest when creating processes for NodeBFT, you have to put the path of jars in line 81.)
    - Execute ISTCoin_Tests: java -cp ".:<Path_to_File>/SEC_DepChain/project 2/jars/*;." ISTCoin_Tests

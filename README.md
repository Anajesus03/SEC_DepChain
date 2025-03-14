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
SEC_DECHAIN/
│── src/
│   ├── main/java/
│   │   ├── AuthenticatedPerfectLink.java
│   │   ├── BFTConsensus.java
│   │   ├── Client.java
│   │   ├── ClientBFT.java
│   │   ├── cryptoClass.java
│   │   ├── KeyGeneratorUtil.java
│   │   ├── LeaderBFT.java
│   │   ├── networkClass.java
│   │   ├── ServerBFT.java
│   ├── test/java/
│   │   ├── AuthenticatedPerfectLinkTest.java
│   │   ├── BFTTest.java
│   │   ├── ConditionalCollectTest.java
│── target/
│── pom.xml
│── README.md
```

## Prerequisites
Ensure you have the following installed before running the project:
- **Java 17**
- **Apache Maven** (latest version recommended)

## Building the Project
To compile the project, use the following Maven command:
```sh
mvn clean compile
```

## Running the Tests
This project includes unit tests written with JUnit 5. Run them using:
```sh
mvn test
```

## Dependencies
The project uses the following dependencies:
- **JUnit 5** for unit testing
- **Maven Compiler Plugin** for Java 17 compilation












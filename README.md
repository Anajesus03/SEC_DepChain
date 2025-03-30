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
│   │   ├── AuthenticatedPerfectLinkTest.java
│   │   ├── Client.java
│   │   ├── conditionalCollect.java
│   │   ├── cryptoClass.java
│   │   ├── KeyGeneratorUtil.java
│   │   ├── LeaderBFT.java
│   │   ├── networkClass.java
│   │   ├── ServerBFT.java
│   ├── test/java/
│   │   ├── BFTTest.java
│   │   ├── ConditionalCollectTest.java
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
This project includes unit tests written with JUnit 5, BFTTest.java and ConditionalCollectTest.java Run them using:
```sh
mvn test
```
To run AuthenticatedPerfectLinkTest simply go to the folder of src/main/java and run the commands:

```sh
javac AuthenticatedPerfectLinkTest.java 
```

followed by:

```sh
java AuthenticatedPerfectLinkTest.java 
```

## Dependencies
The project uses the following dependencies:
- **JUnit 5** for unit testing
- **Maven Compiler Plugin** for Java 17 compilation

## Project 2
- Compilar Main:javac -cp ".:/home/rodrigo/Documents/Faculdade/SEC/lab/lab2/jars/*" Main.java
- Correr Main: java -cp ".:/home/rodrigo/Documents/Faculdade/SEC/lab/lab2/jars/*" Main

public class TestProcessesRS {
    public static void main(String[] args) throws Exception {
        // Start the receiver process
        ProcessBuilder receiverProcessBuilder = new ProcessBuilder("java", "receiver");
        Process receiverProcess = receiverProcessBuilder.inheritIO().start();

        // Wait for the receiver to start up
        Thread.sleep(1000);

        // Start the sender process
        ProcessBuilder senderProcessBuilder = new ProcessBuilder("java", "Sender");
        Process senderProcess = senderProcessBuilder.inheritIO().start();

        // Wait for the sender process to finish
        senderProcess.waitFor();

        // Optionally, wait for the receiver process to finish
        receiverProcess.waitFor();
    }
}

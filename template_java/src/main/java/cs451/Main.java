package cs451;

import cs451.hosting.Host;
import cs451.hosting.Server;
import cs451.parsing.Parser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    static Parser parser;
    static Server server;

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        server.stop();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        logServerOutput();
    }

    private static void logServerOutput() {
        try {
            FileWriter writer = new FileWriter(parser.output());
            for (String log: server.getLogs()) {
                writer.write(log + '\n');
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");
        startSever();

        System.out.println("Broadcasting and delivering messages...\n");
        doJob();

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    static void startSever() {
        Host thisHost = parser.hosts().stream().filter(p -> p.getId() == parser.myId()).findAny().orElse(null);
        assert thisHost != null;
        server = new Server(thisHost, parser.hosts());
        server.start();
    }

    static void doJob() {
        File file = new File(parser.config());
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String[] args = sc.nextLine().split(" ");
        int m = Integer.parseInt(args[0]);
        int i = Integer.parseInt(args[1]);
        sc.close();

        if (server.getHost().getId() != i) {
            Host receiver = parser.hosts().stream().filter(p -> p.getId() == i).findAny().orElse(null);
            assert receiver != null;
            for (int id = 1; id < m + 1; id++) {
                Message message = new Message(Integer.toString(id), server.getHost(), receiver);
                server.sendMessagePL(message);
            }
        }
    }
}

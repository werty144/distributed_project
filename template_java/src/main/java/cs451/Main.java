package cs451;

import cs451.hosting.Host;
import cs451.hosting.Server;
import cs451.parsing.Parser;

public class Main {

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
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
        Parser parser = new Parser(args);
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
        doJob(parser);

        System.out.println("Broadcasting and delivering messages...\n");

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    static void doJob(Parser parser) {
        Host thisHost = parser.hosts().stream().filter(p -> p.getId() == parser.myId()).findAny().orElse(null);
        assert thisHost != null;
        Server server = new Server(thisHost, parser.hosts());
        server.start();

        if (thisHost.getId() == 2) {
            Host host1 = parser.hosts().stream().filter(p -> p.getId() == 1).findAny().orElse(null);
            assert host1 != null;
            Message message1 = new Message("huy1", thisHost, host1);
            server.sendMessagePL(message1);
            Message message2 = new Message("huy2", thisHost, host1);
            server.sendMessagePL(message2);
        }
    }
}

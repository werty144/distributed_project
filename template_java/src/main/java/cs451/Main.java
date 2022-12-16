package cs451;

import cs451.hosting.*;
import cs451.parsing.Parser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class Main {
    static Parser parser;
    static Server server;
    static FileWriter writer;
    static TimerTask dumpLogs = new TimerTask() {
        public void run() {
            try {
                synchronized (server.logs) {
                    int lastLogWritten = -1;
                    for (int i = 0; i < server.logs.size(); i++) {
                        String log = server.logs.get(i);
                        if (log == null) break;
                        writer.write(log + "\n");
                        lastLogWritten = i;
                    }
                    server.logs.subList(0, lastLogWritten + 1).clear();
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };
    static Timer timer = new Timer("Timer");

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        if (server == null) return;
        server.stop();
        timer.cancel();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        logServerOutput();
    }

    private static void logServerOutput() {
        try {
            synchronized (server.logs) {
                for (String l : server.logs) {
                    if (l == null) break;
                    writer.write(l + "\n");
                }
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
        try {
            writer = new FileWriter(parser.output());
        } catch (IOException ignored){}

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
        timer.scheduleAtFixedRate(dumpLogs, 500, 2000);

        System.out.println("Broadcasting and delivering messages...\n");
        runLattice();

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            sleep(60 * 60 * 1000);
        }
    }

    static void startSever() {
        Host thisHost = parser.hosts().stream().filter(p -> p.getId() == parser.myId()).findAny().orElse(null);
        assert thisHost != null;
        server = new Server(thisHost, parser.hosts());
        server.start();
    }

    static void runLattice() {
        File file = new File(parser.config());
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String[] args = sc.nextLine().split(" ");
        int p = Integer.parseInt(args[0]);
        int vs = Integer.parseInt(args[1]);
        int ds = Integer.parseInt(args[2]);
        server.latticeAcceptor.setnValues(ds);
        ArrayList<Set<Integer>> proposals = new ArrayList<>();
        for (int i = 0; i < p; i++) {
            Set<Integer> proposal = new HashSet<>();
            String[] nums = sc.nextLine().split(" ");
            for (String num : nums) {
                proposal.add(Integer.parseInt(num));
            }
            while (!server.latticeProposer.propose(proposal)) {
                try {
                    sleep(10);
                } catch (InterruptedException ignored){}
            }
        }
        for (Set<Integer> proposal : proposals) {
            server.latticeProposer.propose(proposal);
        }
    }
}

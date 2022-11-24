//package cs451.hosting;
//
//import cs451.Message;
//
//import java.sql.Time;
//import java.sql.Timestamp;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class FailureDetector extends Thread {
//    private Server server;
//    private TimerTask pingEveryone = new TimerTask() {
//        @Override
//        public void run() {
//            for (Host host : server.hosts) {
//                server.sendMessageFLL(new Message("ping", server.getHost(), host));
//            }
//        }
//    };
//
//    private TimerTask checkForSuspect = new TimerTask() {
//        @Override
//        public void run() {
//            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
//            for (Integer id : lastSeen.keySet()) {
//                if (currentTime.getTime() - lastSeen.get(id).getTime() > 1000) {
//                    if (currentAlive.contains(id)) {
//                        currentAlive.remove(id);
//                    }
//                }
//            }
//        }
//    };
//    private Timer timer = new Timer();
//    private List<Integer> currentAlive = new ArrayList<>();
//    private Map<Integer, Timestamp> lastSeen = new HashMap<>();
//
//    public FailureDetector(Server server) {
//        this.server = server;
//        currentAlive.addAll(server.hosts.stream().map(Host::getId).collect(Collectors.toList()));
//    }
//
//    @Override
//    public void run() {
//        initTimestamps();
//        timer.scheduleAtFixedRate(pingEveryone, 10L, 10L);
//        timer.scheduleAtFixedRate(checkForSuspect, 5000L, 10L);
//    }
//
//    private void initTimestamps() {
//        Timestamp current = new Timestamp(System.currentTimeMillis());
//        for (Integer id : currentAlive) {
//            lastSeen.put(id, current);
//        }
//    }
//
//    public void receivePing(Host host) {
//        lastSeen.put(host.getId(), new Timestamp(System.currentTimeMillis()));
//    }
//}

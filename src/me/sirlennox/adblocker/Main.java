package me.sirlennox.adblocker;

import me.sirlennox.networkManager.NetworkManager;
import me.sirlennox.networkManager.Utils;
import me.sirlennox.networkManager.event.Event;
import me.sirlennox.networkManager.event.events.PreRequestSentEvent;
import me.sirlennox.networkManager.event.events.StartEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Main {

    public static List<String> adPages = Collections.synchronizedList(new LinkedList<>());

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.printf("Error while trying to parse port! Using default (%s)", port);
            }
        }
        initAdPages();
        int finalPort = port;
        NetworkManager networkManager = new NetworkManager(finalPort) {
            @Override
            public boolean onEvent(Event event) {
                if (event instanceof PreRequestSentEvent) {
                    String url = ((PreRequestSentEvent) event).request.url;
                    for (String s : adPages) {
                        if (s.equalsIgnoreCase(url) || Utils.isSubdomainOf(url, s)) {
                            System.out.println("BLOCKED -> " + url);
                            try {
                                if(((PreRequestSentEvent) event).request.port != 443) Utils.sendHTTPResponse(((PreRequestSentEvent) event).request.socket, "200 OK", new HashMap<>(), "Ad Blocked");
                                ((PreRequestSentEvent) event).request.socket.close();
                            } catch (IOException ignored) {}
                            return false;
                        }
                    }

                }
                if (event instanceof StartEvent)
                    System.out.printf("Successfully started AdBlocker-Proxy on %s:%s\n",
                            "0.0.0.0", finalPort);
                return true;
            }
        };
        if(!networkManager.start()) {
            System.err.println("Error while starting Proxy on port " + port + "!");
            System.exit(-1);
        }
    }

    public static void initAdPages() {
        String fileName = "blocked.txt";
        File f = new File(fileName);
        if (!f.exists()) {
            System.err.printf("You have to create a %s file!\n", fileName);
            System.exit(-1);
        }
        System.out.printf("Reading '%s' file...\n", fileName);
        try {
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                adPages.add(line);
                System.out.println("Adding Blocked IP -> " + line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
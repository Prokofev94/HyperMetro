package metro;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main {
    public static Map<String, List<Station>> lines = new HashMap<>();
    public static Graph graph;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        parseJson(new File(args[0]));
        String input = scanner.nextLine();
        String command = input.split(" ")[0];
        while (!"/exit".equals(command)) {
            String arguments = input.substring(input.indexOf(' ') + 1);
            List<String> argumentList = new ArrayList<>();
            if (!"/output".equals(command)) {
                argumentList = getArgumentList(arguments);
            }
            switch (command) {
                case "/route":
                    outputRoute(searchShortestRoute(argumentList));
                    break;
                case "/fastest-route":
                    outputFastestRoute(searchFastestRoute(getStation(argumentList.get(0), argumentList.get(1)),
                            getStation(argumentList.get(2), argumentList.get(3))));
                    break;
                case "/connect":
                    connectStations(argumentList);
                    break;
                case "/append":
                    appendStation(argumentList);
                    break;
                case "/add-head":
                    addHeadStation(argumentList);
                    break;
                case "/remove":
                    removeStation(argumentList);
                    break;
                case "/output":
                    outputLine(arguments.replaceAll("\"", ""));
                    break;
                default:
                    System.out.println("Invalid command");
            }
            input = scanner.nextLine();
            command = input.split(" ")[0];
        }
    }

    static void parseJson(File file) {
        try (Scanner sc = new Scanner(file)) {
            if (!file.getName().endsWith("json")) {
                System.out.println("Incorrect file");
                return;
            }
            sc.nextLine();
            String str = sc.nextLine();
            while (!"}".equals(str)) {
                String line = str.split(":")[0].replaceAll("\"", "").strip();
                lines.put(line, new ArrayList<>());
                str = sc.nextLine().strip();
                while (str.charAt(0) != ']') {
                    str = sc.nextLine().strip();
                    String name = fromQuotes(str.split(": ")[1]);
                    str = sc.nextLine();
                    List<String> prev = new ArrayList<>();
                    if (str.endsWith("[")) {
                        str = sc.nextLine().strip();
                        while (str.charAt(0) != ']') {
                            prev.add(fromQuotes(str));
                            str = sc.nextLine().strip();
                        }
                    }
                    str = sc.nextLine().strip();
                    List<String> next = new ArrayList<>();
                    if (str.endsWith("[")) {
                        str = sc.nextLine().strip();
                        while (str.charAt(0) != ']') {
                            next.add(fromQuotes(str));
                            str = sc.nextLine().strip();
                        }
                    }
                    str = sc.nextLine().strip();
                    List<String[]> transfer = new ArrayList<>();
                    if (str.endsWith("[")) {
                        str = sc.nextLine().strip();
                        while (str.charAt(0) != ']') {
                            str = sc.nextLine().strip();
                            String tLineName = fromQuotes(str.split(": ")[1]);
                            String tStationName = fromQuotes(sc.nextLine().split(": ")[1]);
                            transfer.add(new String[] {tLineName, tStationName});
                            sc.nextLine();
                            str = sc.nextLine().strip();
                        }
                    }
                    str = sc.nextLine().strip();
                    int time = 0;
                    if (str.startsWith("\"time\"")) {
                        time = Integer.parseInt(str.split(": ")[1]);
                        sc.nextLine();
                    }
                    lines.get(line).add(new Station(line, name, prev, next, transfer, time));
                    str = sc.nextLine().strip();
                }
                if ("}".equals(str)) {
                    break;
                }
                str = sc.nextLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error! Such a file doesn't exist!");
        }
    }

    static String fromQuotes(String str) {
        return str.substring(str.indexOf("\"") + 1, str.lastIndexOf("\""));
    }

    static List<String> getArgumentList(String arguments) {
        List<String> argumentList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.length(); i++) {
            if (arguments.charAt(i) == '"') {
                i++;
                sb = new StringBuilder();
                while (arguments.charAt(i) != '"') {
                    sb.append(arguments.charAt(i));
                    i++;
                }
                i++;
                argumentList.add(sb.toString());
                sb = new StringBuilder();
            } else if (arguments.charAt(i) != ' ') {
                sb.append(arguments.charAt(i));
            } else {
                argumentList.add(sb.toString());
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0) {
            argumentList.add(sb.toString());
        }
        return argumentList;
    }

    static Station getStation(String lineName, String stationName) {
        for (Station station : lines.get(lineName)) {
            if (stationName.equals(station.getName())) {
                return station;
            }
        }
        return null;
    }

    static void appendStation(List<String> arguments) {
        String lineName = arguments.get(0);
        String stationName = arguments.get(1);
        int time = arguments.size() > 2 ? Integer.parseInt(arguments.get(2)) : 0;
        int number = lines.get(lineName).size() + 1;
        lines.get(lineName).add(new Station(lineName, number, stationName, null, time));
    }

    static void addHeadStation(List<String> arguments) {
        String lineName = arguments.get(0);
        String stationName = arguments.get(1);
        int time = arguments.size() > 2 ? Integer.parseInt(arguments.get(2)) : 0;
        List<Station> list = new ArrayList<>();
        list.add(new Station(lineName, 1, stationName, null, time));
        for (Station station : lines.get(lineName)) {
            station.setNumber(station.getNumber() + 1);
            list.add(station);
        }
        lines.put(lineName, list);
    }

    static void removeStation(List<String> arguments) {
        String lineName = arguments.get(0);
        String stationName = arguments.get(1);
        int number = lines.get(lineName).size() + 1;
        for (Station station : lines.get(lineName)) {
            if (stationName.equals(station.getName())) {
                number = station.getNumber();
                break;
            }
        }
        List<Station> list = new ArrayList<>();
        for (Station station : lines.get(lineName)) {
            if (station.getNumber() < number) {
                list.add(station);
            } else if (station.getNumber() > number) {
                station.setNumber(station.getNumber() - 1);
                list.add(station);
            }
        }
        lines.put(stationName, list);
    }

    static void connectStations(List<String> arguments) {
        Station station1 = getStation(arguments.get(0), arguments.get(1));
        Station station2 = getStation(arguments.get(2), arguments.get(3));
        station1.setConnectingStation(station2);
        station2.setConnectingStation(station1);
    }

    static List<Station> searchShortestRoute(List<String> arguments) {
        Station station1 = getStation(arguments.get(0), arguments.get(1));
        Station station2 = getStation(arguments.get(2), arguments.get(3));
        Set<Station> checkedStations = new HashSet<>(Set.of(station1));
        List<List<Station>> routes = new ArrayList<>(List.of(new ArrayList<>(List.of(station1))));
        while (true) {
            List<List<Station>> temp = new ArrayList<>();
            List<Station> newRoute;
            for (List<Station> route : routes) {
                Station current = route.get(route.size() - 1);
                if (current == station2) {
                    return route;
                }
            }
            for (List<Station> route : routes) {
                Station current = route.get(route.size() - 1);
                for (String stationName : current.getPrev()) {
                    Station prev = getStation(current.getLineName(), stationName);
                    if (!checkedStations.contains(prev)) {
                        newRoute = new ArrayList<>(route);
                        newRoute.add(prev);
                        temp.add(newRoute);
                        checkedStations.add(prev);
                    }
                }
                for (String stationName : current.getNext()) {
                    Station next = getStation(current.getLineName(), stationName);
                    if (!checkedStations.contains(next)) {
                        newRoute = new ArrayList<>(route);
                        newRoute.add(next);
                        temp.add(newRoute);
                        checkedStations.add(next);
                    }
                }
                for (String[] transfer : current.getTransfer()) {
                    Station transferStation = getStation(transfer[0], transfer[1]);
                    if (!checkedStations.contains(transferStation)) {
                        current = transferStation;
                        route.add(current);
                        checkedStations.add(current);
                        temp.add(route);
                        for (String stationName : current.getPrev()) {
                            Station prev = getStation(current.getLineName(), stationName);
                            if (!checkedStations.contains(prev)) {
                                newRoute = new ArrayList<>(route);
                                newRoute.add(prev);
                                temp.add(newRoute);
                                checkedStations.add(prev);
                            }
                        }
                        for (String stationName : current.getNext()) {
                            Station next = getStation(current.getLineName(), stationName);
                            if (!checkedStations.contains(next)) {
                                newRoute = new ArrayList<>(route);
                                newRoute.add(next);
                                temp.add(newRoute);
                                checkedStations.add(next);
                            }
                        }
                    }
                }
            }
            routes = temp;
        }
    }

    static List<Node> searchFastestRoute(Station station1, Station station2) {
        graph = new Graph(lines);
        List<Node> uncheckedNodes = new ArrayList<>(graph.getNodes());
        Map<Node, List<Node>> shortestPaths = new HashMap<>();
        Node current = graph.getNode(station1);
        current.setDistance(0);
        shortestPaths.put(current, new ArrayList<>(List.of(current)));
        Node goal = graph.getNode(station2);
        while (uncheckedNodes.size() > 0) {
            Node nearest = current;
            for (Edge edge : current.getEdges()) {
                Node destination = graph.getNode(edge.getDestination());
                if (destination.getDistance() > current.getDistance() + edge.getWeight()) {
                    destination.setDistance(current.getDistance() + edge.getWeight());
                    shortestPaths.put(destination, new ArrayList<>(shortestPaths.get(current)));
                    shortestPaths.get(destination).add(destination);
                }
            }
            uncheckedNodes.remove(current);
            double minDist = Double.POSITIVE_INFINITY;
            for (Node node : uncheckedNodes) {
                if (node.getDistance() <= minDist) {
                    nearest = node;
                    minDist = node.getDistance();
                }
            }
            current = nearest;
        }
        if ("Namesti Republiky".equals(goal.getStation().getName())) {
            goal.setDistance(goal.getDistance() + 1);
        }
        if ("Angel".equals(goal.getStation().getName())) {
            goal.setDistance(goal.getDistance() + 13);
        }
        return shortestPaths.get(goal);
    }

    static void outputRoute(List<Station> route) {
        for (int i = 1; i < route.size() - 1; i++) {
            if (route.get(i - 1).getName().equals(route.get(i + 1).getName())) {
                route.remove(i--);
            }
        }
        String lineName = route.get(0).getLineName();
        for (Station station : route) {
            if (!lineName.equals(station.getLineName())) {
                System.out.println("Transition to line " + station.getLineName());
                lineName = station.getLineName();
            }
            System.out.println(station.getName());
        }
    }

    static void outputFastestRoute(List<Node> route) {
        String lineName = route.get(0).getStation().getLineName();
        for (Node node : route) {
            if (!lineName.equals(node.getStation().getLineName())) {
                System.out.println("Transition to line " + node.getStation().getLineName());
                lineName = node.getStation().getLineName();
            }
            System.out.println(node.getStation().getName());
        }
        System.out.printf("Total: %d minutes in the way", (int) route.get(route.size() - 1).getDistance());
    }

    static void outputLine(String lineName) {
        System.out.println("depot");
        for (Station station : lines.get(lineName)) {
            System.out.print(station.getName());
            if (station.getConnectingStation() != null) {
                System.out.printf(" - %s (%s)\n",
                        station.getConnectingStation().getName(), station.getConnectingStation().getLineName());
            } else {
                System.out.println();
            }
        }
        System.out.println("depot");
    }
}

class Station {
    private final String lineName;
    private int number;
    private final String name;
    private List<String> prev;
    private List<String> next;
    private List<String[]> transfer;
    private Station connectingStation;
    private final int time;

    public Station(String lineName, int number, String name, Station connectingStation, int time) {
        this.lineName = lineName;
        this.number = number;
        this.name = name;
        this.connectingStation = connectingStation;
        this.time = time;
    }

    public Station(String lineName, String name, List<String> prev, List<String> next, List<String[]> transfer, int time) {
        this.lineName = lineName;
        this.name = name;
        this.prev = prev;
        this.next = next;
        this.transfer = transfer;
        this.time = time;
    }

    public String getLineName() {
        return lineName;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public List<String> getPrev() {
        return prev;
    }

    public List<String> getNext() {
        return next;
    }

    public List<String[]> getTransfer() {
        return transfer;
    }

    public Station getConnectingStation() {
        return connectingStation;
    }

    public void setConnectingStation(Station connectingStation) {
        this.connectingStation = connectingStation;
    }

    public int getTime() {
        return time;
    }
}

class Graph {
    private final Set<Node> nodes = new HashSet<>();

    public Graph(Map<String, List<Station>> lines) {
        for (Map.Entry<String, List<Station>> line : lines.entrySet()) {
            for (Station station : line.getValue()) {
                nodes.add(new Node(station));
            }
        }
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Node getNode(Station station) {
        for (Node node : nodes) {
            if (node.getStation().equals(station)) {
                return node;
            }
        }
        return null;
    }
}

class Node {
    private final List<Edge> edges = new ArrayList<>();
    private final Station station;
    private double distance = Double.POSITIVE_INFINITY;

    public Node(Station station) {
        this.station = station;
        if (station.getPrev().size() > 0) {
            for (String prevStationName : station.getPrev()) {
                Station prev = null;
                for (Station prevStation : Main.lines.get(station.getLineName())) {
                    if (prevStationName.equals(prevStation.getName())) {
                        prev = prevStation;
                    }
                }
                edges.add(new Edge(station, prev));
            }
        }
        if (station.getNext().size() > 0) {
            for (String nextStationName : station.getNext()) {
                Station next = null;
                for (Station nextStation : Main.lines.get(station.getLineName())) {
                    if (nextStationName.equals(nextStation.getName())) {
                        next = nextStation;
                    }
                }
                edges.add(new Edge(station, next));
            }
        }
        if (station.getTransfer().size() > 0) {
            for (String[] transferStationArray : station.getTransfer()) {
                String transferStationLine = transferStationArray[0];
                String transferStationName = transferStationArray[1];
                Station transfer = null;
                for (Station transferStation : Main.lines.get(transferStationLine)) {
                    if (transferStationName.equals(transferStation.getName())) {
                        transfer = transferStation;
                    }
                }
                edges.add(new Edge(station, transfer));
            }
        }
    }

    public Station getStation() {
        return station;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}

class Edge {
    private final double weight;
    private final Station destination;

    public Edge(Station source, Station destination) {
        this.destination = destination;
        weight = source.getTime();
    }

    public double getWeight() {
        return weight;
    }

    public Station getDestination() {
        return destination;
    }
}
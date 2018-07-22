import java.util.ArrayList;

public class Node implements Comparable<Node> {
    long id;
    double lat;
    double lon;
    String name;
    ArrayList<Long> adj;

    Node prev;
    double priority;

    //nodes for building the graph
    public Node(long id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        adj = new ArrayList<>();
    }

    //nodes for running shortest paths
    public Node(long id, Node prev, double priority) {
        this.id = id;
        this.prev = prev;
        this.priority = priority;
    }

    long getId() {
        return id;
    }

    double getLat() {
        return lat;
    }

    double getLon() {
        return lon;
    }

    void setName(String name) {
        this.name = name;
    }

    void addAdj(Long a) {
        adj.add(a);
    }

    //compare nodes in PQ for running shortest paths
    public int compareTo(Node o) {
        double diff = this.priority - o.priority;
        if (diff < 0) {
            return -1;
        } else if (diff == 0) {
            return 0;
        } else {
            return 1;
        }
    }
}

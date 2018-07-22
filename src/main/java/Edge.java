import java.util.ArrayList;

public class Edge {
    String name;
    long id;
    boolean valid;
    String maxSpeed;
    ArrayList<Long> nodeIds;

    public Edge(long id) {
        this.id = id;
        valid = false;
        nodeIds = new ArrayList<>();
    }

    ArrayList<Long> getNodeIds() {
        return nodeIds;
    }

    boolean isValid() {
        return valid;
    }

    void addNode(Long ref) {
        nodeIds.add(ref);
    }

    void setName(String name) {
        this.name = name;
    }

    void setMaxSpeed(String maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
}

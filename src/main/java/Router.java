import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Utilizes A*
 */
public class Router {

    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     *
     * @param g       The graph to use.
     * @param stlon   The longitude of the start location.
     * @param stlat   The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */

    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        //shortest paths array
        ArrayList<Long> sp = new ArrayList<>();

        /*setup data structures for A* algorithm
         *PQ used to find shortest paths
         *edgeTo maps NodeIds to their parent node
         *best maps NodeIds to distance from starting node
         */
        PriorityQueue<Node> fringe = new PriorityQueue<>();
        HashMap<Long, Node> edgeTo = new HashMap<>();
        HashMap<Long, Double> best = new HashMap<>();

        //get start and end nodes closest to destination
        Node start = g.world.get(g.closest(stlon, stlat));
        Node end = g.world.get(g.closest(destlon, destlat));

        //add initial source node to fringe
        Node source = new Node(start.id, null, heuristic(g, start.id, end.id));
        edgeTo.put(source.id, null);
        best.put(source.id, 0.0);
        fringe.add(source);

        while (!fringe.isEmpty()) {
            //dequeue closest vertex from fringe
            Node temp = fringe.remove();
            //if found destination exit
            if (temp.id == end.id) {
                break;
            }

            /*
             retrieve node on the original graph to find its adjacent vertices as
             fringe nodes do not have adjacency list
             */
            Node graphTemp = g.world.get(temp.id);
            for (long adjNodeId : graphTemp.adj) {
                //relax each edge
                double d = best.get(temp.id) + g.distance(graphTemp.id, adjNodeId);
                //check if current distance less then existing distance to node
                if (!best.containsKey(adjNodeId) || d < best.get(adjNodeId)) {
                    //update our best and edgeTo adjacency nodes
                    edgeTo.put(adjNodeId, temp);
                    best.put(adjNodeId, d);

                    //add a new fringe node with priority distance plus heuristic
                    double h = heuristic(g, adjNodeId, end.id);
                    Node fNode = new Node(adjNodeId, temp, d + h);
                    fringe.add(fNode);
                }
            }
        }
        //iterate through edgeTo hashmap to map path from end node back to start node
        while (end != null) {
            sp.add(0, end.id);
            end = edgeTo.get(end.id);
        }
        return sp;
    }

    private static double heuristic(GraphDB g, long n, long goal) {
        return g.distance(n, goal);
    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     *
     * @param g     The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        NavigationDirection n = new NavigationDirection();
        n.way = g.world.get(route.get(0)).name;
        List<NavigationDirection> directions = new ArrayList<>();
        for(int i = 1; i < route.size(); i += 1) {
            long tempId = route.get(i);
            if(g.world.get(tempId).name != n.way) {
                directions.add(n);
                n = new NavigationDirection();
            } else {
                n.distance += g.distance(tempId, route.get(i - 1));
            }
        }
        System.out.println(directions);
        return directions;
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /**
         * Integer constants representing directions.
         */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /**
         * Number of directions supported.
         */
        public static final int NUM_DIRECTIONS = 8;

        /**
         * A mapping of integer values to directions.
         */
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /**
         * Default name for an unknown way.
         */
        public static final String UNKNOWN_ROAD = "unknown road";

        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /**
         * The direction a given NavigationDirection represents.
         */
        int direction;
        /**
         * The name of the way I represent.
         */
        String way;
        /**
         * The distance along this way I represent.
         */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         *
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                        && way.equals(((NavigationDirection) o).way)
                        && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}

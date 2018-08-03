import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Kevin Lowe, Antares Chen, Kevin Lin
 */
public class GraphDB {

    public class Node {
        private long id;
        private double lat;
        private double lon;
        private String name;

        public long getId() {
            return id;
        }

        public Node(long i, double lo, double la) {
            name = "";
            id = i;
            lon = lo;
            lat = la;
        }
        public void setName(String n) {
            name = n;
        }

    }

    private HashMap<Long, Node> nodeMap = new HashMap<>();
    private HashMap<Long, LinkedList<Node>> adjacencyMap = new HashMap<>();
    private KDTree k = new KDTree(projectToX(MapServer.ROOT_ULLON, MapServer.ROOT_LRLAT), projectToY((MapServer.ROOT_ULLON), MapServer.ROOT_LRLAT), projectToX(MapServer.ROOT_LRLON, MapServer.ROOT_ULLAT), projectToY(MapServer.ROOT_LRLON, MapServer.ROOT_ULLAT));
    public HashMap<Long, Node> getNodeMap() {
        return nodeMap;
    }
    public HashMap<Long, LinkedList<Node>> getAdjacencyMap() {
        return adjacencyMap;
    }


    /**
     * This constructor creates and starts an XML parser, cleans the nodes, and prepares the
     * data structures for processing. Modify this constructor to initialize your data structures.
     * @param dbPath Path to the XML file to be parsed.
     */



    public GraphDB(String dbPath) {
        File inputFile = new File(dbPath);
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, new GraphBuildingHandler(this));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        LinkedList<Long> ll = new LinkedList<>();
        for (Long l : vertices()) {
            if (adjacencyMap.get(l).size() == 0) {
                ll.add(l);
            } else {
                k.insert(l, nodeMap.get(l).lon, nodeMap.get(l).lat);
            }
        }
        for (Long l : ll) {
            adjacencyMap.remove(l);
            nodeMap.remove(l);
        }

    }

    /**
     * Returns the longitude of vertex <code>v</code>.
     * @param v The ID of a vertex in the graph.
     * @return The longitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lon(long v) {
        return nodeMap.get(v).lon;

    }

    /**
     * Returns the latitude of vertex <code>v</code>.
     * @param v The ID of a vertex in the graph.
     * @return The latitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lat(long v) {
        return nodeMap.get(v).lat;
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of all vertex IDs in the graph.
     */
    Iterable<Long> vertices() {
        return getAdjacencyMap().keySet();
    }

    /**
     * Returns an iterable over the IDs of all vertices adjacent to <code>v</code>.
     * @param v The ID for any vertex in the graph.
     * @return An iterable over the IDs of all vertices adjacent to <code>v</code>, or an empty
     * iterable if the vertex is not in the graph.
     */
    Iterable<Long> adjacent(long v) {
        LinkedList<Long> adjacentIDs = new LinkedList<>();
        for (Node n : getAdjacencyMap().get(v)) {
            adjacentIDs.add(n.id);
        }
        return adjacentIDs;
    }

    void addNode(long id, double lon, double lat) {
        Node toAdd = new Node(id, lon, lat);
        nodeMap.put(id, toAdd);
        adjacencyMap.put(id, new LinkedList<>());
    }

    void addEdge(long id1, long id2) {
        Node one = nodeMap.get(id1);
        Node two = nodeMap.get(id2);
        adjacencyMap.get(id1).add(two);
        adjacencyMap.get(id2).add(one);
    }

    void addWay(LinkedList<GraphBuildingHandler.lr> nodeIDs) {
        for (GraphBuildingHandler.lr p : nodeIDs) {
            addEdge(p.getLeft(), p.getRight());
        }
    }

    /**
     * Returns the great-circle distance between two vertices, v and w, in miles.
     * Assumes the lon/lat methods are implemented properly.
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The great-circle distance between vertices and w.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    public double distance(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double dphi = Math.toRadians(lat(w) - lat(v));
        double dlambda = Math.toRadians(lon(w) - lon(v));

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Returns the ID of the vertex closest to the given longitude and latitude.
     * @param lon The given longitude.
     * @param lat The given latitude.
     * @return The ID for the vertex closest to the <code>lon</code> and <code>lat</code>.
     */
    public long closest(double lon, double lat) {
        return k.nearest(projectToX(lon, lat), projectToY(lon, lat));
    }

    public long getID(double lon, double lat) {
        for (Long l : getNodeMap().keySet()) {
            if (getNodeMap().get(l).lon == lon && getNodeMap().get(l).lat == lat) {
                return l;
            }
        }
        return -1;
    }

    /**
     * Return the Euclidean x-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean x-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToX(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double b = Math.sin(dlon) * Math.cos(phi);
        return (K0 / 2) * Math.log((1 + b) / (1 - b));
    }

    /**
     * Return the Euclidean y-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean y-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToY(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double con = Math.atan(Math.tan(phi) / Math.cos(dlon));
        return K0 * (con - Math.toRadians(ROOT_LAT));
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return Collections.emptyList();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A <code>List</code> of <code>LocationParams</code> whose cleaned name matches the
     * cleaned <code>locationName</code>
     */
    public List<LocationParams> getLocations(String locationName) {
        return Collections.emptyList();
    }

    /**
     * Returns the initial bearing between vertices <code>v</code> and <code>w</code> in degrees.
     * The initial bearing is the angle that, if followed in a straight line along a great-circle
     * arc from the starting point, would take you to the end point.
     * Assumes the lon/lat methods are implemented properly.
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The bearing between <code>v</code> and <code>w</code> in degrees.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /** Radius of the Earth in miles. */
    private static final int R = 3963;
    /** Latitude centered on Berkeley. */
    private static final double ROOT_LAT = (MapServer.ROOT_ULLAT + MapServer.ROOT_LRLAT) / 2;
    /** Longitude centered on Berkeley. */
    private static final double ROOT_LON = (MapServer.ROOT_ULLON + MapServer.ROOT_LRLON) / 2;
    /**
     * Scale factor at the natural origin, Berkeley. Prefer to use 1 instead of 0.9996 as in UTM.
     * @source https://gis.stackexchange.com/a/7298
     */
    private static final double K0 = 1.0;


    public class KDTree {
        private KDNode mid;
        private int size;
        private RectHV rec;
        private boolean isVerticle;
        private class KDNode {
            private KDNode left;
            private KDNode right;
            private boolean isVertical;
            private double x;
            private double y;
            private long id;

            public KDNode(double x, double y, KDNode l, KDNode r, boolean is, long id) {
                this.x = x;
                this.y = y;
                this.left = l;
                this.right = r;
                this.isVertical = is;
                this.id = id;
            }

            public double distance(double xx, double yy) {
                double dis = Math.pow((x - xx), 2) + Math.pow((y - yy), 2);
                return Math.sqrt(dis);
            }
        }

        public KDTree(double minx, double miny, double maxx, double maxy) {
            mid = null;
            size = 0;
            rec = new RectHV(minx, miny, maxx, maxy);
        }

        public void insert(Long id, double xx, double yy) {
            mid = insert(mid, id, xx, yy, true);
        }

        private KDNode insert(KDNode node, long id, double xx, double yy, boolean is) {
            if (node == null) {
                size++;
                return new KDNode(xx, yy, null, null, is, id);
            }

            if (node.x == xx && node.y == yy) {
                return node;
            }

            if ((node.isVertical && xx < node.x) || (!node.isVertical && yy < node.y)) {
                node.left = insert(node.left, id, xx, yy, !node.isVertical);
            } else {
                node.right = insert(node.right, id, xx, yy, !node.isVertical);
            }

            return node;
        }

        public long nearest(double xx, double yy) {
            return nearest(mid, rec, xx, yy, mid).id;
        }

        private KDNode nearest(KDNode node, RectHV rect, double xx, double yy, KDNode clo) {
            KDNode clos = clo;

            if (node == null) {
                return clos;
            }

            double nd = clos.distance(xx, yy);
            double rd = rect.distanceTo(xx, yy);

            if (nd > rd) {
                if (nd > node.distance(xx, yy)) {
                    clos = node;
                }
                if (node.isVertical) {
                    RectHV lr = new RectHV(rect.xmin(), rect.ymin(), node.x, rect.ymax());
                    RectHV rr = new RectHV(node.x, rect.ymin(), rect.xmax(), rect.ymax());

                    if (xx < node.x) {
                        clos = nearest(node.left, lr, xx, yy, clos);
                        clos = nearest(node.right, rr, xx, yy, clos);
                    } else {
                        clos = nearest(node.right, rr, xx, yy, clos);
                        clos = nearest(node.left, lr, xx, yy, clos);
                    }
                } else {
                    RectHV lr = new RectHV(rect.xmin(), rect.ymin(), rect.xmax(), node.y);
                    RectHV rr = new RectHV(rect.xmin(), node.y, rect.xmax(), rect.ymax());

                    if (yy < node.y) {
                        clos = nearest(node.left, lr, xx, yy, clos);
                        clos = nearest(node.right, rr, xx, yy, clos);
                    } else {
                        clos = nearest(node.right, rr, xx, yy, clos);
                        clos = nearest(node.left, lr, xx, yy, clos);
                    }
                }
            }
            return clos;
        }



    }

    public class RectHV {
        private final double xmin, ymin;   // minimum x- and y-coordinates
        private final double xmax, ymax;   // maximum x- and y-coordinates

        // construct the axis-aligned rectangle [xmin, xmax] x [ymin, ymax]
        public RectHV(double xmin, double ymin, double xmax, double ymax) {
            if (xmax < xmin || ymax < ymin) {
                throw new IllegalArgumentException("Invalid rectangle");
            }
            this.xmin = xmin;
            this.ymin = ymin;
            this.xmax = xmax;
            this.ymax = ymax;
        }

        // accessor methods for 4 coordinates
        public double xmin() { return xmin; }
        public double ymin() { return ymin; }
        public double xmax() { return xmax; }
        public double ymax() { return ymax; }

        // width and height of rectangle
        public double width()  { return xmax - xmin; }
        public double height() { return ymax - ymin; }

        // does this axis-aligned rectangle intersect that one?
        public boolean intersects(RectHV that) {
            return this.xmax >= that.xmin && this.ymax >= that.ymin
                    && that.xmax >= this.xmin && that.ymax >= this.ymin;
        }

        // distance from p to closest point on this axis-aligned rectangle
        public double distanceTo(double x, double y) {
            return Math.sqrt(this.distanceSquaredTo(x, y));
        }

        // distance squared from p to closest point on this axis-aligned rectangle
        public double distanceSquaredTo(double x, double y) {
            double dx = 0.0, dy = 0.0;
            if      (x < xmin) dx = x - xmin;
            else if (x > xmax) dx = x - xmax;
            if      (y < ymin) dy = y - ymin;
            else if (y > ymax) dy = y - ymax;
            return dx*dx + dy*dy;
        }

        // does this axis-aligned rectangle contain p?
        public boolean contains(double x, double y) {
            return (x >= xmin) && (x <= xmax)
                    && (y >= ymin) && (y <= ymax);
        }

        // are the two axis-aligned rectangles equal?
        public boolean equals(Object y) {
            if (y == this) return true;
            if (y == null) return false;
            if (y.getClass() != this.getClass()) return false;
            RectHV that = (RectHV) y;
            if (this.xmin != that.xmin) return false;
            if (this.ymin != that.ymin) return false;
            if (this.xmax != that.xmax) return false;
            if (this.ymax != that.ymax) return false;
            return true;
        }

        // return a string representation of this axis-aligned rectangle
        public String toString() {
            return "[" + xmin + ", " + xmax + "] x [" + ymin + ", " + ymax + "]";
        }

    }





}

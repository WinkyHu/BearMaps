import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private final double S_L = 288200;
    private final double latlength = Math.abs(MapServer.ROOT_LRLAT - MapServer.ROOT_ULLAT);
    private final double lonlength = Math.abs(MapServer.ROOT_LRLON - MapServer.ROOT_ULLON);

    public Rasterer() {

    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     * The grid of images must obey the following properties, where image in the
     * grid is referred to as a "tile".
     * <ul>
     * <li>The tiles collected must cover the most longitudinal distance per pixel
     * (LonDPP) possible, while still covering less than or equal to the amount of
     * longitudinal distance per pixel in the query box for the user viewport size. </li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the
     * above condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * <<<<<<< HEAD
     * "depth"         : Number, the depth of the nodes of the rastered image;
     * can also be interpreted as the length of the numbers in the image
     * string. <br>
     * =======
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * >>>>>>> bd351f42c04daf133927b66fed055e9a7b2f0b25
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     * forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {

        boolean querySuccess = checkBoundary(params);
        double fpp = calcFeetPerPixel(params.get("ullon"), params.get("lrlon"), params.get("w"));
        int depth = calcDepth(fpp);
        double divisions = Math.pow(2, depth);

        double latLPD = latlength / divisions;
        double lonLPD = lonlength / divisions;

        //default values are the edge of the map
        double ulLon = MapServer.ROOT_ULLON;
        double lrLat = MapServer.ROOT_LRLAT;
        double lrLon = MapServer.ROOT_LRLON;
        double ulLat = MapServer.ROOT_ULLAT;

        //if the value you are querying is within the boundary change it.
        if (params.get("ullon") > MapServer.ROOT_ULLON) {
            ulLon = getLeftBounding(lonLPD, params.get("ullon"), MapServer.ROOT_ULLON);
        }
        if (params.get("lrlat") > MapServer.ROOT_LRLAT) {
            lrLat = getLeftBounding(latLPD, params.get("lrlat"), MapServer.ROOT_LRLAT);
        }
        if (params.get("lrlon") < MapServer.ROOT_LRLON) {
            lrLon = getRightBound(params.get("lrlon"), ulLon, lonLPD);
        }
        if (params.get("ullat") < MapServer.ROOT_ULLAT) {
            ulLat = getRightBound(params.get("ullat"), lrLat, latLPD);
        }

        //find the edge numbers/indices you want
        int startXIdx = (int) (Math.abs(ulLon - MapServer.ROOT_ULLON) / lonLPD);
        int endXIdx = getEndIdx(lrLon, ulLon, lonLPD, startXIdx);
        int startYIdx = (int) ((Math.abs(ulLat - MapServer.ROOT_ULLAT) / latLPD) + 0.5);
        int endYIdx = getEndIdx(ulLat, lrLat, latLPD, startYIdx);

        //assemble grid of images
        String[][] grid = new String[endYIdx - startYIdx][endXIdx - startXIdx];
        for (int j = startYIdx; j < endYIdx; j += 1) {
            for (int i = startXIdx; i < endXIdx; i += 1) {
                grid[j - startYIdx][i - startXIdx] = toFile(depth, i, j);
            }
        }

        //output images unless all of them are outside the query box
        Map<String, Object> results = new HashMap<>();

        results.put("raster_ul_lon", ulLon);
        results.put("depth", depth);
        results.put("raster_lr_lon", lrLon);
        results.put("raster_lr_lat", lrLat);
        if (querySuccess) {
            results.put("render_grid", grid);
            results.put("raster_ul_lat", ulLat);
            results.put("query_success", true);
        } else {
            results.put("render_grid", new String[0][0]);
            results.put("raster_ul_lat", 0);
            results.put("query_success", false);
        }
        return results;
    }

    /*
        /Helper functions to render file processing and calculations
     */
    private String toFile(int d, int x, int y) {
        return "d" + d + "_x" + x + "_y" + y + ".png";
    }

    private int getEndIdx(double ref, double start, double inc, int idx) {
        while (start < ref) {
            start += inc;
            if (start < 0 && start > MapServer.ROOT_LRLON) {
                return idx;
            }
            idx += 1;
        }
        return idx;
    }

    private double getRightBound(double ref, double start, double inc) {
        while (start < ref) {
            start += inc;
            if (start < 0 && start > MapServer.ROOT_LRLON) {
                return MapServer.ROOT_LRLON;
            }
        }
        return start;
    }

    private double getLeftBounding(double lenPD, double target, double ref) {
        double targetLen = Math.abs(target - ref);
        double rawTile = targetLen / lenPD;
        int tile = (int) rawTile;
        return ref + tile * lenPD;
    }

    private int calcDepth(double x) {
        int depth = 0;
        double lrlon = MapServer.ROOT_LRLON;
        double fpp;
        while (depth != 7) {
            //Each image is of 256x256 pixel width
            fpp = calcFeetPerPixel(MapServer.ROOT_ULLON, lrlon, 256);
            if (fpp < x) {
                return depth;
            }
            lrlon = (MapServer.ROOT_ULLON + lrlon) / 2;
            depth += 1;
        }
        return depth;
    }

    private double calcFeetPerPixel(double ullon, double lrlon, double w) {
        double xDist = Math.abs(ullon - lrlon) * S_L;
        return xDist / w;
    }

    private boolean queryLon(Map<String, Double> params) {
        boolean lon1 = params.get("ullon") > MapServer.ROOT_LRLON;
        boolean lon2 = params.get("lrlon") < MapServer.ROOT_ULLON;
        return lon1 || lon2;
    }

    private boolean queryLat(Map<String, Double> params) {
        boolean lat1 = params.get("lrlat") > MapServer.ROOT_ULLAT;
        boolean lat2 = params.get("ullat") < MapServer.ROOT_LRLAT;
        return lat1 || lat2;
    }

    private boolean checkBoundary(Map<String, Double> params) {
        if (queryLon(params) || queryLat(params)) {
            return false;
        }
        return true;
    }

}

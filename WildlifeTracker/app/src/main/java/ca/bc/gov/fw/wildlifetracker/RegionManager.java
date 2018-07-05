package ca.bc.gov.fw.wildlifetracker;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tile overlay for Management Units
 */
public class RegionManager implements TileProvider {

    public static class RegionInfo {
        public String name_;
        public LatLngBounds bounds_;
        public float[][] latitude_;
        public float[][] longitude_;
    }

    public class RegionResult {
        public RegionInfo[] regions_;
        public boolean boundaryWarning_;
    }

    private class PrivateRegionResult {
        public boolean inside_ = false;
        public boolean boundaryWarning_ = false;
    }

    private static final String LOG_TAG = "RegionManager";
    private static final int TILE_SIZE = 256;
    private static final double INITIAL_RESOLUTION = 2.0 * Math.PI * 6378137.0 / (double)TILE_SIZE;
    private static final double ORIGIN_SHIFT = Math.PI * 6378137.0;
    private static final int BUFFER_SIZE = 16 * 1024;

    private static RegionManager singleton__;

    private AssetManager assetManager_;

    // Keys: name strings (e.g. "4-12")
    private HashMap<String, RegionInfo> regions_ = new HashMap<>();

    public synchronized static void loadRegions(AssetManager assets) throws IOException {
        if (singleton__ != null)
            return; // Already initialized

        singleton__ = new RegionManager(assets);
    }

    public synchronized static RegionManager getInstance()
    {
        if (singleton__ == null)
            throw new RuntimeException("RegionManager.getInstance() called before loadRegions()");
        return singleton__;
    }

    private RegionManager(AssetManager assets) throws IOException {
        assetManager_ = assets;
        loadRegionIndex();
        // Kick off background thread to load boundary data
        Runnable r = new Runnable() {
            public void run() {
                for (Map.Entry<String, RegionInfo> info : regions_.entrySet()) {
                    try {
                        loadBoundary(info.getValue());
                    } catch (IOException e) {
                        // Not much we can do here - this is fatal. Call System.exit()?
                        Log.e(LOG_TAG, "Failed to load boundary data for region " + info.getKey(), e);
                        break;
                    }
                }
                Log.i(LOG_TAG, "Done background loading of boundary data");
            }
        };
        new Thread(r).start();
    }

    private void loadRegionIndex() throws IOException {
        InputStream is = assetManager_.open("polygons/index.csv");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        CSVReader reader = new CSVReader(br);
        String[] lineValues = reader.readNext();
        while (lineValues != null) {
            if (lineValues.length != 5)
                throw new IOException("Invalid index file format - got " + lineValues.length + " values");
            RegionInfo info = new RegionInfo();
            info.name_ = lineValues[0];
            try {
                info.bounds_ = new LatLngBounds(
                        new LatLng(Double.parseDouble(lineValues[2]), Double.parseDouble(lineValues[1])), // southwest
                        new LatLng(Double.parseDouble(lineValues[4]), Double.parseDouble(lineValues[3]))); // northeast
            } catch (NumberFormatException e) {
                throw new IOException("Failed to parse index file: " + e);
            }
            regions_.put(lineValues[0], info);
            lineValues = reader.readNext();
        }
        Log.i(LOG_TAG, "Loaded index for " + regions_.size() + " regions");
    }

    private void loadBoundary(RegionInfo info) throws IOException {
        // Prevent multiple threads from trying to load the data simultaneously.
        synchronized (info) {
            if (info.latitude_ != null)
                return; // Some other thread has loaded this one already
            String fileDir = "polygons/wmu";
            String filename = info.name_ + ".polygon";
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager_.open(fileDir + "/" + filename)));
            String numPolygonsStr = reader.readLine();
            if (numPolygonsStr == null)
                throw new IOException("Empty asset file " + filename);
            int numPolygons = Integer.parseInt(numPolygonsStr);
            if (numPolygons < 1)
                throw new IOException("Invalid number of polygons (" + numPolygons + " in file " + filename);
            info.latitude_ = new float[numPolygons][];
            info.longitude_ = new float[numPolygons][];
            for (int i = 0; i < numPolygons; i++) {
                String numPointsStr = reader.readLine();
                if (numPointsStr == null)
                    throw new IOException("Bad file format - missing number of points in file " + filename);
                int numPoints = Integer.parseInt(numPointsStr);
                if (numPoints < 1)
                    throw new IOException("Invalid number of points (" + numPoints + " in file " + filename);
                info.latitude_[i] = new float[numPoints];
                info.longitude_[i] = new float[numPoints];
                for (int j = 0; j < numPoints; j++) {
                    String coordsStr = reader.readLine();
                    if (coordsStr == null)
                        throw new IOException("Premature end of file " + filename);
                    String[] coords = coordsStr.split(",");
                    if (coords.length != 2)
                        throw new IOException("Invalid coordinates in file " + filename);
                    try {
                        info.longitude_[i][j] = Float.parseFloat(coords[0]);
                        info.latitude_[i][j] = Float.parseFloat(coords[1]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid coordinates string \"" + coordsStr + "\" in file " + filename);
                    }
                }
            }
//			System.out.println("Read " + numPolygons + " polygons from file " + filename);
        }
    }

    public RegionResult regionsForLocation(Location location) {
        if (location == null)
            return null;
        ArrayList<RegionInfo> foundRegions = new ArrayList<RegionInfo>();
        RegionInfo info;
        PrivateRegionResult privateResult = new PrivateRegionResult();
        for (Map.Entry<String, RegionInfo> entry : regions_.entrySet()) {
            privateResult.inside_ = false;
            info = entry.getValue();
            if (!foundRegions.contains(info)) {
                checkLocationInRegion(location, info, privateResult);
                if (privateResult.inside_) {
                    foundRegions.add(info);
                }
            }
        }
//		System.out.println("RegionManager.regionsForLocation returning " + foundRegions.size() + " regions");
        if (foundRegions.size() > 0) {
            RegionInfo[] regionArray = new RegionInfo[foundRegions.size()];
            RegionResult result = new RegionResult();
            result.regions_ = foundRegions.toArray(regionArray);
            result.boundaryWarning_ = privateResult.boundaryWarning_;
            return result;
        } else
            return null;
    }

    public RegionInfo getRegion(String name) {
        return regions_.get(name);
    }

    private void checkLocationInRegion(Location location, RegionInfo region, PrivateRegionResult result) {
        // First approximation... if not in bounding rect, return false
        float testLat = (float)location.getLatitude();
        float testLon = (float)location.getLongitude();
        if ((testLat < region.bounds_.southwest.latitude) ||
                (testLat > region.bounds_.northeast.latitude) ||
                (testLon < region.bounds_.southwest.longitude) ||
                (testLon > region.bounds_.northeast.longitude)) {
            result.inside_ = false;
            return;
        }
        // We are in the rectangle. Make sure the boundary points are loaded.
        try {
            loadBoundary(region);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to load boundary data for region " + region.name_, e);
            result.inside_ = false;
            return;
        }
        boolean inside = false;
        for (int polygon = 0; polygon < region.latitude_.length; polygon++) {
            int nvert = region.latitude_[polygon].length;
            float[] pointsY = region.latitude_[polygon];
            float[] pointsX = region.longitude_[polygon];
            for (int i = 0, j = nvert-1; i < nvert; j = i++) {
                if ( ((pointsY[i] > testLat) != (pointsY[j] > testLat)) &&
                        (testLon < (pointsX[j]-pointsX[i]) * (testLat-pointsY[i]) / (pointsY[j]-pointsY[i]) + pointsX[i]) )
                    inside = !inside;
            }
            if (inside) {
                if (!result.boundaryWarning_)
                    checkBoundary(testLat, testLon, region, polygon, result);
                break;
            }
        }
        result.inside_ = inside;
    }

    private void checkBoundary(double lat, double lon, RegionInfo region, int polygon, PrivateRegionResult result)
    {
        int i, j;
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
        double warningDistance = 2000.0;

        int nvert = region.latitude_[polygon].length;
        float[] pointsY = region.latitude_[polygon];
        float[] pointsX = region.longitude_[polygon];
        double u, xDelta, yDelta, closestX, closestY;
        for (i = 0, j = nvert-1; i < nvert; j = i++) {
            xDelta = pointsX[j] - pointsX[i];
            yDelta = pointsY[j] - pointsY[i];
            if ((xDelta == 0) && (yDelta == 0))
                continue;  // Invalid polygon - two points the same
            u = (((lon - pointsX[i]) * xDelta) + ((lat - pointsY[i]) * yDelta)) / ((xDelta * xDelta) + (yDelta * yDelta));
            if (u <= 0) {
                closestX = pointsX[i];
                closestY = pointsY[i];
            } else if (u >= 1) {
                closestX = pointsX[j];
                closestY = pointsY[j];
            } else {
                closestX = pointsX[i] + u * xDelta;
                closestY = pointsY[i] + u * yDelta;
            }

            if (distanceBetween(latRad, lonRad, Math.toRadians(closestY), Math.toRadians(closestX)) < warningDistance) {
                result.boundaryWarning_ = true;
                break;
            }
        }
    }

    // Lat/lon in radians
    // Result in meters
    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        double x = (lon2 - lon1) * Math.cos((lat1 + lat2) / 2);
        double y = (lat2 - lat1);
        return Math.sqrt(x * x + y * y) * 6371000.0;
    }

    private class Point2D {
        public double x;
        public double y;
        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Rectangle2D {
        public double x;
        public double y;
        public double width;
        public double height;
        public Rectangle2D (double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Creates a bitmap for the given tile
     * @param x X coordinate of tile
     * @param y Y coordinate of tile, in TMS coordinates (origin == bottom left, NOT Google coordinates)
     * @param zoomLevel
     * @return Reference to bitmap, or null if no drawing necessary
     */
    private byte[] createTileImage(int x, int y, int zoomLevel) {
        // Compute lat/lon boundaries of this tile
        Rectangle2D tileRect = tileLatLonBounds(x, y, zoomLevel);
//		Log.i(HuntBuddyBC.LOG_TAG, "tileRect for zoom=" + zoomLevel + " x=" + x + " y=" + y + ": " + tileRect);

        Bitmap bitmap = null;
        Canvas canvas = null;
        for (Map.Entry<String, RegionInfo> entry : regions_.entrySet()) {
            RegionInfo region = (RegionInfo) entry.getValue();
            if (regionIntersects(tileRect, region.bounds_)) {
                try {
                    loadBoundary(region);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to load region boundary for region " + region.name_, e);
                    return null;
                }
                if (bitmap == null) {
                    bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(bitmap);

                    canvas.drawARGB(0, 0, 0, 0); // Initial fill with fully transparent color
                }
                // Draw this region into image
                for (int shapeNum = 0; shapeNum < region.latitude_.length; shapeNum++) {
                    int numPoints = region.latitude_[shapeNum].length;
                    double[] points = new double[numPoints * 2];
                    for (int i = 0; i < numPoints; i++) {
                        points[i * 2] = region.longitude_[shapeNum][i];
                        points[(i * 2) + 1] = region.latitude_[shapeNum][i];
                    }
                    latLonToPixels(points, 0, points, 0, numPoints, x, y, zoomLevel);

                    int destIndex = 1;
                    int lastUsedPoint = 0;
                    double c2 = 1.0;
                    for (int srcIndex = 1; srcIndex < numPoints; srcIndex++) {
                        double a2b2 = Math.pow(points[srcIndex * 2] - points[lastUsedPoint * 2], 2.0) +
                                Math.pow(points[(srcIndex * 2) + 1] - points[(lastUsedPoint * 2) + 1], 2.0);
                        if (srcIndex == (numPoints - 1) || (a2b2 >= c2)) {
                            points[destIndex * 2] = points[srcIndex * 2];
                            points[(destIndex * 2) + 1] = points[(srcIndex * 2) + 1];
                            lastUsedPoint = srcIndex;
                            destIndex++;
                        }
                    }
                    Path path = new Path();
                    path.moveTo((float)points[0], (float)points[1]);
                    for (int i = 1; i < destIndex; i++)
                        path.lineTo((float)points[i * 2], (float)points[(i * 2) + 1]);
                    path.close();
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setARGB(26, 0, 255, 0); // green, 0.1 alpha
                    paint.setStrokeWidth(2.0f);
                    paint.setAlpha(127); // Outline should be darker
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)) {
            return baos.toByteArray();
        } else {
            Log.e(LOG_TAG, "Failed to encode bitmap!");
            return null;
        }
    }

    /**
     * Lat/lon uses flipped coordinate system so can't seem to get regular intersects() method to work
     * @param tileRect
     * @param regionBounds
     * @return
     */
    private boolean regionIntersects(Rectangle2D tileRect, LatLngBounds regionBounds) {
        if (regionBounds.northeast.longitude < tileRect.x) // region right edge is to the left
            return false;
        if (regionBounds.southwest.longitude > (tileRect.x + tileRect.width)) // region left edge is to the right
            return false;
        if (regionBounds.southwest.latitude > tileRect.y) // region bottom edge is above
            return false;
        if (regionBounds.northeast.latitude < (tileRect.y - tileRect.height)) // region top edge is below
            return false;
        return true;
    }

    /**
     * Converts given lat/lon in WGS84 Datum to tile coordinates. XY in Spherical Mercator EPSG:900913"
     * No error checking. It's up to the caller to ensure no array index out of bounds.
     *
     * @param src Source points in format x0, y0, x1, y1...
     * @param srcIndex Source index (point number, not array index of 2 * point number)
     * @param dest Destination points in same format
     * @param destIndex Destination index
     * @param x
     * @param y
     * @param zoom
     * @param numPoints
     */
    private void latLonToPixels(double[] src, int srcIndex, double[] dest, int destIndex, int numPoints, int x, int y, int zoom) {
        double res = resolution(zoom);
        for (int i = 0; i < numPoints; i++) {
            // Convert longitude
            double metersX = src[(srcIndex + i) * 2] * ORIGIN_SHIFT / 180.0;
            double pixelsX = (metersX + ORIGIN_SHIFT) / res;
            dest[(destIndex + i) * 2] = pixelsX - (double)(x * TILE_SIZE);
            // Convert latitude
            double tempY = Math.log(Math.tan((90.0 + src[(srcIndex + i) * 2 + 1]) * Math.PI / 360.0)) / (Math.PI / 180.0);
            double metersY = tempY * ORIGIN_SHIFT / 180.0;
            double pixelsY = (metersY + ORIGIN_SHIFT) / res;
            double tilePixelsY = pixelsY - (double)(y * TILE_SIZE);
            dest[(destIndex + i) * 2 + 1] = -(tilePixelsY - (double)TILE_SIZE);
        }
    }

    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     * @param zoom
     * @return
     */
    private double resolution(int zoom) {
        // return (2 * Math.PI * 6378137) / (TILE_SIZE * 2 ^ zoom)
        return INITIAL_RESOLUTION / (double)(1 << zoom);
    }

    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
     * @param px
     * @param py
     * @param zoom
     * @return
     */
    private Point2D pixelsToMeters(double px, double py, int zoom) {
        double res = resolution(zoom);
        double mx = px * res - ORIGIN_SHIFT;
        double my = py * res - ORIGIN_SHIFT;
        return new Point2D(mx, my);
    }

    /**
     * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84 Datum
     * @param metersX
     * @param metersY
     * @return
     */
    private Point2D metersToLatLon(double metersX, double metersY) {
        double lon = (metersX / ORIGIN_SHIFT) * 180.0;
        double lat = (metersY / ORIGIN_SHIFT) * 180.0;

        lat = 180.0 / Math.PI * (2 * Math.atan(Math.exp( lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new Point2D(lon, lat);
    }

    /**
     * Returns bounds of the given tile in latitude/longitude using WGS84 datum
     * @param x Tile number x
     * @param y Tile number y (from bottom!)
     * @param zoom
     * @return
     */
    private Rectangle2D tileLatLonBounds(int x, int y, int zoom) {
        Point2D bottomLeft = pixelsToMeters(x * TILE_SIZE, y * TILE_SIZE, zoom);
        Point2D topRight = pixelsToMeters((x + 1) * TILE_SIZE, (y + 1) * TILE_SIZE, zoom);
        bottomLeft = metersToLatLon(bottomLeft.x, bottomLeft.y);
        topRight = metersToLatLon(topRight.x, topRight.y);
        Rectangle2D result = new Rectangle2D(bottomLeft.x, topRight.y, topRight.x - bottomLeft.x, topRight.y - bottomLeft.y);
        return result;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        byte[] image = null;

        if (zoom < 10) {
            image = readTileImage(x, y, zoom); // Use prerendered tiles for larger zooms
        } else {
            int tmsY = (1 << zoom) - y - 1;
            image = createTileImage(x, tmsY, zoom);
        }

        return image == null ? NO_TILE : new Tile(TILE_SIZE, TILE_SIZE, image);
    }

    private byte[] readTileImage(int x, int y, int zoom) {
        InputStream in = null;
        ByteArrayOutputStream buffer = null;

        try {
            in = assetManager_.open(getTileFilename(x, y, zoom));
            buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[BUFFER_SIZE];

            while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            return buffer.toByteArray();
        } catch (FileNotFoundException e) {
            // No big deal if no tile is present
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to load rendered tile image", e);
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "Failed to load rendered tile image", e);
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (buffer != null) try { buffer.close(); } catch (Exception ignored) {}
        }
    }

    private String getTileFilename(int x, int y, int zoom) {
        return "tiles/" + zoom + '/' + x + '/' + y + ".png";
    }

}


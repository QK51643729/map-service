package util.function;

import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of miscellaneous utilities functions.
 *
 * @author uqdalves
 */
public class SpatialUtils implements Serializable {
	/**
	 * Earth radius in meters - AVERAGE.
	 */
	public static final int EARTH_RADIUS = 6371000;
	/**
	 * Earth radius in meters - EQUATOR.
	 */
	public static final int EARTH_RADIUS_EQUATOR = 6378137;
	/**
	 * Earth radius in meters - TROPICS.
	 */
	public static final int EARTH_RADIUS_TROPICS = 6374761;
	/**
	 * Earth radius in meters - POLES.
	 */
	public static final int EARTH_RADIUS_POLES = 6356752;
	
	/**
	 * Find the bounding box of a given set of point.
	 *
	 * @param pointList The point set.
	 * @param df        The distance function.
	 * @return The rectangle representing the bounding box.
	 */
	public static Rect getBoundingBox(List<Point> pointList, DistanceFunction df) {
		double minX = pointList.get(0).x();
		double maxX = pointList.get(0).x();
		double minY = pointList.get(0).y();
		double maxY = pointList.get(0).y();
		for (int i = 1; i < pointList.size(); i++) {
			Point p = pointList.get(i);
			if (p.x() < minX)
				minX = p.x();
			if (p.x() > maxX)
				maxX = p.x();
			if (p.y() < minY)
				minY = p.y();
			if (p.y() > maxY)
				maxY = p.y();
		}
		if (minX == maxX && minY == maxY)
			throw new IllegalArgumentException("The bounding box for point list: " + pointList.toString() + " is a point: " + minX + "," + maxY);
		return new Rect(minX, minY, maxX, maxY, df);
	}
	
	/**
	 * Check whether the points sequence a--b--c is a counter-clockwise turn.
	 *
	 * @param a First point of the sequence.
	 * @param b Second point of the sequence.
	 * @param c Third point of the sequence.
	 * @return +1 if counter-clockwise, -1 if clockwise, 0 if collinear.
	 */
	public static int isCCW(Point a, Point b, Point c) {
		double area2 = (b.x() - a.x()) * (c.y() - a.y()) -
				(c.x() - a.x()) * (b.y() - a.y());
		if (area2 < 0) return -1;
		else if (area2 > 0) return +1;
		else return 0;
	}
	
	/**
	 * Check whether the points a--b--c are collinear.
	 *
	 * @return True if a,b,c are collinear.
	 */
	public static boolean isCollinear(Point a, Point b, Point c) {
		return isCCW(a, b, c) == 0;
	}
	
	/**
	 * @param values A list of numbers to get the mean.
	 * @return The mean of the values in this list.
	 */
	public static double getMean(List<Double> values) {
		Decimal sum = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
		}
		Decimal mean = sum.divide(values.size());
		
		return mean.value();
	}
	
	/**
	 * @param values A list of numbers to get the mean.
	 * @return The mean of the values in this list.
	 */
	public static double getMean(double... values) {
		Decimal sum = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
		}
		Decimal mean = sum.divide(values.length);
		
		return mean.value();
	}
	
	/**
	 * @param list A list of numbers to get the standard deviation.
	 * @param mean The mean of the values in the list.
	 * @return The standard deviation of values in this list.
	 */
	public static double getStd(List<Double> list, double mean) {
		int size = list.size();
		Decimal diff;
		Decimal meanDecimal = new Decimal(mean);
		Decimal sumSqr = new Decimal(0);
		for (double value : list) {
			diff = meanDecimal.sub(value);
			sumSqr = sumSqr.sum(diff.pow2());
		}
		Decimal std = sumSqr.divide(size).sqrt();
		
		return std.value();
	}
	
	/**
	 * @param values A list of numbers to get the standard deviation.
	 * @return The standard deviation of values in this list.
	 */
	public static double getStd(List<Double> values) {
		int size = values.size();
		Decimal sum = new Decimal(0);
		Decimal sqr = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
			sqr = sqr.sum(value * value);
		}
		Decimal stdSqr = sqr.sub(sum.pow2().divide(size));
		Decimal std = stdSqr.divide(size - 1).sqrt();
		
		return std.value();
	}
	
	/**
	 * @param values A list of numbers to get the standard deviation.
	 * @return The standard deviation of values in this list.
	 */
	public static double getStd(double... values) {
		int size = values.length;
		Decimal sum = new Decimal(0);
		Decimal sqr = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
			sqr = sqr.sum(value * value);
		}
		Decimal stdSqr = sqr.sub(sum.pow2().divide(size));
		Decimal std = stdSqr.divide(size - 1).sqrt();
		
		return std.value();
	}
	
	/**
	 * Returns a random number between zero and the given number.
	 *
	 * @param number threshold.
	 * @return A double value with a positive sign, greater than or
	 * equal to 0.0 and less than the given number.
	 */
	public static double random(double number) {
		return number * Math.random();
	}
	
	/**
	 * Select a given number of random sample elements
	 * from the given list.
	 *
	 * @param list The list to sample.
	 * @param n    Number of sample elements.
	 * @return A list of size (n) sample elements.
	 */
	public static <T> List<T> getSample(List<T> list, final int n) {
		if (list == null) {
			throw new NullPointerException("List of elements for sampling "
					+ "cannot be null.");
		}
		if (n <= 0 || n >= list.size()) {
			throw new IllegalArgumentException("Number of samples must be positve "
					+ "and smaller than the list size.");
		}
		// make sure the original list is unchanged
		List<T> result = new ArrayList<T>(list);
		Collections.shuffle(result);
		return result.subList(0, n);
	}
	
	/**
	 * Dot product between two generic vectors V * U.
	 * Vector U and V should be of same dimension.
	 *
	 * @param v Vector V.
	 * @param u Vector U.
	 * @return Dot product V*U.
	 */
	public static double dotProduct(double[] v, double[] u) {
		if (v == null || u == null) {
			throw new NullPointerException("Vectors for dot product calculation cannot be null.");
		}
		if (u.length != v.length) {
			throw new IllegalArgumentException("Vectors should be of same size for dot product calculation.");
		}
		
		if (v.length == 0) {
			return 0;
		}
		double dotProduct = 0;
		for (int i = 0; i < v.length; i++) {
			dotProduct += v[i] * u[i];
		}
		
		return dotProduct;
	}
	
	/**
	 * The Norm (length) of this N-Dimensional vector.
	 *
	 * @param v N-dimensional vector.
	 * @return The norm of V.
	 */
	public static double norm(double[] v) {
		if (v == null) {
			throw new NullPointerException("Vector cannot be null");
		}
		
		if (v.length == 0) {
			return 0;
		}
		double norm = 0;
		for (double value : v) {
			norm += value * value;
		}
		
		return Math.sqrt(norm);
	}
	
	/* Coordinate system converter */
	
	/**
	 * Check if the current point is within the China region. The China region uses GCJ-02 coordinate system which cannot be displayed
	 * correctly on OpenStreetMap.
	 * <p>
	 * This function is only used when converting between GCJ-02 and WGS84.
	 *
	 * @param lon Longitude of the point.
	 * @param lat Latitude of the point.
	 * @return True when it is outside China area. Otherwise false.
	 */
	public static boolean outOfChina(double lon, double lat) {
		return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
	}
	
	/**
	 * Calculate the coordinate drift between GCJ-02 and WGS84.
	 *
	 * @param lon Input point longitude.
	 * @param lat Input point latitude.
	 * @return delta[0] is lonDiff，delta[1] is latDiff
	 */
	private static double[] delta(double lon, double lat) {
		double[] delta = new double[2];
		double a = EARTH_RADIUS_EQUATOR;    // earth radius
		double ee = 0.00669342162296594323;
		double dLon = transformLon(lon - 105.0, lat - 35.0);
		double dLat = transformLat(lon - 105.0, lat - 35.0);
		double radLat = lat / 180.0 * Math.PI;
		double magic = Math.sin(radLat);
		magic = 1 - ee * magic * magic;
		double sqrtMagic = Math.sqrt(magic);
		delta[0] = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
		delta[1] = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
		return delta;
	}
	
	/**
	 * Used in <tt>delta()</tt> when converting longitude from GCJ-02 to WGS84 or vice versa.
	 *
	 * @param x The current longitude.
	 * @param y The current latitude.
	 * @return The longitude after converter.
	 */
	private static double transformLon(double x, double y) {
		double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
		ret +=
				(150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
		return ret;
	}
	
	/**
	 * Used in <tt>delta()</tt> when converting latitude from GCJ-02 to WGS84 or vice versa.
	 *
	 * @param x The current longitude.
	 * @param y The current latitude.
	 * @return The latitude after converter.
	 */
	private static double transformLat(double x, double y) {
		double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
		return ret;
	}
	
	/**
	 * Convert GCJ-02 coordinate to WGS84.
	 *
	 * @param lon The input longitude.
	 * @param lat The input latitude.
	 * @return The output longitude and latitude, Pair(lon,lat)
	 */
	public static Pair<Double, Double> convertGCJ2WGS(double lon, double lat) {
		DecimalFormat decFor = new DecimalFormat(".00000");
		// the GCJ-02 is only used in China region
		if (outOfChina(lon, lat)) {
			System.out.println("The current point is not inside China: (" + lon + "," + lat + ")");
			return new Pair<>(lon, lat);
		}
		double[] delta = delta(lon, lat);
		return new Pair<>(Double.parseDouble(decFor.format(lon - delta[0])), Double.parseDouble(decFor.format(lat - delta[1])));
	}
	
	/**
	 * Convert WGS84 coordinate to GCJ-02.
	 *
	 * @param lon The input longitude.
	 * @param lat The input latitude.
	 * @return The output longitude and latitude, Pair(lon,lat).
	 */
	public static Pair<Double, Double> convertWGS2GCJ(double lon, double lat) {
		DecimalFormat decFor = new DecimalFormat(".00000");
		// the GCJ-02 is only used in China region
		if (outOfChina(lon, lat)) {
			System.out.println("The current point is not inside China: (" + lon + "," + lat + ")");
			return new Pair<>(lon, lat);
		}
		double[] delta = delta(lon, lat);
		return new Pair<>(Double.parseDouble(decFor.format(lon + delta[0])), Double.parseDouble(decFor.format(lat + delta[1])));
	}
	
	/**
	 * Convert WGS84 coordinate to Mercator projection
	 *
	 * @param lon The input longitude.
	 * @param lat The input latitude.
	 * @return The projected point, Pair(x,y).
	 */
	public static Pair<Double, Double> convertWGS2UTM(double lon, double lat) {
		
		Pair<Integer, Character> zoneInfo = findUTMZone(lon, lat);
		return convertWGS2UTM(lon, lat, zoneInfo._1(), zoneInfo._2());
	}
	
	/**
	 * Convert GCJ-02 coordinate to WGS84.
	 *
	 * @param lon The input longitude.
	 * @param lat The input latitude.
	 * @return The output longitude and latitude, Pair(lon,lat)
	 */
	public static Pair<Double, Double> convertGCJ2UTM(double lon, double lat) {
		DecimalFormat decFor = new DecimalFormat(".00");
		// the GCJ-02 is only used in China region
		if (outOfChina(lon, lat)) {
			System.out.println("The current point is not inside China: (" + lon + "," + lat + ")");
			return new Pair<>(lon, lat);
		}
		Pair<Double, Double> wgsPoint = convertGCJ2WGS(lon, lat);
		Pair<Double, Double> utmPoint = convertWGS2UTM(wgsPoint._1(), wgsPoint._2());
		return new Pair<>(Double.parseDouble(decFor.format(utmPoint._1())), Double.parseDouble(decFor.format(utmPoint._2())));
	}
	
	/**
	 * Find the corresponding zone and letter in Mercator projection given a WGS84 point.
	 *
	 * @param lon The input longitude.
	 * @param lat The input latitude.
	 * @return The corresponding zone and letter, Pair(zone,letter).
	 */
	public static Pair<Integer, Character> findUTMZone(double lon, double lat) {
		int zone;
		char letter;
		zone = (int) Math.floor(lon / 6 + 31);
		if (lat < -72)
			letter = 'C';
		else if (lat < -64)
			letter = 'D';
		else if (lat < -56)
			letter = 'E';
		else if (lat < -48)
			letter = 'F';
		else if (lat < -40)
			letter = 'G';
		else if (lat < -32)
			letter = 'H';
		else if (lat < -24)
			letter = 'J';
		else if (lat < -16)
			letter = 'K';
		else if (lat < -8)
			letter = 'L';
		else if (lat < 0)
			letter = 'M';
		else if (lat < 8)
			letter = 'N';
		else if (lat < 16)
			letter = 'P';
		else if (lat < 24)
			letter = 'Q';
		else if (lat < 32)
			letter = 'R';
		else if (lat < 40)
			letter = 'S';
		else if (lat < 48)
			letter = 'T';
		else if (lat < 56)
			letter = 'U';
		else if (lat < 64)
			letter = 'V';
		else if (lat < 72)
			letter = 'W';
		else
			letter = 'X';
		
		return new Pair<>(zone, letter);
	}
	
	/**
	 * Convert WGS84 coordinate to Mercator projection given zone and letter information.
	 *
	 * @param lon    The input longitude.
	 * @param lat    The input latitude.
	 * @param zone   The zone value.
	 * @param letter The letter information.
	 * @return The Mercator coordinates.
	 */
	public static Pair<Double, Double> convertWGS2UTM(double lon, double lat, int zone, char letter) {
		DecimalFormat decFor = new DecimalFormat(".00");
		double x;
		double y;
		x = 0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow((1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)), 0.5) * (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2) / 3) + 500000;
		x = Math.round(x * 100) * 0.01;
		y = (Math.atan(Math.tan(lat * Math.PI / 180) / Math.cos((lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180))) - lat * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(1 + 0.006739496742 * Math.pow(Math.cos(lat * Math.PI / 180), 2)) * (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin((lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180))) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin((lon * Math.PI / 180 - (6 * zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) + 0.9996 * 6399593.625 * (lat * Math.PI / 180 - 0.005054622556 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + 4.258201531e-05 * (3 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 4 - 1.674057895e-07 * (5 * (3 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 4 + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 3);
		if (letter < 'M')
			y = y + 10000000;
		y = Math.round(y * 100) * 0.01;
		return new Pair<>(Double.parseDouble(decFor.format(x)), Double.parseDouble(decFor.format(y)));
	}
	
	/**
	 * Convert Mercator coordinate to WGS84 given zone and letter information.
	 *
	 * @param x      The x coordinate under UTM.
	 * @param y      The y coordinate under UTM.
	 * @param zone   The zone value.
	 * @param letter The letter information, the letter should be UPPERCASE.
	 * @return The WGS84 coordinates.
	 */
	public static Pair<Double, Double> convertUTM2WGS(double x, double y, int zone, char letter) {
		DecimalFormat decFor = new DecimalFormat(".00000");
		double lon;
		double lat;
		double Hem;
		if (letter > 'M')
			Hem = 'N';
		else
			Hem = 'S';
		double north;
		if (Hem == 'S')
			north = y - 10000000;
		else
			north = y;
		lon = Math.atan((Math.exp((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north
				/ 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))
				- Math.exp(-(x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
				* (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north
				/ 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996
				* 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north
				/ 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2
				* north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2
				* north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625
				/ Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north
				/ 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) * 180 / Math.PI + zone * 6 - 183;
		lon = Math.round(lon * 10000000);
		lon = lon / 10000000;
		lat = (north / 6366197.724 / 0.9996 + (1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) - 0.006739496742
				* Math.sin(north / 6366197.724 / 0.9996) * Math.cos(north / 6366197.724 / 0.9996) * (Math.atan(Math.cos(Math.atan((Math.exp((x - 500000)
				/ (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
				* (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))
				- Math.exp(-(x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
				* (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north
				/ 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996
				* 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north
				/ 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2
				* north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2
				* north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625
				/ Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north
				/ 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724
				/ 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
				+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996)
				/ 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742
				* 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north
				/ 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996)
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3)) / (0.9996
				* 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742
				* Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north
				/ 6366197.724 / 0.9996) * 3 / 2) * (Math.atan(Math.cos(Math.atan((Math.exp((x - 500000) / (0.9996 * 6399593.625
				/ Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742
				* Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)) - Math.exp(-(x - 500000) / (0.9996
				* 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742
				* Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))) / 2 / Math.cos((north - 0.9996 * 6399593.625
				* (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724
				/ 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north
				/ 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
				/ 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724
				/ 0.9996) / 2) + Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4
				+ Math.sin(2 * north / 6366197.724 / 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north
				/ 6366197.724 / 0.9996), 2)) / 3)) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724
				/ 0.9996), 2)))) * (1 - 0.006739496742 * Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
				* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
				+ north / 6366197.724 / 0.9996))) * Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742
				* 3 / 4 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.pow(0.006739496742 * 3 / 4, 2)
				* 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724
				/ 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 - Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 *
				(5 * (3 * (north / 6366197.724 / 0.9996 + Math.sin(2 * north / 6366197.724 / 0.9996) / 2) + Math.sin(2 * north / 6366197.724
						/ 0.9996) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 4 + Math.sin(2 * north / 6366197.724 / 0.9996)
						* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) / 3))
				/ (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1 - 0.006739496742
				* Math.pow((x - 500000) / (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))), 2)
				/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)) + north / 6366197.724 / 0.9996)) - north / 6366197.724 / 0.9996)) * 180 / Math.PI;
		lat = Math.round(lat * 10000000);
		lat = lat / 10000000;
		return new Pair<>(Double.parseDouble(decFor.format(lon)), Double.parseDouble(decFor.format(lat)));
	}
	
	/**
	 * Change the coordination system of a trajectory from GCJ-02 to WGS84.
	 *
	 * @param traj The input trajectory.
	 */
	public static void convertTrajGCJ2WGS(Trajectory traj) {
		for (TrajectoryPoint trajPoint : traj) {
			Pair<Double, Double> wgsPosition = SpatialUtils.convertGCJ2WGS(trajPoint.x(), trajPoint.y());
			trajPoint.setPoint(wgsPosition._1(), wgsPosition._2(), trajPoint.getDistanceFunction());
		}
	}
	
	/**
	 * Change the coordination system of a trajectory from WGS84 to UTM.
	 *
	 * @param traj The input trajectory.
	 */
	public static void convertTrajWGS2UTM(Trajectory traj) {
		DistanceFunction distFunc = new EuclideanDistanceFunction();
		traj.setDistanceFunction(distFunc);
		for (TrajectoryPoint trajPoint : traj) {
			Pair<Double, Double> utmPosition = SpatialUtils.convertWGS2UTM(trajPoint.x(), trajPoint.y());
			trajPoint.setPoint(utmPosition._1(), utmPosition._2(), distFunc);
		}
	}
	
	/**
	 * Change the coordination system of a trajectory from GCJ-02 to UTM.
	 *
	 * @param traj The input trajectory.
	 */
	public static void convertTrajGCJ2UTM(Trajectory traj) {
		DistanceFunction distFunc = new EuclideanDistanceFunction();
		traj.setDistanceFunction(distFunc);
		for (TrajectoryPoint trajPoint : traj) {
			Pair<Double, Double> wgsPosition = SpatialUtils.convertGCJ2WGS(trajPoint.x(), trajPoint.y());
			Pair<Double, Double> utmPosition = SpatialUtils.convertWGS2UTM(wgsPosition._1(), wgsPosition._2());
			trajPoint.setPoint(utmPosition._1(), utmPosition._2(), distFunc);
		}
	}
	
	/**
	 * Change the coordination system of a trajectory from UTM to WGS84.
	 *
	 * @param traj The input trajectory.
	 */
	public static void convertTrajUTM2WGS(Trajectory traj, int zone, char letter) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		traj.setDistanceFunction(distFunc);
		for (TrajectoryPoint trajPoint : traj) {
			Pair<Double, Double> wgsPosition = SpatialUtils.convertUTM2WGS(trajPoint.x(), trajPoint.y(), zone, letter);
			trajPoint.setPoint(wgsPosition._1(), wgsPosition._2(), distFunc);
		}
	}
	
	
	/**
	 * Change the coordination system of the input map from GCJ-02 to WGS84.
	 *
	 * @param map The input map.
	 */
	public static void convertMapGCJ2WGS(RoadNetworkGraph map) {
		for (RoadNode node : map.getAllTypeOfNodes()) {
			Pair<Double, Double> wgsPosition = SpatialUtils.convertGCJ2WGS(node.lon(), node.lat());
			node.setLocation(wgsPosition._1(), wgsPosition._2());
		}
		map.updateBoundary();
	}
	
	/**
	 * Change the coordination system of the input map from WGS84 to UTM. The distance function should be changed.
	 *
	 * @param map The input map.
	 */
	public static void convertMapWGS2UTM(RoadNetworkGraph map) {
		DistanceFunction distFunc = new EuclideanDistanceFunction();
		map.setDistanceFunction(distFunc);
		for (RoadNode node : map.getAllTypeOfNodes()) {
			Pair<Double, Double> utmPosition = SpatialUtils.convertWGS2UTM(node.lon(), node.lat());
			node.setLocation(utmPosition._1(), utmPosition._2());
			node.setDistFunc(distFunc);
		}
		for (RoadWay way : map.getWays()) {
			way.setDistFunc(distFunc);
		}
		map.updateBoundary();
	}
	
	/**
	 * Change the coordination system of the input map from GCJ-02 to UTM.
	 *
	 * @param map The input map.
	 */
	public static void convertMapGCJ2UTM(RoadNetworkGraph map) {
		DistanceFunction distFunc = new EuclideanDistanceFunction();
		map.setDistanceFunction(distFunc);
		for (RoadNode node : map.getAllTypeOfNodes()) {
			Pair<Double, Double> wgsPosition = SpatialUtils.convertGCJ2WGS(node.lon(), node.lat());
			Pair<Double, Double> utmPosition = SpatialUtils.convertWGS2UTM(wgsPosition._1(), wgsPosition._2());
			node.setLocation(utmPosition._1(), utmPosition._2());
			node.setDistFunc(distFunc);
		}
		for (RoadWay way : map.getWays()) {
			way.setDistFunc(distFunc);
		}
		map.updateBoundary();
	}
	
	/**
	 * Change the coordination system of the input map from UTM to WGS84.
	 *
	 * @param map The input map.
	 */
	public static void convertMapUTM2WGS(RoadNetworkGraph map, int zone, char letter) {
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		map.setDistanceFunction(distFunc);
		for (RoadNode node : map.getAllTypeOfNodes()) {
			Pair<Double, Double> wgsPosition = SpatialUtils.convertUTM2WGS(node.lon(), node.lat(), zone, letter);
			node.setLocation(wgsPosition._1(), wgsPosition._2());
			node.setDistFunc(distFunc);
		}
		for (RoadWay way : map.getWays()) {
			way.setDistFunc(distFunc);
		}
		map.updateBoundary();
	}
}

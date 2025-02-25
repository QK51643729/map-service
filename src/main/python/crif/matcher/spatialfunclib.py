#
# Spatial function library.
# Author: James P. Biagioni (jbiagi1@uic.edu)
# Company: University of Illinois at Chicago
# Created: 3/3/10
#

import math

#
# Global constants.
#
METERS_PER_DEGREE_LATITUDE = 111070.34306591158
METERS_PER_DEGREE_LONGITUDE = 83044.98918812413
EARTH_RADIUS = 6371000.0  # meters


#
# Returns the distance in meters between two points specified in degrees, using the default (Haversine formula) method.
#
def distance(a, b):
    return haversine_distance(a, b)


#
# Returns whether the coordinates for two points A and B are the same.
#
def same_coords(a_lat, a_lon, b_lat, b_lon):
    if (a_lat == b_lat and a_lon == b_lon):
        return True
    else:
        return False


#
# Returns the distance in meters between two points specified in degrees, using the Haversine formula. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
#
def haversine_distance((a_lat, a_lon), (b_lat, b_lon)):
    if (same_coords(a_lat, a_lon, b_lat, b_lon)):
        return 0.0

    dLat = math.radians(b_lat - a_lat)
    dLon = math.radians(b_lon - a_lon)

    a = math.sin(dLat / 2.0) * math.sin(dLat / 2.0) + math.cos(math.radians(a_lat)) * math.cos(math.radians(b_lat)) * math.sin(
        dLon / 2.0) * math.sin(dLon / 2.0)

    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    d = EARTH_RADIUS * c

    return d


#
# Returns the distance in meters between two points specified in degrees, using the Spherical Law of Cosines. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
#
def slc_distance(a_lat, a_lon, b_lat, b_lon):
    if (same_coords(a_lat, a_lon, b_lat, b_lon)):
        return 0.0

    a_lat = math.radians(a_lat)
    a_lon = math.radians(a_lon)
    b_lat = math.radians(b_lat)
    b_lon = math.radians(b_lon)

    a = math.sin(a_lat) * math.sin(b_lat) + math.cos(a_lat) * math.cos(b_lat) * math.cos(b_lon - a_lon)
    d = math.acos(a) * EARTH_RADIUS

    return d


#
# Returns the path bearing between two points specified in degrees. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
#
def path_bearing(a_lat, a_lon, b_lat, b_lon):
    a_lat = math.radians(a_lat)
    a_lon = math.radians(a_lon)
    b_lat = math.radians(b_lat)
    b_lon = math.radians(b_lon)

    y = math.sin(b_lon - a_lon) * math.cos(b_lat)
    x = math.cos(a_lat) * math.sin(b_lat) - math.sin(a_lat) * math.cos(b_lat) * math.cos(b_lon - a_lon)

    bearing = math.atan2(y, x)

    return math.fmod(math.degrees(bearing) + 360.0, 360.0)


#
# Returns the destination point given distance and bearing from a start point. Formula adapted from: http://www.movable-type.co.uk/scripts/latlong.html
#
def destination_point(a_lat, a_lon, bearing, distance):
    angular_distance = (distance / 6371000.0)  # meters
    bearing = math.radians(bearing)

    a_lat = math.radians(a_lat)
    a_lon = math.radians(a_lon)

    b_lat = math.asin(math.sin(a_lat) * math.cos(angular_distance) + math.cos(a_lat) * math.sin(angular_distance) * math.cos(bearing))
    b_lon = a_lon + math.atan2(math.sin(bearing) * math.sin(angular_distance) * math.cos(a_lat),
                               math.cos(angular_distance) - math.sin(a_lat) * math.sin(b_lat))

    b_lon = math.fmod((b_lon + (3 * math.pi)), (2 * math.pi)) - math.pi

    return (math.degrees(b_lat), math.degrees(b_lon))


#
# Returns the coordinates of a point some "fraction_along" the line AB.
#
def point_along_line(a_lat, a_lon, b_lat, b_lon, fraction_along):
    c_lon = a_lon + (fraction_along * (b_lon - a_lon))
    c_lat = a_lat + (fraction_along * (b_lat - a_lat))

    return (c_lat, c_lon)


def distance_to_segment(segment, point):
    """ This should work fine for small distances. Not sure about longer distances 
    once the curvature of the earth starts playing a significant role."""

    projected_point = project_onto_segment(segment, point)
    return distance(projected_point, point)


def dotProduct((x1, y1), (x2, y2)):
    return x1 * x2 + y1 * y2


# calculates vector length
def vectorLen(x1, y1):
    return math.sqrt(x1 * x1 + y1 * y1)


def project_onto_segment(((aX, aY), (bX, bY)), (cX, cY)):
    """ Version from Tomas Gerlich's match_lib"""

    if dotProduct(((bX - aX), (bY - aY)), ((cX - aX), (cY - aY))) <= 0:
        p = (aX, aY)
    elif dotProduct(((aX - bX), (aY - bY)), ((cX - bX), (cY - bY))) <= 0:
        p = (bX, bY)
    else:
        temp1 = dotProduct(((bX - aX), (bY - aY)), ((cX - aX), (cY - aY)))
        b2Len = vectorLen(bX - aX, bY - aY)
        b1Len = temp1 / b2Len
        fraction = b1Len / b2Len
        x = aX + fraction * (bX - aX)
        y = aY + fraction * (bY - aY)
        p = (x, y)
    return p


def projection_onto_line(a_lat, a_lon, b_lat, b_lon, c_lat, c_lon, debug=False):
    """ Returns the orthogonal projection of point C onto line AB, and its fraction along line AB. Version by James Biagioni"""

    ab_angle = path_bearing(a_lat, a_lon, b_lat, b_lon)
    if (debug): print "ab_angle: " + str(ab_angle) + " degrees"

    ac_angle = path_bearing(a_lat, a_lon, c_lat, c_lon)
    if (debug): print "ac_angle: " + str(ac_angle) + " degrees"

    ab_length = distance(a_lat, a_lon, b_lat, b_lon)
    if (debug): print "ab_length: " + str(ab_length) + " meters"

    ac_length = distance(a_lat, a_lon, c_lat, c_lon)
    if (debug): print "ac_length: " + str(ac_length) + " meters"

    angle_diff = (ac_angle - ab_angle)
    if (debug): print "angle_diff: " + str(angle_diff) + " degrees"

    meters_along = (ac_length * math.cos(math.radians(angle_diff)))
    if (debug): print "meters_along: " + str(meters_along) + " meters"

    if (ab_length == 0.0):
        fraction_along = 0.0
    else:
        fraction_along = (meters_along / ab_length)
    if (debug): print "fraction_along: " + str(fraction_along)

    projected_point = point_along_line(a_lat, a_lon, b_lat, b_lon, fraction_along)
    if (debug): print "projected_point: " + str(projected_point)

    cproj_length = distance(c_lat, c_lon, projected_point[0], projected_point[1])
    if (debug): print "cproj_length: " + str(cproj_length) + " meters"

    cproj_angle = path_bearing(c_lat, c_lon, projected_point[0], projected_point[1])
    if (debug): print "cproj_angle: " + str(cproj_angle) + " degrees"

    return (projected_point, fraction_along, cproj_length)

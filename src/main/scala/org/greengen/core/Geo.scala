package org.greengen.core

object Geo {

  import Coordinate._

  type Meter = Double

  val EarthRadius: Meter = 6.371008e6
  val EarthRadiusEquator: Meter = 6378137.0
  val EarthRadiusPoles: Meter = 6356752.0

  def distance(p1: LatLong, p2: LatLong): Meter =
    haversine(p1,p2)

  def distance(p1: XYZ, p2: XYZ): Meter =
    euclidean(p1,p2)

  // Earth radius calculation in meters
  // https://rechneronline.de/earth-radius/
  // latitude B, radius R, radius at equator r1, radius at pole r2
  // R = √ [ (r1² * cos(B))² + (r2² * sin(B))² ] / [ (r1 * cos(B))² + (r2 * sin(B))² ]
  def earthRadius(pos: LatLong): Meter = {
    val R1 = EarthRadiusEquator
    val R2 = EarthRadiusPoles
    val lat = d2r(pos.latitude).value
    val num = math.pow(R1*R1*math.cos(lat), 2) + math.pow(R2*R2*math.sin(lat), 2)
    val denom = math.pow(R1*math.cos(lat), 2) + math.pow(R2*math.sin(lat), 2)
    math.sqrt(num / denom)
  }



  def euclidean(p1: XYZ, p2: XYZ): Meter =
    math.sqrt(math.pow(p1.x-p2.x, 2) + math.pow(p1.y-p2.y, 2) + math.pow(p1.z-p2.z, 2))

  // Haversine formula for distance
  def haversine(p1: LatLong, p2: LatLong): Meter = {
    val (lat1, lng1) = p1.asPair
    val (lat2, lng2) = p2.asPair
    val (dLat, dLng) = (d2r(Degree(lat2.value - lat1.value)).value, d2r(Degree(lng2.value - lng1.value)).value)
    val a = math.pow(math.sin(dLat/2.0),2) +
      (math.cos(d2r(lat1).value) * math.cos(d2r(lat2).value)) * math.pow(math.sin(dLng/2.0),2)
    val c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    EarthRadius * c // Distance in meters
  }


  // TODO
//  % Half-way point
//    % https://www.movable-type.co.uk/scripts/latlong.html
//  % Bx = cos φ2 ⋅ cos Δλ
//  % By = cos φ2 ⋅ sin Δλ
//  % φm = atan2( sin φ1 + sin φ2, √((cos φ1 + Bx)² + By² )
//  % λm = λ1 + atan2(By, cos(φ1)+Bx)
//  halfway(#latlong{lat=Lat1, long=Lng1}, #latlong{lat=Lat2, long=Lng2})
//  when is_number(Lat1), is_number(Lng1),
//  is_number(Lat2), is_number(Lng2) ->
//  [RLng1, RLat1, RLng2, RLat2] = [deg2rad(Deg) || Deg <- [Lng1, Lat1, Lng2, Lat2]],
//  Bx = math:cos(RLat2) * math:cos(RLng2-RLng1),
//  By = math:cos(RLat2) * math:sin(RLng2-RLng1),
//  Lat = math:atan2(math:sin(RLat1) + math:sin(RLat2), math:sqrt(math:pow(math:cos(RLat1) + Bx, 2) + math:pow(By, 2))),
//  Lng = RLng1 + math:atan2(By, math:cos(RLat1) + Bx),
//  #latlong{lat=rad2deg(Lat), long=rad2deg(Lng)}.
  def midPoint(p1: LatLong, p2: LatLong) = ???


  // Nearest neighbours search

  def nearestNeighbours(k: Int)(pos: LatLong)(points: Seq[LatLong]): Seq[(LatLong, Meter)] =
    nearestNeighbours(Geo.haversine _)(k)(pos)(points)

  // Linear sequence nn search
  def nearestNeighbours[T](distfn: (T,T) => Meter)
                          (k: Int)
                          (pos: T)
                          (points: Seq[T]): Seq[(T, Meter)] =
    points.map(p => (p, distfn(pos, p))).sortBy(_._2).take(k)

}

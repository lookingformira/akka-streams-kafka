object model {

  case class TaxiRide(
                       rideId: Long = 0L,
                       isStart: String,
                       startTime: String,
                       endTime: String,
                       startLon: Double,
                       startLat: Double,
                       endLon: Double,
                       endLat: Double,
                       passengerCnt: Short,
                       taxiId: Long,
                       driverId: Long
                     )

  case class TaxiRideEvent(rideId: Long = 0L, passengerCnt: Short, taxiId: Long, driverId: Long)

}


import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanInShape2, SinkShape, SourceShape, UniformFanInShape, UniformFanOutShape}
import akka.stream.scaladsl.{Balance, Broadcast, Concat, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import model.{TaxiRide, TaxiRideEvent}


object Graphs extends App {
  implicit val system = ActorSystem("OtusGraph")
  implicit val materializer = ActorMaterializer()

  val sourceTaxiRide = Source(
    List(
      TaxiRide(5, "START", "2020-01-01T12:01:40Z", "1970-01-01T00:00:00Z", -73.85884, 40.77057, -73.75468, 40.903137, 1, 2013000087, 2013000087),
      TaxiRide(1, "START", "2020-01-01T12:00:20Z", "1970-01-01T00:00:00Z", -73.76764, 40.88664, -73.843834, 40.78967, 3, 2013000185, 2013000185),
      TaxiRide(2, "START", "2020-01-01T12:00:40Z", "1970-01-01T00:00:00Z", -73.85604, 40.77413, -73.80203, 40.84287, 0, 2013000108, 2013000108),
      TaxiRide(3, "START", "2020-01-01T12:01:00Z", "1970-01-01T00:00:00Z", -73.86453, 40.763325, -73.84797, 40.7844, 3, 2013000134, 2013000134),
      TaxiRide(4, "START", "2020-01-01T12:01:20Z", "1970-01-01T00:00:00Z", -73.86093, 40.767902, -73.781784, 40.868633, 2, 2013000062, 2013000062),
      TaxiRide(10, "START", "2020-01-01T12:03:20Z", "1970-01-01T00:00:00Z", -73.814865, 40.826534, -73.77973, 40.871254, 3, 2013000113, 2013000113),
      TaxiRide(8, "START", "2020-01-01T12:02:40Z", "1970-01-01T00:00:00Z", -73.769424, 40.884365, -73.77441, 40.87803, 4, 2013000164, 2013000164),
      TaxiRide(6, "START", "2020-01-01T12:02:00Z", "1970-01-01T00:00:00Z", -73.764946, 40.890068, -73.87163, 40.754288, 3, 2013000011, 2013000011),
      TaxiRide(7, "START", "2020-01-01T12:02:20Z", "1970-01-01T00:00:00Z", -73.816895, 40.823956, -73.8498, 40.782078, 1, 2013000036, 2013000036),
      TaxiRide(11, "START", "2020-01-01T12:03:40Z", "1970-01-01T00:00:00Z", -73.763145, 40.89236, -73.831, 40.805996, 3, 2013000138, 2013000138))
  )

  val journalLog = Flow[TaxiRide].map(r => TaxiRideEvent(r.rideId, r.passengerCnt, r.taxiId, r.driverId))
  val onlyRideId = Flow[TaxiRide].map(_.rideId + 1000)
  val output = Sink.foreach[(TaxiRideEvent, Long)](println)

  val sourceToSinkGraph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast: UniformFanOutShape[TaxiRide, TaxiRide] = builder.add(Broadcast[TaxiRide](2))

      sourceTaxiRide ~> broadcast

      val zip: FanInShape2[TaxiRideEvent, Long, (TaxiRideEvent, Long)] = builder.add(Zip[TaxiRideEvent, Long])

      broadcast.out(0) ~> journalLog ~> zip.in0
      broadcast.out(1) ~> onlyRideId ~> zip.in1

      zip.out ~> output

      ClosedShape
    }
  )

//  sourceToSinkGraph.run()


  val journalSink = Sink.foreach[(TaxiRideEvent)](println)
  val logicSink = Sink.foreach[(Long)](println)

  val sourceToTwoSinkGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[TaxiRide](2))

      sourceTaxiRide ~> broadcast

      broadcast.out(0) ~> journalLog ~> journalSink
      broadcast.out(1) ~> onlyRideId ~> logicSink

      ClosedShape
    }
  )

//  sourceToTwoSinkGraph.run()


  val input = Source(1 to 1000)
  import scala.concurrent.duration._

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.foreach[Int](x => println(s"First sink: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Second sink: $x"))

  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge: UniformFanInShape[Int, Int] = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

/*
      fastSource ~> merge.in(0)
      slowSource ~> merge.in(1)
      merge.out ~> balance
      balance.out(0) ~> sink1
      balance.out(1) ~> sink2
*/

          fastSource ~> merge ~> balance ~> sink1
          slowSource ~> merge
          balance ~> sink2

      ClosedShape
    }
  )

//  balanceGraph.run()


  val sourceTaxiRideInProgress = Source(
    List(
      TaxiRide(5, "START", "2020-01-01T12:01:40Z", "1970-01-01T00:00:00Z", -73.85884, 40.77057, -73.75468, 40.903137, 1, 2013000087, 2013000087),
      TaxiRide(1, "START", "2020-01-01T12:00:20Z", "1970-01-01T00:00:00Z", -73.76764, 40.88664, -73.843834, 40.78967, 3, 2013000185, 2013000185),
      TaxiRide(2, "START", "2020-01-01T12:00:40Z", "1970-01-01T00:00:00Z", -73.85604, 40.77413, -73.80203, 40.84287, 0, 2013000108, 2013000108),
      TaxiRide(3, "START", "2020-01-01T12:01:00Z", "1970-01-01T00:00:00Z", -73.86453, 40.763325, -73.84797, 40.7844, 3, 2013000134, 2013000134),
      TaxiRide(4, "START", "2020-01-01T12:01:20Z", "1970-01-01T00:00:00Z", -73.86093, 40.767902, -73.781784, 40.868633, 2, 2013000062, 2013000062))
  )

  val sourceTaxiRideFinished = Source(
    List(
      TaxiRide(5, "FINISHED", "2020-01-01T12:01:40Z", "1970-01-01T00:00:00Z", -73.85884, 40.77057, -73.75468, 40.903137, 1, 2013000087, 2013000087),
      TaxiRide(1, "FINISHED", "2020-01-01T12:00:20Z", "1970-01-01T00:00:00Z", -73.76764, 40.88664, -73.843834, 40.78967, 3, 2013000185, 2013000185),
      TaxiRide(2, "FINISHED", "2020-01-01T12:00:40Z", "1970-01-01T00:00:00Z", -73.85604, 40.77413, -73.80203, 40.84287, 0, 2013000108, 2013000108),
      TaxiRide(3, "FINISHED", "2020-01-01T12:01:00Z", "1970-01-01T00:00:00Z", -73.86453, 40.763325, -73.84797, 40.7844, 3, 2013000134, 2013000134),
      TaxiRide(4, "FINISHED", "2020-01-01T12:01:20Z", "1970-01-01T00:00:00Z", -73.86093, 40.767902, -73.781784, 40.868633, 2, 2013000062, 2013000062))
  )


  val sourceGraph: Source[TaxiRide, NotUsed] = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[TaxiRide](2))

      sourceTaxiRideInProgress ~> concat
      sourceTaxiRideFinished ~> concat

      SourceShape(concat.out)
    }
  )

//  sourceGraph.to(Sink.foreach(println)).run()


  val openSink1 = Sink.foreach[TaxiRide](x => println(s"In sink 1 Kafka: $x"))
  val openSink2 = Sink.foreach[TaxiRide](x => println(s"In sink 2 Cassandra: $x"))

  val sinkGraph: Sink[TaxiRide, NotUsed] = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[TaxiRide](2))

      broadcast ~> openSink1
      broadcast ~> openSink2

      SinkShape(broadcast.in)
    }
  )

  sourceTaxiRideInProgress.to(sinkGraph).run()


}

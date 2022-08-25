import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}


object Backpressure extends App {
  val sourceFast = Source(1 to 1000)

  val flow = Flow[Int].map { el =>
    println(s"Flow inside: $el")
    el + 100
  }

  val flowWithBuffer = flow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)

  val sinkSlow = Sink.foreach[Int] { el =>
    Thread.sleep(1000)
    println(s"Sink inside: $el")
  }

  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  sourceFast.async
    .via(flowWithBuffer).async
    .to(sinkSlow)
    .run()





}

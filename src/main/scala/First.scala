import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object First extends App {
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 10)

  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x + 1)

  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

  val graph1: RunnableGraph[NotUsed] = source.to(sink)
  val graph2: RunnableGraph[NotUsed] = source.via(flow).to(sink)


//  graph2.run()

  /**
   * Next Example
   * */

  val simpleSource = Source(1 to 1000)

  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)

  val simpleSink = Sink.foreach[Int](println)

  val runnableGraph: RunnableGraph[NotUsed] =
    simpleSource
      .via(simpleFlow)
      .via(simpleFlow2)
      .to(simpleSink)

  //  runnableGraph.run()


  val hardFlow3 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x + 1
  }
  val hardFlow4 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x * 10
  }

  simpleSource
    .via(hardFlow3)
    .via(hardFlow4)
    .via(hardFlow4)
    .to(simpleSink)
//  .run()



  Source(1 to 3).async
    .map(element => { println(s"Flow A: $element"); element }).async
    .map(element => { println(s"Flow B: $element"); element }).async
    .map(element => { println(s"Flow C: $element"); element })
  .runWith(Sink.ignore)


}

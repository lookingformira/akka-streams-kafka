import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}


object Pipeline extends App {
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)
  val producerConfig = config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  val sourceKafka = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("akka-stream-test-in"))

  sourceKafka.async  // 1 core
    .map{element => element.value().toUpperCase}.async // 1 core
    .map(value => new ProducerRecord[String, String]("akka-stream-test-out", value))
    .runWith(Producer.plainSink(producerSettings))
}

// topic with 50 part => 50 akka stream app

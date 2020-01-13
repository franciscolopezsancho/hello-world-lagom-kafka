package com.example.aloha.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.scaladsl.Flow
import com.example.aloha.api.{AlohaService, GreetingMessage}
import com.example.consumer.api.ConsumerService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.Future

class AlohaLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new AlohaApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AlohaApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[AlohaService])
}

abstract class AlohaApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AlohaService](wire[AlohaServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = AlohaSerializerRegistry

  lazy val helloService = serviceClient.implement[AlohaService]

  lazy val consumerService = serviceClient.implement[ConsumerService]


  consumerService
    .initialTopic()
    .subscribe // <-- you get back a Subscriber instance
    .atLeastOnce(
      Flow.fromFunction { x =>
            println("be polite and say hello")
            println(s"${x.message}")
            println(s"I'm the worst consumer $x")
            val result: Future[Done] =
              helloService.useGreeting(x.message).invoke(GreetingMessage(x.message))
           result.map { response =>
              println(s"Hello service said: $response")
            }
            Done
      }
    )

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(AlohaState.typeKey)(
      entityContext => AlohaBehavior.create(entityContext)
    )
  )

}

package io.vaan.notz

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.management.scaladsl.AkkaManagement
import io.vaan.notz.users.{InMemoryUserRepo, UserActor, UserHandler, UserRegistry, UserRepository, UserRoutes}

import scala.util.{Failure, Success}

object App {
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val userRepo: UserRepository = InMemoryUserRepo

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      UserActor.initSharding(context.system)

      val userHandler: UserHandler = new UserHandler(context.system)

      val userRegistryActor = context.spawn(UserRegistry(userRepo), "UserRegistryActor")
      context.watch(userRegistryActor)

      val routes = new UserRoutes(userRegistryActor, userHandler)(context.system)
      startHttpServer(routes.userRoutes, context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "notz")
    AkkaManagement(system).start()
  }
}


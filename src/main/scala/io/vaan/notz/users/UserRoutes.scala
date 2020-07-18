package io.vaan.notz.users

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.vaan.notz.users.utils.JsonFormats._

import scala.concurrent.Future

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command], userHandler: UserHandler)(implicit val system: ActorSystem[_]) {

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers: Future[Users] =
    userHandler.getAll

  def getUser(email: String): Future[UserActor.GetUserResponse] =
    userHandler.get(email)

  def createUser(user: User): Future[User] =
    userHandler.createOrUpdate(user.email, user)

  def deleteUser(email: String): Future[UserActor.DeleteResponse] =
    userHandler.delete(email)

  private val users = pathEnd {
    concat(
      get {
        complete(getUsers)
      },
      post {
        entity(as[User]) { user =>
          onSuccess(createUser(user)) { performed =>
            complete((StatusCodes.Created, performed))
          }
        }
      })
  }

  private val `users/{id}` = path(Segment) { email =>
    concat(
      get {
        rejectEmptyResponse {
          onSuccess(getUser(email)) { response =>
            complete(response.maybeUser)
          }
        }

      },
      delete {
        onSuccess(deleteUser(email)) { _ =>
          complete(StatusCodes.OK)
        }
      })
  }

  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        users,
        `users/{id}`
      )
    }
}

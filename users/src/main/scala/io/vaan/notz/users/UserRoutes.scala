package io.vaan.notz.users

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.vaan.notz.users.UserActor.{DeleteResponse, GetUserResponse}
import io.vaan.notz.users.UserRegistry.{CreateUser, DeleteUser, GetUser, GetUsers}
import io.vaan.notz.users.utils.JsonFormats._

import scala.concurrent.Future

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers: Future[Users] =
    userRegistry.ask(GetUsers).flatten

  def getUser(email: String): Future[GetUserResponse] = {
    userRegistry.ask(GetUser(email, _)).flatten
  }

  def createUser(user: User): Future[User] =
    userRegistry.ask(CreateUser(user, _)).flatten

  def deleteUser(email: String): Future[DeleteResponse] =
    userRegistry.ask(DeleteUser(email, _)).flatten

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

  val usersRoutes: Route =
    pathPrefix("users") {
      concat(
        users,
        `users/{id}`
      )
    }
}

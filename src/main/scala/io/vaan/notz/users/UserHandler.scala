package io.vaan.notz.users

import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import io.vaan.notz.users.UserActor.{GetUserResponse, Response}
import io.vaan.notz.users.model.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class UserHandler(context: ActorContext[_]) {
  val sharding: ClusterSharding    = ClusterSharding(context.system)
  implicit val askTimeout: Timeout = Timeout(5.seconds)

  // TODO separate to create and update
  def createOrUpdate(userEmail: String, user: User): Future[User] =
    sharding
      .entityRefFor(UserActor.typeKey, userEmail)
      .ask[Response](UserActor.Update(user, _))
      .map { result =>
        context.system.log.info(s"Updated user: ${result.user}")
        result.user
      }

  def read(userEmail: String): Future[Option[User]] =
    sharding
      .entityRefFor(UserActor.typeKey, userEmail)
      .ask[GetUserResponse](UserActor.GetUser(userEmail, _))
      .map { response =>
        response.maybeUser match {
          case Some(user) =>
            context.system.log.info(s"Found user by email $userEmail: $user")
          case None =>
            context.system.log.info(s"Not found user by email $userEmail")
        }
        response.maybeUser
      }
}

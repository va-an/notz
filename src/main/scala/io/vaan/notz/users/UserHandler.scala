package io.vaan.notz.users

import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import io.vaan.notz.users.UserActor.{Response, Updated}
import io.vaan.notz.users.model.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class UserHandler(context: ActorContext[_]) {
  val sharding: ClusterSharding = ClusterSharding(context.system)
  implicit val askTimeout: Timeout = Timeout(5.seconds)

  def update(userEmail: String, user: User): Future[Response] =
    sharding.entityRefFor(UserActor.typeKey, userEmail)
    .ask[Response](UserActor.Update(user, _))
    .map {
      result => context.system.log.debug(s"Uptated user: ${result.user}")
      result
    }
}

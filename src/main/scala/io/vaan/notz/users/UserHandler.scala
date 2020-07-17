package io.vaan.notz.users

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import io.vaan.notz.users.UserActor.{DeleteResponse, GetUserResponse, UpdateResponse}
import io.vaan.notz.users.model.{User, Users}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class UserHandler(system: ActorSystem[Nothing]) {
  private implicit val askTimeout: Timeout = Timeout(5.seconds)
  private val sharding: ClusterSharding    = ClusterSharding(system)

  private implicit val materializer: Materializer = Materializer(system)
  private val readJournal: CassandraReadJournal = PersistenceQuery(system)
    .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  private def ids: Source[String, NotUsed] = readJournal.currentPersistenceIds

  // TODO separate to create and update
  // TODO make an actor?
  def createOrUpdate(userEmail: String, user: User): Future[User] =
    sharding
      .entityRefFor(UserActor.typeKey, userEmail)
      .ask[UpdateResponse](UserActor.Update(user, _))
      .map { result =>
        system.log.info(s"Updated user: ${result.user}")
        result.user
      }

  def get(userEmail: String): Future[GetUserResponse] =
    sharding
      .entityRefFor(UserActor.typeKey, userEmail)
      .ask[GetUserResponse](UserActor.Get)
      .map { response =>
        response.maybeUser match {
          case Some(user) =>
            system.log.info(s"Found user by email $userEmail: $user")
          case None =>
            system.log.info(s"Not found user by email $userEmail")
        }
        GetUserResponse(response.maybeUser)
      }

  def delete(userEmail: String): Future[DeleteResponse] =
    sharding
      .entityRefFor(UserActor.typeKey, userEmail)
      .ask[DeleteResponse](UserActor.Delete)
      .map { response =>
        system.log.info(s"Deleted user by email $userEmail")
        response
      }

  /** Not recommended for use in production */
  def getAll: Future[Users] =
    ids
    //      .filter(_.startsWith("UserEntity")) // FIXME now returns all entities, not only users
      .mapAsync(4)(get)
      .runWith(Sink.seq)
      .map(_.flatMap(_.maybeUser))
      .map(xs => Users(xs))
}

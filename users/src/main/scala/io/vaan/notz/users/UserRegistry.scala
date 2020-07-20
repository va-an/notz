package io.vaan.notz.users

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import io.vaan.notz.users.UserActor.{DeleteResponse, GetUserResponse, UpdateResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object UserRegistry {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Future[Users]])              extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[Future[User]]) extends Command
  final case class GetUser(email: String, replyTo: ActorRef[Future[GetUserResponse]])
      extends Command
  final case class DeleteUser(email: String, replyTo: ActorRef[Future[DeleteResponse]])
      extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val askTimeout: Timeout       = Timeout(5.seconds)
      implicit val sharding: ClusterSharding = ClusterSharding(context.system)

      implicit val materializer: Materializer = Materializer(context.system)
      val readJournal: CassandraReadJournal = PersistenceQuery(context.system)
        .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

      def ids: Source[String, NotUsed] = readJournal.currentPersistenceIds

      def createOrUpdate(userEmail: String, user: User)(implicit
          sharding: ClusterSharding
      ): Future[User] =
        sharding
          .entityRefFor(UserActor.typeKey, userEmail)
          .ask[UpdateResponse](UserActor.Update(user, _))
          .map { result =>
            context.system.log.info(s"Updated user: ${result.user}")
            result.user
          }

      def get(userEmail: String): Future[GetUserResponse] =
        sharding
          .entityRefFor(UserActor.typeKey, userEmail)
          .ask[UserActor.GetUserResponse](UserActor.Get)
          .map { response =>
            response.maybeUser match {
              case Some(user) =>
                context.system.log.info(s"Found user by email $userEmail: $user")
              case None =>
                context.system.log.info(s"Not found user by email $userEmail")
            }
            GetUserResponse(response.maybeUser)
          }

      /** Not recommended for use in production */
      def getAll: Future[Users] =
        ids
        //      .filter(_.startsWith("UserEntity")) // FIXME now returns all entities, not only users
          .mapAsync(4)(get)
          .runWith(Sink.seq)
          .map(_.flatMap(_.maybeUser))
          .map(xs => Users(xs))

      def delete(userEmail: String): Future[DeleteResponse] =
        sharding
          .entityRefFor(UserActor.typeKey, userEmail)
          .ask[DeleteResponse](UserActor.Delete)
          .map { response =>
            context.system.log.info(s"Deleted user by email $userEmail")
            response
          }

      Behaviors.receiveMessage {
        case GetUsers(replyTo) =>
          replyTo ! getAll
          Behaviors.same
        case CreateUser(user, replyTo) =>
          replyTo ! createOrUpdate(user.email, user)
          Behaviors.same
        case GetUser(email, replyTo) =>
          replyTo ! get(email)
          Behaviors.same
        case DeleteUser(email, replyTo) =>
          replyTo ! delete(email)
          Behaviors.same
      }
    }
}

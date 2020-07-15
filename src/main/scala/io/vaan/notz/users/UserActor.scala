package io.vaan.notz.users

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.external.ExternalShardAllocationStrategy
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import io.vaan.notz.users.model.User
import io.vaan.notz.users.utils.JsonSerializable
import org.slf4j.{ Logger, LoggerFactory }

object UserActor {
  val log: Logger = LoggerFactory.getLogger(getClass)

  // commands and events
  sealed trait Command  extends JsonSerializable
  sealed trait Event    extends JsonSerializable
  sealed trait Response extends JsonSerializable

  final case class Update(user: User, replyTo: ActorRef[UpdateResponse]) extends Command
  final case class Updated(user: User)                                   extends Event
  final case class UpdateResponse(user: User)

  final case class Get(replyTo: ActorRef[GetUserResponse])  extends Command
  final case class Got(user: User)                          extends Event
  final case class GetUserResponse(maybeUser: Option[User]) extends Response

  final case class Delete(replyTo: ActorRef[DeleteResponse]) extends Command
  final case class Deleted()                                 extends Event
  final case class DeleteResponse()                          extends Response

  // state
  final case class UserState(
      email: String,
      firstName: String,
      lastName: String,
      isDeleted: Boolean
  )

  // empty state constructor
  object UserState {
    def apply(user: User): UserState =
      UserState(
        user.firstName,
        user.lastName,
        user.email,
        isDeleted = false
      )

    val empty: UserState = UserState("", "", "", isDeleted = false)

    def isEmptyOrDeleted(state: UserState): Boolean =
      state == empty || state.isDeleted
  }

  // tags
  object Tag {
    val CREATED = "user-created"
    val DELETED = "user-deleted"
  }

  val commandHandler: (UserState, Command) => Effect[Event, UserState] = { (state, command) =>
    command match {
      case Update(user, replyTo) =>
        log.info(s"Command received: Update($user)")
        Effect
          .persist(Updated(user))
          .thenReply(replyTo) { state =>
            UpdateResponse(
              User(email = state.email, firstName = state.firstName, lastName = state.lastName)
            )
          }
      case Get(replyTo) =>
        log.info(s"Command received: GetUser(${state.email})")
        Effect
          .none
          .thenReply(replyTo) { state =>
            if (UserState.isEmptyOrDeleted(state))
              GetUserResponse(None)
            else
              GetUserResponse(Some(User(state)))
          }

      case Delete(replyTo) =>
        log.info(s"Command received: Delete(${state.email})")
        Effect
          .persist(Deleted())
          .thenReply(replyTo)(_ => DeleteResponse())

      case _ => throw new Exception("Unknown Command for User Actor")
    }
  }

  val eventHandler: (UserState, Event) => UserState = { (state, event) =>
    event match {
      case Updated(user) =>
        state.copy(
          email = user.email,
          firstName = user.firstName,
          lastName = user.lastName,
          isDeleted = false
        )
      case Got(_)    => state
      case Deleted() => state.copy(isDeleted = true)
    }
  }

  def apply(email: String): Behavior[Command] =
    Behaviors.setup { context =>
      log.info(s"Starting User Actor $email")
      EventSourcedBehavior[Command, Event, UserState](
        persistenceId = PersistenceId.ofUniqueId(email),
        emptyState = UserState.empty,
        commandHandler = commandHandler,
        eventHandler = eventHandler
      ).withTagger(event => tagEvent(email, event))
    }

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("User")

  def tagEvent(entityId: String, event: Event): Set[String] = {
    val entityGroup = s"group-${math.abs(entityId.hashCode % 10)}" // FIXME magic number

    event match {
      case _: Updated => Set(entityGroup, Tag.CREATED)
      case _: Deleted => Set(entityGroup, Tag.DELETED)
    }
  }

  def initSharding(system: ActorSystem[_]): Unit = {
    log.info(s"Initializing Sharding . . .")

    ClusterSharding(system).init(
      Entity(typeKey)(createBehavior = entityContext => UserActor(entityContext.entityId))
        .withAllocationStrategy(new ExternalShardAllocationStrategy(system, typeKey.name))
    )
  }
}

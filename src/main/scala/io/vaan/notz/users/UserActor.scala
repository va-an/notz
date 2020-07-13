package io.vaan.notz.users

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.external.ExternalShardAllocationStrategy
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.vaan.notz.users.model.User
import io.vaan.notz.users.utils.JsonSerializable

object UserActor {
  // commands and events
  sealed trait Command extends JsonSerializable
  sealed trait Event extends JsonSerializable

  final case class Update(user: User, replyTo: ActorRef[Response]) extends Command
  final case class Updated(user: User) extends Event

  final case class Response(user: User) extends JsonSerializable

  // state
  final case class UserState(
    firstName: String,
    lastName: String,
    email: String
  )

  // empty state constructor
  object UserState {
    def apply(user: User): UserState = UserState(
      user.firstName,
      user.lastName,
      user.email
    )

    def apply(email: String): UserState = UserState("", "", email)
  }

  val commandHandler: (UserState, Command) => Effect[Event, UserState] = { (state, command) =>
    command match {
      case Update(user, replyTo) =>
        Effect
          .persist(Updated(user))
          .thenReply(replyTo) { state =>
            Response(
              User(
                email = state.email,
                firstName = state.firstName,
                lastName = state.lastName)
            )
          }
      case _ => throw new Exception("Unknown Command for User Actor")
    }
  }

  val eventHandler: (UserState, Event) => UserState = { (state, event) =>
    event match {
      case Updated(user) =>
        state.copy(
          firstName = user.firstName,
          lastName = user.lastName
        )
    }
  }

  def apply(email: String): Behavior[Command] = Behaviors.setup { context => {
    context.system.log.debug(s"Starting User Actor $email")
    EventSourcedBehavior[Command, Event, UserState](
      persistenceId = PersistenceId.ofUniqueId(email),
      emptyState = UserState(email),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )}
  }

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("User")

  def initSharding(system: ActorSystem[_]): Unit = {
    system.log.debug(s"Initializing Sharding . . .")

    ClusterSharding(system).init(Entity(typeKey)(createBehavior = entityContext =>
      UserActor(entityContext.entityId))
      .withAllocationStrategy(new ExternalShardAllocationStrategy(system, typeKey.name)))
  }
}

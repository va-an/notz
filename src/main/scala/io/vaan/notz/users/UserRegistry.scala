package io.vaan.notz.users

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import io.vaan.notz.users.model.{User, Users}

object UserRegistry {
  // actor protocol
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users])                           extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed])   extends Command
  final case class GetUser(name: String, replyTo: ActorRef[GetUserResponse])    extends Command
  final case class DeleteUser(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[User]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! Users(users.toSeq)
        Behaviors.same
      case CreateUser(user, replyTo) =>
        replyTo ! ActionPerformed(s"User ${user.firstName} created.")
        registry(users + user)
      case GetUser(email, replyTo) =>
        replyTo ! GetUserResponse(users.find(_.email == email))
        Behaviors.same
      case DeleteUser(email, replyTo) =>
        replyTo ! ActionPerformed(s"User $email deleted.")
        registry(users.filterNot(_.email == email))
    }
}

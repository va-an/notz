package io.vaan.notz.users

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object UserRegistry {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users])                           extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed])   extends Command
  final case class GetUser(email: String, replyTo: ActorRef[GetUserResponse])    extends Command
  final case class DeleteUser(email: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  def apply(userRepository: UserRepository): Behavior[Command] = Behaviors.receiveMessage {
    case GetUsers(replyTo) =>
      replyTo ! userRepository.findAll
      Behaviors.same
    case CreateUser(user, replyTo) =>
      replyTo ! ActionPerformed(s"User ${user.firstName} created.")
      userRepository.create(user)
      Behaviors.same
    case GetUser(email, replyTo) =>
      replyTo ! GetUserResponse(userRepository.findByEmail(email))
      Behaviors.same
    case DeleteUser(email, replyTo) =>
      replyTo ! ActionPerformed(s"User $email deleted.")
      userRepository.deleteByEmail(email)
      Behaviors.same
  }
}

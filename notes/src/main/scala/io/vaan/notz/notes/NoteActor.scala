package io.vaan.notz.notes

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.vaan.notz.notes.utils.JsonSerializable
import org.slf4j.{Logger, LoggerFactory}

object NoteActor {
  val log: Logger = LoggerFactory.getLogger(getClass)

  sealed trait Command  extends JsonSerializable
  sealed trait Event    extends JsonSerializable

  final case class Create(note: Note, replyTo: ActorRef[CreateResponse]) extends Command
  final case class Created(note: Note) extends Event
  final case class CreateResponse(note: Note)

  final case class NoteState(note: Note)
  object NoteState {
    val empty: NoteState = NoteState(
      Note(
        userEmail = "",
        title = "",
        text = "",
        tags = Set.empty
      )
    )
  }

  def apply(note: Note): Behavior[Command] =
  Behaviors.setup { context =>
    context.system.log.info(s"Starting Note Actor")
    EventSourcedBehavior[Command, Event, NoteState](
      persistenceId = PersistenceId.ofUniqueId(note.title), // FIXME bad idea
      emptyState = NoteState.empty,
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
  }

  def commandHandler: (NoteState, Command) => Effect[Event, NoteState] = { (state, command) =>
    command match {
      case Create(note, replyTo) =>
        log.info(s"Command received: Create($note)")
        Effect
          .persist(Created(note))
          .thenReply(replyTo)(state => CreateResponse(Note(state)))
      case _ => throw new Exception("Unknown Command for Note Actor")
    }
  }

  def eventHandler: (NoteState, Event) => NoteState = { (state, event) =>
    event match {
      case Created(note) => state.copy(note)
    }
  }
}

package io.vaan.notz.notes

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import io.vaan.notz.notes.utils.JsonSerializable

object NoteActor {
  sealed trait Command  extends JsonSerializable
  sealed trait Event    extends JsonSerializable

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
      commandHandler = ???,
      eventHandler = ???
    )
  }
}

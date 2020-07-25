package io.vaan.notz.notes

import io.vaan.notz.notes.NoteActor.NoteState

final case class Note(
    userEmail: String,
    title: String,
    text: String,
    tags: Set[String]
)

object Note {
  def apply(noteState: NoteState): Note =
    Note(
      userEmail = noteState.note.userEmail,
      title = noteState.note.title,
      text = noteState.note.text,
      tags = noteState.note.tags
    )
}

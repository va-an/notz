package io.vaan.notz.notes

final case class Note(
    userEmail: String,
    title: String,
    text: String,
    tags: Set[String]
)

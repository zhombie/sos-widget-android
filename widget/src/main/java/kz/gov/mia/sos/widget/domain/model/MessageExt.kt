package kz.gov.mia.sos.widget.domain.model

import kz.garage.chat.model.Message
import kz.garage.multimedia.store.model.*

fun Message.isNullableTextMessageWithImages(): Boolean = isNullableTextMessageWithContents<Image>()

fun Message.isNullableTextMessageWithVideos(): Boolean = isNullableTextMessageWithContents<Video>()

fun Message.isNullableTextMessageWithAudio(): Boolean = isNullableTextMessageWithContents<Audio>()

fun Message.isNullableTextMessageWithDocuments(): Boolean = isNullableTextMessageWithContents<Document>()

inline fun <reified T> Message.isNullableTextMessageWithContents(): Boolean =
    isNullableTextMessageWithContents<T> { it.isAnyFileExists() }

inline fun <reified T> Message.isNullableTextMessageWithContents(
    predicate: (content: Content) -> Boolean
): Boolean =
    (!contents.isNullOrEmpty() && contents!!.all { it is T && predicate.invoke(it) })
            && location == null
            && replyMarkup == null
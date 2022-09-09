package org.mozilla.fenix.wallpapers

import java.util.*

fun List<Wallpaper>.groupByCollection(): Map<Wallpaper.Collection, List<Wallpaper>> = groupBy {
    it.collection
}.filter {
    it.key.name != "default"
}.map {
    if (it.key.name == "classic-firefox") {
        it.key to listOf(Wallpaper.Default) + it.value
    } else {
        it.key to it.value
    }
}.toMap()

fun Wallpaper.Collection.getDisplayName(): String = this.name
    .split('-')
    .joinToString(" ") { word ->
        word.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

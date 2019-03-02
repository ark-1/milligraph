package me.arkadybazhanov.milligraph

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Column

object Posts : IdTable<Long>() {
    override val id: Column<EntityID<Long>> = long(this::id.name).primaryKey().entityId()

    val textContent: Column<String> = text(this::textContent.name)
}

class PostEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PostEntity>(Posts)

    var textContent by Posts.textContent

    fun toData() = Post(id.value, textContent)
}


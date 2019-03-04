package me.arkadybazhanov.milligraph

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Column

object Posts : IdTable<Long>() {
    override val id: Column<EntityID<Long>> = long(this::id.name).primaryKey().entityId()

    val timestamp: Column<Int> = integer(this::timestamp.name)
    val author: Column<String> = varchar(this::author.name, length = 100)
    val isPublic: Column<Boolean> = bool(this::isPublic.name)
    val chatId: Column<Long> = long(this::chatId.name)
}

class PostEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PostEntity>(Posts)

    var timestamp by Posts.timestamp
    var author by Posts.author
    var isPublic by Posts.isPublic
    var chatId by Posts.chatId

    fun toData() = Post(id.value, timestamp, author, isPublic, chatId)
}


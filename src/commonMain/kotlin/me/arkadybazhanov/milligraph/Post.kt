package me.arkadybazhanov.milligraph

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long,
    val timestamp: Int,
    val author: String,
    val isPublic: Boolean,
    val chatId: Long
)

const val jsResponseElementId = "js-response"
const val renderFunctionName = "render"
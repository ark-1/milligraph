package me.arkadybazhanov.milligraph

import kotlinx.serialization.Serializable

@Serializable
data class Post(val id: Long, val textContent: String)

const val jsResponseElementId = "js-response"
const val renderFunctionName = "render"
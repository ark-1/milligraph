package me.arkadybazhanov.milligraph

import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.w3c.dom.HTMLElement
import kotlin.browser.document

fun parsePostList(serialized: String): List<Post> = Json.parse(Post.serializer().list, serialized)

@Suppress("unused")
@JsName(renderFunctionName)
fun render(serializedPostList: String) {
    val posts = parsePostList(serializedPostList)

    (document.getElementById(jsResponseElementId) as HTMLElement).append {
        table {
            tbody {
                for (post in posts) {
                    tr {
                        td { +post.textContent }
                    }
                }
            }
        }
    }
}
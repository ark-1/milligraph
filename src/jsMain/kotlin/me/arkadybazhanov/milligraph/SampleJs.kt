package me.arkadybazhanov.milligraph

import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.w3c.dom.HTMLElement
import kotlin.browser.document

fun parsePostList(serialized: String): List<Post> = Json.parse(Post.serializer().list, serialized)

inline fun <T, C : TagConsumer<T>, I> C.makeTable(items: Iterable<I>, crossinline row: TD.(I) -> Unit) {
    table {
        tbody {
            for (item in items) tr {
                td { row(item) }
            }
        }
    }
}

@Suppress("unused")
@JsName(renderFunctionName)
fun render(serializedPostList: String) {
    val posts = parsePostList(serializedPostList)

    (document.getElementById(jsResponseElementId) as HTMLElement).append {
        makeTable(posts) {
            +it.textContent
        }
    }
}

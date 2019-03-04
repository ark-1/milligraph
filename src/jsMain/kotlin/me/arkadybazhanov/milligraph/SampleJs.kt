package me.arkadybazhanov.milligraph

import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.w3c.dom.HTMLElement
import kotlin.browser.document

fun parsePostList(serialized: String): List<Post> = Json.parse(Post.serializer().list, serialized)

fun <T, C : TagConsumer<T>> C.makeTable(posts: Iterable<Post>) {
    div(classes = "container") {
        for (item in posts) {
            val cssClass = if (item.isPublic) "public" else "private"
            val channel = if (item.isPublic) "balthazar_trip" else "faces_on_tv_trip"

            div(classes = "item $cssClass") {
                script(src = "https://telegram.org/js/telegram-widget.js?5") {
                    attributes["async"] = ""
                    attributes["data-telegram-post"] = channel + "/" + item.id
                }
            }
        }
    }
}

@Suppress("unused")
@JsName(renderFunctionName)
fun render(serializedPostList: String) {
    val posts = parsePostList(serializedPostList)

    (document.getElementById(jsResponseElementId) as HTMLElement).append {
        makeTable(posts)
    }
}

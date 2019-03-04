package me.arkadybazhanov.milligraph

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import me.ivmg.telegram.*
import me.ivmg.telegram.dispatcher.*
import me.ivmg.telegram.dispatcher.handlers.Handler
import me.ivmg.telegram.entities.*
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.File
import java.sql.Connection

private fun initDB(dbFilePath: String) {
    Database.connect("jdbc:sqlite:$dbFilePath", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        SchemaUtils.create(Posts)
    }
}

private fun Dispatcher.eachPost(toChatId: Long, callback: (Message) -> Unit) {
    addHandler(object : Handler({ _, update -> callback(update.channelPost!!) }) {
        override val groupIdentifier get() = "Messages"
        override fun checkUpdate(update: Update): Boolean =
            update.channelPost != null && update.channelPost!!.chat.id.also(::println) == toChatId
    })
}

private fun Dispatcher.saveEachMessage(channelId: Long, isPublic: Boolean) {
    eachPost(channelId) {
        transaction {
            PostEntity.new(it.messageId) {
                timestamp = it.date
                author = it.from?.firstName.toString()
                this.isPublic = isPublic
                chatId = channelId
            }
        }
    }
}

private fun Dispatcher.verify() {
    command("verify") { bot, update ->
        val posts = transaction {
            PostEntity.all().map { it.toData() }
        }

        val toRemove = mutableListOf<Long>()

        for (post in posts) {
            val remove = bot.forwardMessage(
                chatId = update.message!!.chat.id,
                fromChatId = post.chatId,
                messageId = post.id,
                disableNotification = true
            ).first?.body()?.result == null

            if (remove) toRemove += post.id
        }

        transaction {
            Posts.deleteWhere { Posts.id inList toRemove }
        }
    }
}

private fun startBot() = bot {
    token = System.getenv("telegram_api_token") ?: error("No telegram api token")
    val publicChannelId = System.getenv("public_channel_id")?.toLong() ?: error("No public channel id")
    val privateChannelId = System.getenv("private_channel_id")?.toLong() ?: error("No private channel id")

    dispatch {
        verify()

        command("send") { bot, update, args ->
            bot.sendMessage(privateChannelId, args.joinToString(", "))
            val msg = update.message!!
            bot.deleteMessage(msg.chat.id, msg.messageId)
        }

        saveEachMessage(publicChannelId, isPublic = true)
        saveEachMessage(privateChannelId, isPublic = false)
    }
}.startPolling()

fun startServer() = embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
    val currentDir = File(".").absoluteFile
    environment.log.info("Current directory: $currentDir")

    val webDir = listOf(
        "web",
        "../src/jsMain/web",
        "src/jsMain/web"
    ).map {
        File(currentDir, it)
    }.firstOrNull { it.isDirectory }?.absoluteFile ?: error("Can't find 'web' folder for this sample")

    environment.log.info("Web directory: $webDir")

    routing {
        get("/") {
            val posts = transaction {
                PostEntity.all().map(PostEntity::toData).sortedBy { it.timestamp }
            }
            val postsS = StringEscapeUtils.escapeEcmaScript(Json.stringify(Post.serializer().list, posts))!!

            call.respondHtml {
                head {
                    title("Milligraph")
                }

                body {
                    div {
                        id = jsResponseElementId
                    }
                    script(src = "/static/require.min.js") {}
                    script {
                        unsafe {
                            @Suppress("JSUnresolvedFunction", "JSUnresolvedVariable", "JSFileReferences")
                            //language=JavaScript
                            raw("""
                                require.config({baseUrl: '/static'});
                                require(['/static/milligraph.js'], function(js) {
                                    js.me.arkadybazhanov.milligraph.$renderFunctionName("$postsS");
                                });
                            """.trimIndent())
                        }
                    }
                }
            }
        }
        static("/static") {
            files(webDir)
        }
    }
    Unit
}.start(wait = true)

fun main() {
    initDB("milligraph.db")
    startBot()
    startServer()
}

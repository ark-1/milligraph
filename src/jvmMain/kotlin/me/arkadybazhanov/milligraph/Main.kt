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
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.File
import java.sql.Connection

fun initDB(dbFilePath: String) {
    Database.connect("jdbc:sqlite:$dbFilePath", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        SchemaUtils.create(Posts)
    }
}

fun startBot() = bot {
    token = System.getenv("telegram_api_token") ?: error("No telegram api token")

    dispatch {
        text { bot, update ->
            val message = update.message ?: error("WTF no message")
            val text = message.text ?: error("WTF no text")

            transaction {
                PostEntity.new(message.messageId) {
                    textContent = text
                }
            }

            bot.sendMessage(message.chat.id, "hah gotcha $text")
        }
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
                PostEntity.all().map(PostEntity::toData)
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
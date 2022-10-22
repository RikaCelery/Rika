package org.celery.command.common.nihongo

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.sendTo
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.onLocked
import org.celery.command.controller.abs.throwOnFailure
import org.celery.command.controller.abs.withlock
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.toImage
import org.celery.utils.withRetry
import org.ktorm.database.Database
import org.ktorm.database.asIterable
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.text
import org.ktorm.support.sqlite.SQLiteDialect
import org.openqa.selenium.Dimension
import java.sql.ResultSet

object Nihongo : Command("日语学习") {

    @Command("^日语语法\\s?([0-9A-Za-zぁ-んァ-ヶ]{1,6})\$")
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (NihongoDataBase.database == null) NihongoDataBase.init()
        if (NihongoDataBase.database == null) {
            sendMessage("数据库未连接")
            return ExecutionResult.Ignored("数据库未连接")
        }
        withlock(subject.id, 0) {
            val g = NihongoDataBase.getRandomGrammarByTag(eventMatchResult[1])
            SharedSelenium.render(g.string(), Dimension(2000, 0)).toImage(subject).sendTo(subject)
        }.onLocked {
            sendMessage("急你妈")
            return ExecutionResult.Ignored
        }.throwOnFailure()

        return ExecutionResult.Success

    }

    private object NihongoDataBase {
        var database: Database? = null

        interface Grammar : Entity<Grammar> {
            companion object : Entity.Factory<Grammar>() {
                fun createFrom(it: ResultSet): Grammar {
                    return Grammar {
                        id = it.getInt("id")
                        tag = it.getString("tag")
                        name = it.getString("name")
                        pronunciation = it.getString("pronunciation")
                        usage = it.getString("usage")
                        meaning = it.getString("meaning")
                        explanation = it.getString("explanation")
                        example = it.getString("example")
                        grammarUrl = it.getString("grammar_url")
                    }
                }
            }

            var id: Int
            var tag: String
            var name: String
            var pronunciation: String
            var usage: String
            var meaning: String
            var explanation: String
            var example: String
            var grammarUrl: String
            fun string(): String {
                return "ID:\n%d\n\n标签:\n%s\n\n语法名:\n%s\n\n发音:\n%s\n\n用法:\n%s\n\n意思:\n%s\n\n解说:\n%s\n\n示例:\n%s".format(id,
                    tag,
                    name,
                    pronunciation,
                    usage,
                    meaning,
                    explanation,
                    example)
            }
        }

        object Grammars : Table<Grammar>("grammar") {
            var id = int("id").bindTo { it.id }
            var tag = text("tag").bindTo { it.tag }
            var name = text("name").bindTo { it.name }
            var pronunciation = text("pronunciation").bindTo { it.pronunciation }
            var usage = text("usage").bindTo { it.usage }
            var meaning = text("meaning").bindTo { it.meaning }
            var explanation = text("explanation").bindTo { it.explanation }
            var example = text("example").bindTo { it.example }
            var grammarUrl = text("grammar_url").bindTo { it.grammarUrl }
        }

        fun getRandomGrammarByTag(tag: String): Grammar {
            return withRetry(3) {
                database!!.useConnection {
                    it.prepareStatement("select * from grammar  where tag LIKE ? ORDER BY RANDOM() limit 1").apply {
                        setString(1, "%${tag}%")
                    }.executeQuery().asIterable().map {
                        Grammar.createFrom(it)
                    }.single()
                }
            }
        }

        fun init() {
            //https://raw.githubusercontent.com/FloatTech/zbpdata/7bc1828db55f8549e04ccb85cd6f90f2964e2062/Nihongo/nihongo.db
            try {
                val dataFile = getDataFile("nihongo.db")
                if (dataFile.exists()) {
                    try {
                        database = withRetry(2) {
                            Database.connect(
                                "jdbc:sqlite:${dataFile.absolutePath}",
                            "org.sqlite.JDBC",
                                dialect = SQLiteDialect()
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e)
                        dataFile.delete()
                    }
                }
                if (database == null) {
                    withRetry {
                        logger.debug("下载数据库")
                        HttpUtils.downloadToFile("https://github.com/FloatTech/zbpdata/raw/7bc1828db55f8549e04ccb85cd6f90f2964e2062/Nihongo/nihongo.db",
                            file = dataFile)
                    }
                    database = withRetry(2) {
                        Database.connect("jdbc:sqlite:${dataFile.absolutePath}", dialect = SQLiteDialect())
                    }
                }
            } catch (e: Exception) {
                logger.error("无法初始化数据库")
            }
        }

        init {
            init()
        }
    }
}
package org.celery.command.controller

import events.ExecutionResult.*
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.message.data.findIsInstance
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.safeCast
import org.celery.command.controller.abs.AbstractCommand
import org.celery.data.Coins
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.CoroutineContext

/*
event -> findMatch( Command.matches(event)!=null ) -> ExecutionEvent -> execute

 */
/**
 * 指令执行器
 */
object CommandExecutor : SimpleListenerHost() {
    private val logger = MiraiLogger.Factory.create(this::class)
    private val commands2 = hashSetOf<AbstractCommand>()
    fun add(c: AbstractCommand) = commands2.add(c)

    val lastMessages = ConcurrentLinkedDeque<MessageEvent>()


    @EventHandler
    suspend fun Event.listen() {
        if (this is MessageEvent){
            lastMessages.add(this)
            if (lastMessages.size>20)
                lastMessages.poll()
        }
        for (command in commands2.sortedByDescending { it.priority }) {
            try {
                if (!command.triggered(this)) {
                    continue
                }
                logger.info("find match: ${command.commandId}")
                if (!command.isEnable()) {
                    logger.warning("${command.commandId} closed")
                    continue
                }
                if (!command.checkPermission(this)) {
                    logger.warning("${command.commandId} checkPermission failed")
                    continue
                }
                val (subjectId,userId) = getIds(-1L, -1L)
                if (command.canCall(subjectId, userId)) {
                    logger.debug("executing...")
                    val start = System.currentTimeMillis()
                    val (result, coin) = command.runCommand(this)
                    when (result) {
                        is Error -> {
                            this.safeCast<GroupEvent>()?.group?.sendMessage(
                                "在执行指令时出现了一个内部错误${result.cause?.javaClass?.simpleName}, ${
                                    result.cause?.message?.let {
                                        if (it.length > 100) it.substring(
                                            0..100
                                        ) else it
                                    }
                                }"
                            )
                                ?: this.safeCast<MessageEvent>()?.subject?.sendMessage(
                                    "在执行指令时出现了一个内部错误${result.cause?.javaClass?.simpleName}, ${
                                        result.cause?.message?.let {
                                            if (it.length > 100) it.substring(
                                                0..100
                                            ) else it
                                        }
                                    }"
                                )

                            logger.error(
                                "internal error happened when executing $this caused by ${result.cause}.${
                                    result.cause?.stackTraceToString()?.let { "\n$it" }
                                }"
                            )
                        }
                        is Failed -> {

                            this.safeCast<GroupEvent>()?.group?.sendMessage(
                                "在执行指令时出现了无法处理的错误${result.exception?.javaClass?.simpleName}, ${
                                    result.exception?.message ?: result.message?.let {
                                        if (it.length > 100) it.substring(
                                            0..100
                                        ) else it
                                    }
                                }"
                            )
                                ?: this.safeCast<MessageEvent>()?.subject?.sendMessage(
                                    "在执行指令时出现了无法处理的错误${result.exception?.javaClass?.simpleName}, ${
                                        result.exception?.message ?: result.message?.let {
                                            if (it.length > 100) it.substring(
                                                0..100
                                            ) else it
                                        }
                                    }"
                                )
                            logger.error(
                                "exception happened when executing $this caused by ${result.exception}.${
                                    result.exception?.stackTraceToString()?.let { "\n$it" }
                                }"
                            )
                        }
                        LimitCall -> {
                            logger.debug("limit user $userId in subjectId $subjectId ${command.getCooldown()}ms")
                            command.temporaryBan(subjectId, userId, command.getCooldown() * 1L+1)
                        }
                        is Ignored -> {
                            logger.debug("ignored, reason: ${result.reason}.")
                        }
                        is Ignored.NoReason -> {
                            logger.debug("ignored, no reason.")
                        }
                        Unknown -> {
                            logger.warning("unknown status, not add.")
                        }
                        Success -> {
                            logger.debug("executed sucessfully")
                            Coins[userId] -= coin
                            if (Coins[userId] < 0) Coins[userId] = 0
                            command.increaseCount(subjectId, userId)
                        }
                    }
                    logger.debug("time cost:${System.currentTimeMillis() - start}ms")
                } else
                    logger.warning("${command.commandId} can not use")
                if (command.blockSub){
                    break
                }
            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }

    private fun Event.getIds(
        defaultSubjectId: Long,
        defaultUserId: Long
    ): Pair<Long, Long> {
        var subjectId1 = defaultSubjectId
        var userId1 = defaultUserId
        if (this is MemberMuteEvent) {
            subjectId1 = group.id
            userId1 = member.id
        }
        if (this is MemberUnmuteEvent) {
            subjectId1 = group.id
            userId1 = member.id
        }
        if (this is BotMuteEvent) {
            subjectId1 = group.id
            userId1 = bot.id
        }
        if (this is BotUnmuteEvent) {
            subjectId1 = group.id
            userId1 = bot.id
        }
        if (this is MessageEvent) {
            subjectId1 = subject.id
            userId1 = sender.id
        }
        if (this is NudgeEvent) {
            subjectId1 = subject.id
            userId1 = from.id
        }
        if (this is GroupMemberEvent) {
            subjectId1 = group.id
            userId1 = member.id
        }
        return Pair(subjectId1, userId1)
    }


    override fun handleException(context: CoroutineContext, exception: Throwable) {
        logger.error(exception)
    }


    inline fun <reified M : SingleMessage?> lastInstanceOrNull(messageEvent: MessageEvent): M? =
        lastMessages.reversed()
            .find { it.sender == messageEvent.sender && it.subject == messageEvent.subject && it.message.findIsInstance<M>() != null }?.message?.findIsInstance<M>()


}
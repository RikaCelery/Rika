package command.common.ero

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.warning
import org.celery.command.common.ero.Setu
import org.celery.command.common.ero.SetuType

/**
 * 涩图库,包含很多'涩图[Setu]'
 */
interface SetuLib {
    val logger: MiraiLogger
        get() = MiraiLogger.Factory.create(this::class)

    /**
     * 别名，用于匹配用户输入
     */
    val alias: List<String>
    val type: SetuType

    /**
     * 用于内部确定一个图库
     */
    val libName: String

    /**
     * 随机获取一张涩图，不做任何判断
     */
    fun getRandomOrNull(): Setu?

    /**
     * 随机获取一张 聊天对象subject[Contact]下的用户sender[Contact] 可以查看的涩图
     */
    fun getSetuOrNull(sender: Contact, subject: Contact): Setu? {
        var setu: Setu? = getRandomOrNull()
        var trys = 0
        while (setu != null && setu.canSand(sender, subject).not()) {
            setu = getRandomOrNull()
            if (trys++ >= 25) {
                logger.warning { "在25次尝试中没有找到合适的涩图." }
                break
            }
        }
        return setu
    }


}
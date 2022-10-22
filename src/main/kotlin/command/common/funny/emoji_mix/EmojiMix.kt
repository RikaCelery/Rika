package org.celery.command.common.funny.emoji_mix

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.internal.toHexString
import org.celery.command.common.funny.emoji_mix.EmojiConsts.EMOJI_REGEX
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.http.HttpUtils
import java.io.File

@Suppress("unused")

private val LAST_UPDATE = "2022-03-11T23:19:31Z"

object EmojiMix : Command(
    commandId = "表情混合",
    priority = 4,
    usage = "<emoji1><emoji2>",
    description = "混合两个emoji表情",
    example = "😂🤣",
) {
    @Command
    suspend fun MessageEvent.nn(matchResult: EventMatchResult): ExecutionResult {
        val result1 = matchResult.getAllMatches().random()
        val result2 = matchResult.getAllMatches().apply { remove(result1) }.random()
        val emos: List<String> =
            matchResult.getAllMatches().joinToString("").codePoints().toArray().map { it.toHexString() }
        val e1 = result1.codePoints().toArray().first()
        val e2 = result2.codePoints().toArray().first()
//        val emo1 = e1.toHexString()
//        val emo2 = e2.toHexString()
        val fileNamePrefix = emos.sorted().joinToString("+")

        val file = getDataFile("emojiMix").listFiles { it: File ->
            it.name.startsWith(fileNamePrefix)
        }?.singleOrNull() ?: getDataFile("emojiMix/$fileNamePrefix.png")
        if (file.exists()) {
            file.toExternalResource().use {
                try {
                    subject.sendMessage(it.uploadAsImage(subject))
                } catch (e: Exception) {
                    logger.error(e)
                }
            }
        } else {
            try {
                getMix(e1, e2).let {
                    if (it.isEmpty()) return@let
                    it.toExternalResource().use {
                        subject.sendMessage(subject.uploadImage(it))
                    }
                    file.writeBytes(it)
                }
            } catch (e: Exception) {
                try {
                    getMix(e2, e1).let {
                        if (it.isEmpty()) return@let
                        it.toExternalResource().use {
                            subject.sendMessage(subject.uploadImage(it))
                        }
                        file.writeBytes(it)
                    }
                } catch (e: Exception) {
                    return ExecutionResult.Ignored("表情$result1+$result1,未找到匹配的混合图($fileNamePrefix),已忽略")
                }
            }
        }
        return ExecutionResult.Success

    }

    private fun getMix(a: Int, b: Int): ByteArray {
        val api1 = getApi(a) ?: throw Exception("no match")
        //"https://www.gstatic.com/android/keyboard/emojikitchen/$api1/u${a.toHexString()}/u${a.toHexString()}_u${b.toHexString()}.png"
        val api =
        "https://www.gstatic.com/android/keyboard/emojikitchen/%d/u%x/u%x_u%x.png".format(api1, a, a, b)

        return HttpUtils.downloader(api)
    }

    private fun getApi(codePoint: Int): Long? {
        val map = mutableMapOf<Int, Long>()
        map.put(128516, 20201001) // 😄 grinning face with smiling eyes
        map.put(128512, 20201001) // 😀 grinning face
        map.put(128578, 20201001) // 🙂 slightly smiling face
        map.put(128579, 20201001) // 🙃 upside-down face
        map.put(128521, 20201001) // 😉 winking face
        map.put(128522, 20201001) // 😊 smiling face with smiling eyes
        map.put(128518, 20201001) // 😆 grinning squinting face
        map.put(128515, 20201001) // 😃 grinning face with big eyes
        map.put(128513, 20201001) // 😁 beaming face with smiling eyes
        map.put(129315, 20201001) // 🤣 rolling on the floor laughing
        map.put(128517, 20201001) // 😅 grinning face with sweat
        map.put(128514, 20201001) // 😂 face with tears of joy
        map.put(128519, 20201001) // 😇 smiling face with halo
        map.put(129392, 20201001) // 🥰 smiling face with hearts
        map.put(128525, 20201001) // 😍 smiling face with heart-eyes
        map.put(128536, 20201001) // 😘 face blowing a kiss
        map.put(129321, 20201001) // 🤩 star-struck
        map.put(128535, 20201001) // 😗 kissing face
        map.put(128538, 20201001) // 😚 kissing face with closed eyes
        map.put(128537, 20201001) // 😙 kissing face with smiling eyes
        map.put(128539, 20201001) // 😛 face with tongue
        map.put(128541, 20201001) // 😝 squinting face with tongue
        map.put(128523, 20201001) // 😋 face savoring food
        map.put(129394, 20201001) // 🥲 smiling face with tear
        map.put(129297, 20201001) // 🤑 money-mouth face
        map.put(128540, 20201001) // 😜 winking face with tongue
        map.put(129303, 20201001) // 🤗 smiling face with open hands hugs
        map.put(129323, 20201001) // 🤫 shushing face quiet whisper
        map.put(129300, 20201001) // 🤔 thinking face question hmmm
        map.put(129325, 20201001) // 🤭 face with hand over mouth embarrassed
        map.put(129320, 20201001) // 🤨 face with raised eyebrow question
        map.put(129296, 20201001) // 🤐 zipper-mouth face
        map.put(128528, 20201001) // 😐 neutral face
        map.put(128529, 20201001) // 😑 expressionless face
        map.put(128566, 20201001) // 😶 face without mouth
        map.put(129322, 20201001) // 🤪 zany face
        map.put(128527, 20201001) // 😏 smirking face suspicious
        map.put(128530, 20201001) // 😒 unamused face
        map.put(128580, 20201001) // 🙄 face with rolling eyes
        map.put(128556, 20201001) // 😬 grimacing face
        map.put(128558, 20210218) // 😮 face exhaling
        map.put(129317, 20201001) // 🤥 lying face
        map.put(128524, 20201001) // 😌 relieved face
        map.put(128532, 20201001) // 😔 pensive face
        map.put(128554, 20201001) // 😪 sleepy face
        map.put(129316, 20201001) // 🤤 drooling face
        map.put(128564, 20201001) // 😴 sleeping face
        map.put(128567, 20201001) // 😷 face with medical mask
        map.put(129298, 20201001) // 🤒 face with thermometer
        map.put(129301, 20201001) // 🤕 face with head-bandage
        map.put(129314, 20201001) // 🤢 nauseated face
        map.put(129326, 20201001) // 🤮 face vomiting throw
        map.put(129319, 20201001) // 🤧 sneezing face
        map.put(129397, 20201001) // 🥵 hot face warm
        map.put(129398, 20201001) // 🥶 cold face freezing ice
        map.put(128565, 20201001) // 😵 face with crossed-out eyes
        map.put(129396, 20201001) // 🥴 woozy face drunk tipsy drug high
        map.put(129327, 20201001) // 🤯 exploding head mindblow
        map.put(129312, 20201001) // 🤠 cowboy hat face
        map.put(129395, 20201001) // 🥳 partying face
        map.put(129400, 20201001) // 🥸 disguised face
        map.put(129488, 20201001) // 🧐 face with monocle glasses
        map.put(128526, 20201001) // 😎 smiling face with sunglasses
        map.put(128533, 20201001) // 😕 confused face
        map.put(128543, 20201001) // 😟 worried face
        map.put(128577, 20201001) // 🙁 slightly frowning face
        map.put(128559, 20201001) // 😯 hushed face
        map.put(128562, 20201001) // 😲 astonished face
        map.put(129299, 20201001) // 🤓 nerd face glasses
        map.put(128563, 20201001) // 😳 flushed face
        map.put(129402, 20201001) // 🥺 pleading face
        map.put(128551, 20201001) // 😧 anguished face
        map.put(128552, 20201001) // 😨 fearful face
        map.put(128550, 20201001) // 😦 frowning face with open mouth
        map.put(128560, 20201001) // 😰 anxious face with sweat
        map.put(128549, 20201001) // 😥 sad but relieved face
        map.put(128557, 20201001) // 😭 loudly crying face
        map.put(128553, 20201001) // 😩 weary face
        map.put(128546, 20201001) // 😢 crying face
        map.put(128547, 20201001) // 😣 persevering face
        map.put(128544, 20201001) // 😠 angry face
        map.put(128531, 20201001) // 😓 downcast face with sweat
        map.put(128534, 20201001) // 😖 confounded face
        map.put(129324, 20201001) // 🤬 face with symbols on mouth
        map.put(128542, 20201001) // 😞 disappointed face
        map.put(128555, 20201001) // 😫 tired face
        map.put(128548, 20201001) // 😤 face with steam from nose
        map.put(129393, 20201001) // 🥱 yawning face
        map.put(128169, 20201001) // 💩 pile of poo
        map.put(128545, 20201001) // 😡 pouting face
        map.put(128561, 20201001) // 😱 face screaming in fear
        map.put(128127, 20201001) // 👿 angry face with horns
        map.put(128128, 20201001) // 💀 skull
        map.put(128125, 20201001) // 👽 alien
        map.put(128520, 20201001) // 😈 smiling face with horns devil
        map.put(129313, 20201001) // 🤡 clown face
        map.put(128123, 20201001) // 👻 ghost
        map.put(129302, 20201001) // 🤖 robot
        map.put(128175, 20201001) // 💯 hundred points percent
        map.put(128064, 20201001) // 👀 eyes
        map.put(127801, 20201001) // 🌹 rose flower
        map.put(127804, 20201001) // 🌼 blossom flower
        map.put(127799, 20201001) // 🌷 tulip flower
        map.put(127797, 20201001) // 🌵 cactus
        map.put(127821, 20201001) // 🍍 pineapple
        map.put(127874, 20201001) // 🎂 birthday cake
        map.put(127751, 20210831) // 🌇 sunset
        map.put(129473, 20201001) // 🧁 cupcake muffin
        map.put(127911, 20210521) // 🎧 headphone earphone
        map.put(127800, 20210218) // 🌸 cherry blossom flower
        map.put(129440, 20201001) // 🦠 microbe germ bacteria virus covid corona
        map.put(128144, 20201001) // 💐 bouquet flowers
        map.put(127789, 20201001) // 🌭 hot dog food
        map.put(128139, 20201001) // 💋 kiss mark lips
        map.put(127875, 20201001) // 🎃 jack-o-lantern pumpkin
        map.put(129472, 20201001) // 🧀 cheese wedge
        map.put(9749, 20201001) // ☕ hot beverage coffee cup tea
        map.put(127882, 20201001) // 🎊 confetti ball
        map.put(127880, 20201001) // 🎈 balloon
        map.put(9924, 20201001) // ⛄ snowman without snow
        map.put(128142, 20201001) // 💎 gem stone crystal diamond
        map.put(127794, 20201001) // 🌲 evergreen tree
        map.put(129410, 20210218) // 🦂 scorpion
        map.put(128584, 20201001) // 🙈 see-no-evil monkey
        map.put(128148, 20201001) // 💔 broken heart
        map.put(128140, 20201001) // 💌 love letter heart
        map.put(128152, 20201001) // 💘 heart with arrow
        map.put(128159, 20201001) // 💟 heart decoration
        map.put(128158, 20201001) // 💞 revolving hearts
        map.put(128147, 20201001) // 💓 beating heart
        map.put(128149, 20201001) // 💕 two hearts
        map.put(128151, 20201001) // 💗 growing heart
        map.put(129505, 20201001) // 🧡 orange heart
        map.put(128155, 20201001) // 💛 yellow heart
        map.put(10084, 20210218) // ❤ mending heart
        map.put(128156, 20201001) // 💜 purple heart
        map.put(128154, 20201001) // 💚 green heart
        map.put(128153, 20201001) // 💙 blue heart
        map.put(129294, 20201001) // 🤎 brown heart
        map.put(129293, 20201001) // 🤍 white heart
        map.put(128420, 20201001) // 🖤 black heart
        map.put(128150, 20201001) // 💖 sparkling heart
        map.put(128157, 20201001) // 💝 heart with ribbon
        map.put(127873, 20211115) // 🎁 wrapped-gift
        map.put(129717, 20211115) // 🪵 wood
        map.put(127942, 20211115) // 🏆 trophy
        map.put(127838, 20210831) // 🍞 bread
        map.put(128240, 20201001) // 📰 newspaper
        map.put(128302, 20201001) // 🔮 crystal ball
        map.put(128081, 20201001) // 👑 crown
        map.put(128055, 20201001) // 🐷 pig face
        map.put(129412, 20210831) // 🦄 unicorn
        map.put(127771, 20201001) // 🌛 first quarter moon face
        map.put(129420, 20201001) // 🦌 deer
        map.put(129668, 20210521) // 🪄 magic wand
        map.put(128171, 20201001) // 💫 dizzy
        map.put(128049, 20201001) // 🐱 meow cat face
        map.put(129409, 20201001) // 🦁 lion
        map.put(128293, 20201001) // 🔥 fire
        map.put(128038, 20210831) // 🐦 bird
        map.put(129415, 20201001) // 🦇 bat
        map.put(129417, 20210831) // 🦉 owl
        map.put(127752, 20201001) // 🌈 rainbow
        map.put(128053, 20201001) // 🐵 monkey face
        map.put(128029, 20201001) // 🐝 honeybee bumblebee wasp
        map.put(128034, 20201001) // 🐢 turtle
        map.put(128025, 20201001) // 🐙 octopus
        map.put(129433, 20201001) // 🦙 llama alpaca
        map.put(128016, 20210831) // 🐐 goat
        map.put(128060, 20201001) // 🐼 panda
        map.put(128040, 20201001) // 🐨 koala
        map.put(129445, 20201001) // 🦥 sloth
        map.put(128059, 20210831) // 🐻 bear
        map.put(128048, 20201001) // 🐰 rabbit face
        map.put(129428, 20201001) // 🦔 hedgehog
        map.put(128054, 20211115) // 🐶 dog puppy
        map.put(128041, 20211115) // 🐩 poodle dog
        map.put(129437, 20211115) // 🦝 raccoon
        map.put(128039, 20211115) // 🐧 penguin
        map.put(128012, 20210218) // 🐌 snail
        map.put(128045, 20201001) // 🐭 mouse face rat
        map.put(128031, 20210831) // 🐟 fish
        map.put(127757, 20201001) // 🌍 globe showing Europe-Africa
        map.put(127774, 20201001) // 🌞 sun with face
        map.put(127775, 20201001) // 🌟 glowing star
        map.put(11088, 20201001) // ⭐ star
        map.put(127772, 20201001) // 🌜 last quarter moon face
        map.put(129361, 20201001) // 🥑 avocado
        map.put(127820, 20211115) // 🍌 banana
        map.put(127827, 20210831) // 🍓 strawberry
        map.put(127819, 20210521) // 🍋 lemon
        map.put(127818, 20211115) // 🍊 tangerine orange
        return map[codePoint]
    }

    private val regex = EMOJI_REGEX.toRegex()

    @Trigger("nn")
    fun MessageEvent.matchw(): EventMatchResult? {
        val let = regex.find(message.content)?.let {
            if (it.next() != null) EventMatchResult(it)
            else null
        }
        return let
    }

}


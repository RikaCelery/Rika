package command.common.group.funny.petpet.plugin

import net.mamoe.mirai.console.data.value
import org.celery.config.AutoSavePluginConfigAutoReloadAble

object PetPetAutoSaveConfig : AutoSavePluginConfigAutoReloadAble("plugin-configs/PetPet") {

    val content: PluginConfig by value(PluginConfig())

}
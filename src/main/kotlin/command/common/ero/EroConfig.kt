package command.common.ero

import org.celery.Rika
import org.celery.config.Reloadable

object EroConfig: Reloadable(Rika.configFolderPath.resolve("plugin-configs/setuLib").toString())

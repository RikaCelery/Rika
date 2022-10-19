package org.celery.config.main

import org.celery.Rika
import org.celery.config.Reloadable

object PublicConfig:Reloadable(Rika.configFolderPath.resolve("publicConfigs").toString())
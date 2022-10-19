package org.celery.config.main

import org.celery.Rika
import org.celery.config.Reloadable

object CallConfig : Reloadable(Rika.configFolder.toString()+"/callConfigs")
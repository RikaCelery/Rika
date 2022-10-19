package org.celery.config.main.function

import org.celery.Rika
import org.celery.config.Reloadable

object CountLimit: Reloadable(Rika.configFolder.toString()+"/limits/CountLimit")
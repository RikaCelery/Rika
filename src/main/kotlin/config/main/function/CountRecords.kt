package org.celery.config.main.function

import org.celery.Rika
import org.celery.config.Reloadable

object CountRecords: Reloadable(Rika.configFolder.toString()+"/limits/CountRecords")
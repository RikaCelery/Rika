package org.celery.data

import org.celery.Rika
import org.celery.config.Reloadable

object TempData: Reloadable(Rika.dataFolderPath.toString()+"/tempvalue")
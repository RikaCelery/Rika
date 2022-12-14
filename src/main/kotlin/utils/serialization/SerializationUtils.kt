package org.celery.utils.serialization

import kotlinx.serialization.json.Json

val defaultJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    coerceInputValues = true
}
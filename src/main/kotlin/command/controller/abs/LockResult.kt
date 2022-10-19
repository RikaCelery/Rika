package org.celery.command.controller.abs

import org.celery.Rika
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class LockResult<out T>(val value: Any?) {

    /**
     * Returns `true` if this instance represents a successful outcome.
     * In this case [isFailure] returns `false`.
     */
    val isSuccess: Boolean get() = value !is LockResult.Failure && value !is Locked

    /**
     * Returns `true` if this instance represents a failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    val isFailure: Boolean get() = value is LockResult.Failure

    val isLocked: Boolean get() = value is Locked

    /**
     * Returns the encapsulated value if this instance represents [success][LockResult.isSuccess] or `null`
     * if it is [failure][LockResult.isFailure].
     *
     * This function is a shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */

    public inline fun getOrNull(): T? =
        when {
            isFailure -> null
            else -> value as T
        }

    /**
     * Returns the encapsulated [Throwable] exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is a shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is LockResult.Failure -> value.exception
            else -> null
        }

    /**
     * Returns a string `Success(v)` if this instance represents [success][LockResult.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    public override fun toString(): String =
        when (value) {
            is LockResult.Failure -> value.toString() // "Failure($exception)"
            else -> "Success($value)"
        }

    companion object {
        fun <T> locked(key: Pair<Pair<Long, Long>, String>): LockResult<T> {
            return LockResult(Locked(key))
        }

        fun <T> success(value: T): LockResult<T> {
            return LockResult(value)
        }

        fun <T> failure(e: Throwable): LockResult<T> {
            Rika.logger.debug("exception $e")
            return LockResult(Failure(e))
        }
    }

    internal class Failure(
        val exception: Throwable
    ) {
        override fun equals(other: Any?): Boolean = other is LockResult.Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }

    internal class Locked(
        val key: Pair<Pair<Long, Long>, String>
    ) {
        override fun equals(other: Any?): Boolean = other is LockResult.Locked && key == other.key
        override fun hashCode(): Int = key.hashCode()
        override fun toString(): String = "Locked($key)"
    }
}

/**
 * Throws exception if the result is failure. This internal function minimizes
 * inlined bytecode for [getOrThrow] and makes sure that in the future we can
 * add some exception-augmenting logic here (if needed).
 */
public fun LockResult<*>.throwOnFailure() {
    if (value is LockResult.Failure) throw value.exception
}

/**
 * Returns the encapsulated value if this instance represents [success][LockResult.isSuccess] or throws the encapsulated [Throwable] exception
 * if it is [failure][LockResult.isFailure].
 *
 * This function is a shorthand for `getOrElse { throw it }` (see [getOrElse]).
 */

inline fun <T> LockResult<T>.getOrThrow(): T {
    throwOnFailure()
    return value as T
}

/**
 * Returns the encapsulated value if this instance represents [success][LockResult.isSuccess] or the
 * result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][LockResult.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onFailure] function.
 *
 * This function is a shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 */

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> LockResult<T>.getOrElse(onFailure: (exception: Throwable) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> value as T
        else -> onFailure(exception)
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][LockResult.isSuccess] or the
 * [defaultValue] if it is [failure][LockResult.isFailure].
 *
 * This function is a shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
 */

public inline fun <R, T : R> LockResult<T>.getOrDefault(defaultValue: R): R {
    if (isFailure) return defaultValue
    return value as T
}

/**
 * Returns the result of [onSuccess] for the encapsulated value if this instance represents [success][LockResult.isSuccess]
 * or the result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][LockResult.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onSuccess] or by [onFailure] function.
 */

@OptIn(ExperimentalContracts::class)
public inline fun <R, T> LockResult<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Throwable) -> R
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> onSuccess(value as T)
        else -> onFailure(exception)
    }
}

// transformation

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [success][LockResult.isSuccess] or the
 * original encapsulated [Throwable] exception if it is [failure][LockResult.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [transform] function.
 * See [mapCatching] for an alternative that encapsulates exceptions.
 */

@OptIn(ExperimentalContracts::class)
public inline fun <R, T> LockResult<T>.map(transform: (value: T) -> R): LockResult<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        isSuccess -> LockResult.success(transform(value as T))
        else -> LockResult(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [success][LockResult.isSuccess] or the
 * original encapsulated [Throwable] exception if it is [failure][LockResult.isFailure].
 *
 * This function catches any [Throwable] exception thrown by [transform] function and encapsulates it as a failure.
 * See [map] for an alternative that rethrows exceptions from `transform` function.
 */

public inline fun <R, T> LockResult<T>.mapCatching(transform: (value: T) -> R): LockResult<R> {
    return when {
        isSuccess -> lockCatching { transform(value as T) }
        else -> LockResult(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable] exception
 * if this instance represents [failure][LockResult.isFailure] or the
 * original encapsulated value if it is [success][LockResult.isSuccess].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [transform] function.
 * See [recoverCatching] for an alternative that encapsulates exceptions.
 */

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> LockResult<T>.recover(transform: (exception: Throwable) -> R): LockResult<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> LockResult.success(transform(exception))
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable] exception
 * if this instance represents [failure][LockResult.isFailure] or the
 * original encapsulated value if it is [success][LockResult.isSuccess].
 *
 * This function catches any [Throwable] exception thrown by [transform] function and encapsulates it as a failure.
 * See [recover] for an alternative that rethrows exceptions.
 */

public inline fun <R, T : R> LockResult<T>.recoverCatching(transform: (exception: Throwable) -> R): LockResult<R> {
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> lockCatching { transform(exception) }
    }
}

/**
 * Performs the given [action] on the encapsulated [Throwable] exception if this instance represents [failure][LockResult.isFailure].
 * Returns the original `LockResult` unchanged.
 */

public inline fun <T> LockResult<T>.onFailure(action: (exception: Throwable) -> Unit): LockResult<T> {

    exceptionOrNull()?.let { action(it) }
    return this
}


public inline fun <T> LockResult<T>.onLocked(action: (LockResult<T>) -> Unit): LockResult<T> {
    if (isLocked)
        action(this)
    return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [success][LockResult.isSuccess].
 * Returns the original `LockResult` unchanged.
 */


public inline fun <T> LockResult<T>.onSuccess(action: (value: T) -> Unit): LockResult<T> {

    if (isSuccess) action(value as T)
    return this
}

public inline fun <R> lockCatching(block: () -> R): LockResult<R> {
    return try {
        LockResult.success(block())
    } catch (e: Throwable) {
        LockResult.failure(e)
    }
}
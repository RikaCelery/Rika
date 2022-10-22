package events

sealed class ExecutionResult {
    object Success : ExecutionResult()

    /**
     * 指令没有正常执行，但需要限制调用频率
     */
    object LimitCall: ExecutionResult()
    class Failed(val exception: Throwable? = null, val message:String? = null) : ExecutionResult(){
        constructor(exception: Throwable): this(exception, null)
    }
    class Ignored(val reason: String? = null) : ExecutionResult(){
        companion object NoReason:ExecutionResult()
    }
    object Unknown : ExecutionResult()
    class Error(val cause: Throwable? = null) : ExecutionResult()
}
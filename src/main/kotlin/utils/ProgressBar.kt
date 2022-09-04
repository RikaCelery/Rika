package com.celery.rika.utils

import com.celery.com.celery.rika.Rika
import javax.annotation.concurrent.NotThreadSafe

/**
 * 进度条工具
 *
 *
 * 这个进度条打印期间，其他控制台输出会影响最终结果
 * 所以做成单线程，阻塞的打印
 *
 * @author snx
 */
@NotThreadSafe
class ProgressBar private constructor() {
    /**
     * 当前进度
     */
    var index = 0

    /**
     * 步长
     */
    private var step = 1

    /**
     * 进度条长度,总进度数值
     */
    private val barLength = 100

    /**
     * 是否初始化
     */
    private var hasInited = false

    /**
     * 是否已经结束
     */
    private var hasFinished = false

    /**
     * 进度条title
     */
    private var title = "Progress:"
    private fun generate(num: Int, ch: Char): String {
        if (num == 0) return ""
        val builder = StringBuilder()
        for (i in 0 until num) {
            builder.append(ch)
        }
        return builder.toString()
    }

    private fun genProcess(num: Int): String {
        return generate(num, processChar)
    }

    private fun genWaitProcess(num: Int): String {
        return generate(num, waitChar)
    }

    /**
     * 清空进度条
     */
    private fun cleanProcessBar() {
        print(generate(barLength / step + 6, '\b'))
    }

    /**
     * 进度+1
     */
    fun process() {
        checkStatus()
        checkInit()
        cleanProcessBar()
        index++
        drawProgressBar()
        checkFinish()
    }

    /**
     * 进度+指定数值
     *
     * @param process 指定数值
     */
    fun process(process: Int) {
        checkStatus()
        checkInit()
        cleanProcessBar()
        if (index + process >= barLength) index = barLength else index += process
        drawProgressBar()
        checkFinish()
    }

    /**
     * 步进
     */
    fun step() {
        checkStatus()
        checkInit()
        cleanProcessBar()
        if (index + step >= barLength) index = barLength else index += step
        drawProgressBar()
        checkFinish()
    }

    /**
     * 绘制进度条
     */
    fun drawProgressBar() {
        Rika.logger.info(
            String.format(
                "%3d%%├%s%s┤",
                index,
                genProcess(index / step),
                genWaitProcess(barLength / step - index / step)
            )
        )
    }

    val stringProgressBar: String
        get() = String.format("├%s%s┤", genProcess(index / step), genWaitProcess(barLength / step - index / step))

    /**
     * 检查进度条状态
     * 已完成的进度条不可以继续执行
     */
    private fun checkStatus() {
        check(!hasFinished) { "进度条已经完成" }
    }

    /**
     * 检查是否已经初始化
     */
    private fun checkInit() {
        if (!hasInited) init()
    }

    /**
     * 检查是否已经完成
     */
    private fun checkFinish() {
        if (hasFinished() && !hasFinished) finish()
    }

    /**
     * 是否已经完成进度条
     *
     * @return
     */
    fun hasFinished(): Boolean {
        return index >= barLength
    }

    /**
     * 初始化进度条
     */
    private fun init() {
        checkStatus()
        print(title)
        print(
            String.format(
                "%3d%%[%s%s]",
                index,
                genProcess(index / step),
                genWaitProcess(barLength / step - index / step)
            )
        )
        hasInited = true
    }

    /**
     * 结束进度条，由 checkFinish()调用
     */
    private fun finish() {
        println()
        hasFinished = true
    }

    /**
     * 间隔50ms 自动执行进度条
     *
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    fun printProgress() {
        init()
        do {
            step()
            Thread.sleep(50)
            index++
        } while (index <= barLength)
        println()
    }

    companion object {
        private const val processChar = '─'

        //        private const val processChar = '█'
        private const val waitChar = ' '

        //        private const val waitChar = '─'
        fun build(): ProgressBar {
            return ProgressBar()
        }

        fun build(step: Int): ProgressBar {
            val progressBar = build()
            progressBar.step = step
            return progressBar
        }

        fun build(index: Int, step: Int): ProgressBar {
            val progressBar = build(step)
            progressBar.index = index
            return progressBar
        }

        fun build(index: Int, step: Int, title: String): ProgressBar {
            val progressBar = build(index, step)
            progressBar.title = title
            return progressBar
        }
    }
}
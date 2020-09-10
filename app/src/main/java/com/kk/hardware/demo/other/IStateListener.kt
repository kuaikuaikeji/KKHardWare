package com.kk.hardware.demo.other

import android.app.Dialog
import android.view.View
import android.widget.ProgressBar

/**
 * 加载状态回调
 * @author zhangtao
 * @date 2020/6/28 16:35
 */
interface IStateListener {

    /** 开始 */
    fun start()

    /** 成功 */
    fun success() {
        finish()
    }

    /** 错误 */
    fun fail() {
        finish()
    }

    /** 结束 */
    fun finish()

    /** 进度条显示 */
    class ProgressBarStateListener(private val progressBar: ProgressBar) :
        IStateListener {
        override fun start() {
            progressBar.visibility = View.VISIBLE
        }

        override fun finish() {
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * loading
     */
    class DialogStateListener(private val dialog: Dialog) :
        IStateListener {
        override fun start() {
            dialog.show()
        }

        override fun finish() {
            dialog.cancel()
        }
    }
}
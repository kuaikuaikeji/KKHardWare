package com.kk.hardware.demo

import android.view.ViewManager
import android.widget.Button
import androidx.appcompat.view.ContextThemeWrapper
import com.blankj.utilcode.util.Utils
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.dip

/**
 * 拓展
 * @author zhangtao
 * @date 2020/7/31 17:54
 */

inline fun ViewManager.styledButton(
    text: CharSequence?,
    styleRes: Int = 0,
    init: Button.() -> Unit
): Button {
    return ankoView({
        if (styleRes == 0) Button(it)
        else Button(
            ContextThemeWrapper(it, styleRes),
            null,
            0
        )
    }, 0) {
        init()
        setText(text)
    }
}

inline fun ViewManager.styledButton(
    text: CharSequence?,
    init: Button.() -> Unit
): Button {
    return styledButton(text, R.style.ButtonPrimary, init)
}

val titleHeight: Int by lazy { Utils.getApp().dip(40) }
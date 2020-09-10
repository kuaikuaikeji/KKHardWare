package com.kk.hardware.demo.armlet

import android.content.Context
import com.blankj.utilcode.util.ToastUtils
import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.peripheral.IWriteListener
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import java.util.*

/**
 * 指令写入回调
 * @author zhangtao
 * @date 2020/6/28 16:58
 */
class WriteListener(private val mContext: Context) : IWriteListener {
    companion object {
        fun Context.commandWriteError(command: Byte, e: KKPeripheralException) {
            alert {
                title = String.format(Locale.getDefault(), "指令写入失败-0x%02x", command)
                message = "${e.message}-${e.code}"
                isCancelable = false
                okButton { }
            }.show()
        }
    }

    override fun onResult(command: Byte, e: KKPeripheralException?) {
        if (e != null) {
            mContext.commandWriteError(command, e)
            return
        }
        ToastUtils.showShort(String.format(Locale.getDefault(), "指令写入成功-0x%02x", command))
    }
}
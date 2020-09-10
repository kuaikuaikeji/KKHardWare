package com.kk.hardware.demo.other

import com.blankj.utilcode.util.ToastUtils
import com.kk.hardware.core.IConnectListener
import com.kk.hardware.core.IKKPeripheral
import com.kk.hardware.core.KKPeripheralException

/**
 * 设备连接回调
 * @author zhangtao
 * @date 2020/6/28 16:34
 */
open class ConnectListener<T : IKKPeripheral>(
    private val stateListener: IStateListener,
    private val success: T.() -> Unit
) : IConnectListener<T> {
    override fun onStart() {
        stateListener.start()
    }

    override fun onConnectSuccess(peripheral: T) {
        ToastUtils.showShort("连接成功")
        stateListener.success()
        success.invoke(peripheral)
    }

    override fun onConnectFail(
        peripheral: T?,
        exception: KKPeripheralException
    ) {
        stateListener.fail()
        ToastUtils.showShort("连接失败\n${exception.message}-${exception.code}")
    }

    override fun onDisconnected(isActive: Boolean, peripheral: T) {
        ToastUtils.showShort("连接断开：${if (isActive) "主动断开" else "非主动断开"}")
    }
}
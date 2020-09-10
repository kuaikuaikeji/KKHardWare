package com.kk.hardware.demo.ota

import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.kk.hardware.ble.dfu.DfuController
import com.kk.hardware.ble.dfu.listener.UpgradeListenerAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 设备固件升级回调
 * @author zhangtao
 * @date 2020/7/2 19:44
 */
class UpgradeListener(
    private val activity: ComponentActivity,
    private val mTvState: TextView,
    private val mProgressBar: ProgressBar
) :
    UpgradeListenerAdapter() {
    override fun onDeviceConnecting(deviceAddress: String) {
        setStateText("onDeviceConnecting")
    }

    override fun onDeviceConnected(deviceAddress: String) {
        setStateText("onDeviceConnected")
    }

    override fun onDfuProcessStarting(deviceAddress: String) {
        setStateText("onDfuProcessStarting")
    }

    override fun onDfuProcessStarted(deviceAddress: String) {
        setStateText("onDfuProcessStarted")
    }

    override fun onEnablingDfuMode(deviceAddress: String) {
        setStateText("onEnablingDfuMode")
    }

    override fun onProgressChanged(
        deviceAddress: String, percent: Int, speed: Float,
        avgSpeed: Float, currentPart: Int, partsTotal: Int
    ) {
        mProgressBar.progress = percent
        mTvState.text = String.format(
            Locale.getDefault(),
            "onProgressChanged\n百分比：%d%%\n速度：%dKB/s\n平均速度：%dKB/s\n进度：%d/%d",
            percent, speed.div(1024).times(1000).toInt(),
            avgSpeed.div(1024).times(1000).toInt(), currentPart, partsTotal
        )
    }

    override fun onFirmwareValidating(deviceAddress: String) {
        setStateText("onFirmwareValidating")
    }

    override fun onDeviceDisconnecting(deviceAddress: String?) {
        setStateText("onDeviceDisconnecting")
    }

    override fun onDeviceDisconnected(deviceAddress: String) {
        setStateText("onDeviceDisconnected")
    }

    override fun onDfuCompleted(deviceAddress: String) {
        setStateText("onDfuCompleted")
        activity.lifecycleScope.launch {
            delay(1000 * 3)
            ToastUtils.showShort("dfu 成功")
            activity.finish()
        }
    }

    override fun onDfuAborted(deviceAddress: String) {
        setStateText("onDfuAborted")
    }

    override fun onError(
        deviceAddress: String, error: Int, errorType: Int, message: String?
    ) {
        mTvState.text = String.format(
            Locale.getDefault(),
            "onError\nerror：%d\nerrorType：%d\nmessage：%s",
            error, errorType, message
        )
    }

    override fun onDownloadProgress(percent: Int) {
        setStateText("下载进度：${percent}%")
    }

    override fun onDownloadComplete(e: Exception?) {
        setStateText("下载${if (e == null) "成功" else "失败：${e.message}"}")
    }

    override fun onDfuStart(controller: DfuController) {
        setStateText("dfu 升级开始")
    }

    private fun setStateText(text: String) {
        mTvState.text = text
    }
}
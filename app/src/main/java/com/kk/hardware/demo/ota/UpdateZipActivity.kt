package com.kk.hardware.demo.ota

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.GsonUtils
import com.kk.hardware.ble.AbstractKKBlePeripheral
import com.kk.hardware.ble.dfu.DfuHelper
import com.kk.hardware.ble.dfu.DfuHelperObserver
import com.kk.hardware.ble.dfu.DfuService
import com.kk.hardware.ble.dfu.listener.UpgradeListenerAdapter
import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.net.DeviceApi
import com.kk.hardware.core.net.upgrade.UpgradeCallback
import com.kk.hardware.core.net.upgrade.UpgradeVersionEntity
import com.kk.hardware.demo.R
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * 处理升级逻辑的 Activity
 * @author zhangtao
 * @date 2020/6/28 18:41
 */
class UpdateZipActivity : AppCompatActivity() {

    companion object {

        @JvmStatic
        fun startUpdateZip(context: Context, mac: String, entity: UpgradeVersionEntity? = null) {
            context.startActivity<UpdateZipActivity>(
                "mac" to mac,
                "entity" to entity?.let { GsonUtils.toJson(entity) }
            )
        }
    }

    private lateinit var mProgressBar: ProgressBar
    private lateinit var mTvState: TextView
    private lateinit var mMac: String
    private lateinit var mEntity: UpgradeVersionEntity
    private lateinit var mListener: DfuProgressListener
    private val mDfuHelper: DfuHelper by lazy { DfuHelperObserver(this) }
    private val mUpgradeListener: UpgradeListenerAdapter by lazy {
        UpgradeListener(this,mTvState, mProgressBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMac = intent?.getStringExtra("mac") ?: return
        intent?.getStringExtra("entity")?.let {
            mEntity = GsonUtils.fromJson(it, UpgradeVersionEntity::class.java)
        }
        verticalLayout {
            padding = dip(10)
            mProgressBar = horizontalProgressBar().lparams(-1, -2)
            mTvState = textView().lparams { topMargin = dip(10) }
//            linearLayout {
//                gravity = Gravity.BOTTOM
//
//                button("在线升级") { onClick { onlineDfu() } }
//                    .lparams(0, -2) { weight = 1f }
//
//                button("本地") { onClick { testDfu() } }
//                    .lparams(0, -2) { weight = 1f }
//            }.lparams(-1, -1)
        }
        if (this::mEntity.isInitialized) onlineDfu()
        else testDfu()
    }

    /** 在线升级 */
    private fun onlineDfu() {
        mDfuHelper.setListener(mUpgradeListener)
        mDfuHelper.upgrade(mMac, mEntity)

//        DeviceApi.upgradeArmlet(1.18f, object : UpgradeCallback {
//            override fun upgrade(entity: UpgradeVersionEntity?) {
//                if (entity == null) {
//                    setStateText("当前为最新版")
//                    return
//                }
//                mDfuHelper.upgrade(mMac, entity)
//            }
//
//            override fun error(e: KKPeripheralException) {
//                setStateText("版本信息加载失败：${e.message}")
//            }
//        })
    }

    /** 本地升级 */
    private fun testDfu() {
        if (!this::mListener.isInitialized) {
            mListener = com.kk.hardware.ble.dfu.listener.DfuProgressListener(mUpgradeListener)
            DfuServiceListenerHelper.registerProgressListener(this, mListener)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(this)
        }
        DfuServiceInitiator(mMac)
            .setDeviceName(AbstractKKBlePeripheral.DEVICE_NAME_OTA)
            .setKeepBond(false)
            .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
            .setZip(R.raw.household_aband_118_app_s340)
            .start(this, DfuService::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mListener.isInitialized) {
            DfuServiceListenerHelper.unregisterProgressListener(this, mListener)
        }
    }

    private fun setStateText(text: String) {
        mTvState.text = text
    }
}
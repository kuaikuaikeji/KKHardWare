package com.kk.hardware.demo

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.FragmentUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.SpanUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.clj.fastble.BleManager
import com.kk.core.common.extensions.*
import com.kk.core.common.utils.LogUtils
import com.kk.core.view.dialog.DataLoadingDialog
import com.kk.hardware.ble.AbstractKKBlePeripheral
import com.kk.hardware.ble.KKBleCentralImpl
import com.kk.hardware.ble.peripheral.BleArmletPeripheralImpl
import com.kk.hardware.ble.peripheral.BleHealthScalePeripheralImpl
import com.kk.hardware.core.IConnectListener
import com.kk.hardware.core.IScanListener
import com.kk.hardware.core.KKHardWare
import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.peripheral.KKHealthScalePeripheral
import com.kk.hardware.core.peripheral.armlet.KKArmletPeripheral
import com.kk.hardware.demo.armlet.ArmletFragment
import com.kk.hardware.demo.scale.HealthScaleFragment
import kotlinx.android.synthetic.main.item_device.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                LogUtils.d("io init", "当前登录用户", UserData.name)
            }
            init()
        }
    }

    private fun init() {
        window.apply {
            transparentStatusBar()
            transparentNavBar()
            setStatusBarLightMode(true)
            setNavBarLightMode(true)
        }

        KKHardWare.init("${UserData.userId}", application)
        KKBleCentralImpl.init()
        val millis = System.currentTimeMillis()
        verticalLayout {
            backgroundColor = Color.WHITE
            gravity = Gravity.CENTER_HORIZONTAL
            clipToPadding = false
            addPaddingBottomEqualNavBarHeight()

            frameLayout {
                addPaddingTopEqualStatusBarHeight()
                backgroundColor = 0xDDFFFFFF.toInt()
                ViewCompat.setElevation(this, dip(2).toFloat())
                textView("智能设备") {
                    textSize = 18f
                    gravity = Gravity.CENTER
                    textColor = 0xFF333333.toInt()
                }.lparams(-2, titleHeight) { gravity = Gravity.CENTER }
            }

            recyclerView {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = mAdapter
            }.lparams(-1, 0) { weight = 1f }

            mProgressBar = horizontalProgressBar {
                isIndeterminate = true
                visibility = View.GONE
            }
            linearLayout {
                styledButton("扫描", R.style.ButtonPrimary) {
                    id = R.id.btn_ok
                    setOnClickListener(this@MainActivity)
                }.lparams(0, -2) { weight = 1f }
                styledButton("取消扫描", R.style.ButtonPrimary) {
                    id = R.id.btn_cancel
                    backgroundResource = R.drawable.selector_btn_border
                    setTextColor(
                        ContextCompat.getColorStateList(
                            this@MainActivity, R.color.selector_color_primary
                        )
                    )
                    setOnClickListener(this@MainActivity)
                }.lparams(0, -2) { weight = 1f }
            }
        }
        LogUtils.d("UI 加载耗时", System.currentTimeMillis() - millis)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_ok -> {
                if (!KKBleCentralImpl.isSupportBle()) {
                    ToastUtils.showShort("当前设备不支持蓝牙")
                    return
                }
                if (!KKBleCentralImpl.isBlueEnable()) {
                    ToastUtils.showShort("请开启蓝牙后重试")
                    KKBleCentralImpl.enableBluetooth()
                    return
                }
                if (!KKBleCentralImpl.isLocationEnabled(this)) {
                    ToastUtils.showShort("请开启位置服务后重试")
                    return
                }
                val callback = object : PermissionUtils.SimpleCallback {
                    override fun onGranted() {
//                        KKBleCentralImpl.scan(BleArmletPeripheralImpl::class.java, scanCallback)
                        KKBleCentralImpl.scan(
                            false,
                            scanCallback,
                            BleHealthScalePeripheralImpl::class.java,
                            BleArmletPeripheralImpl::class.java
                        )
                    }

                    override fun onDenied() {
                        ToastUtils.showShort("扫描取消")
                    }
                }
                BleManager.getInstance().disconnectAllDevice()
                PermissionUtils.permission(PermissionConstants.LOCATION)
                    .callback(callback)
                    .request()
            }
            R.id.btn_cancel -> {
                KKBleCentralImpl.cancelScan()
            }
        }
    }


    /**
     * 扫描 loading
     */
    private val dialog: Dialog by lazy { DataLoadingDialog(this).apply { setCancelable(true) } }

    /**
     * 扫描回调
     */
    private val scanCallback: IScanListener<AbstractKKBlePeripheral> by lazy {
        object : IScanListener<AbstractKKBlePeripheral> {
            override fun onStart(success: Boolean) {
                dialog.show()
                mAdapter.setNewData(null)
                ToastUtils.showShort("$success")
            }

            override fun onScaning(peripheral: AbstractKKBlePeripheral) {
                mAdapter.addData(peripheral)
            }

            override fun onEnd() {
                ToastUtils.showShort("end")
                dialog.cancel()
            }
        }
    }

    /**
     * 搜索设备列表适配器
     */
    private val mAdapter: BaseQuickAdapter<AbstractKKBlePeripheral, BaseViewHolder> by lazy {
        object : BaseQuickAdapter<AbstractKKBlePeripheral, BaseViewHolder>(R.layout.item_device) {
            private val titleColor = ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
            override fun convert(helper: BaseViewHolder, item: AbstractKKBlePeripheral) {
                helper.itemView.tv_name.text =
                    SpanUtils().appendLine(item.getName())
                        .setForegroundColor(titleColor).setFontSize(16, true)
                        .append(item.getMac())
                        .setForegroundColor(0xFF999999.toInt()).setFontSize(12, true)
                        .create()
                val drawable = (item is KKHealthScalePeripheral) then R.drawable.ic_scale
                    ?: R.drawable.ic_watch
                helper.itemView.tv_name.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, drawable, 0
                )
            }
        }.also {
            val emptyView =
                UI { frameLayout { textView("暂无设备").lparams { gravity = Gravity.CENTER } } }.view
            it.setEmptyView(emptyView)
            var lastTime = 0L
            val connectDialog: Dialog by lazy { DataLoadingDialog(this) }
            // 列表点击
            it.setOnItemClickListener { _, _, position ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTime < 500) {
                    return@setOnItemClickListener
                }
                lastTime = currentTime
                when (val item = mAdapter.getItem(position)) {
                    is KKArmletPeripheral -> {
                        KKBleCentralImpl.cancelScan()
                        val fragment = ArmletFragment.newInstance(item.getMac())
                        FragmentUtils.add(
                            supportFragmentManager, fragment,
                            android.R.id.content, false, true
                        )
                    }
                    is KKHealthScalePeripheral -> {
                        KKBleCentralImpl.cancelScan()
                        val fragment = HealthScaleFragment.newInstance(item.getMac())
                        KKBleCentralImpl.connect(
                            item.getMac(),
                            BleHealthScalePeripheralImpl::class.java,
                            object : IConnectListener<BleHealthScalePeripheralImpl> {
                                override fun onConnectFail(
                                    peripheral: BleHealthScalePeripheralImpl?,
                                    exception: KKPeripheralException
                                ) {
                                    ToastUtils.showShort("连接失败\n${exception.message}-${exception.code}")
                                    connectDialog.cancel()
                                }

                                override fun onConnectSuccess(peripheral: BleHealthScalePeripheralImpl) {
                                    KKBleCentralImpl.disconnect(peripheral)
                                }

                                override fun onDisconnected(
                                    isActive: Boolean, peripheral: BleHealthScalePeripheralImpl
                                ) {
                                    connectDialog.cancel()
                                    if (onBackPressedDispatcher.hasEnabledCallbacks()) return
                                    FragmentUtils.add(
                                        supportFragmentManager, fragment,
                                        android.R.id.content, false, true
                                    )
//                                    ToastUtils.showShort("连接断开")
                                }

                                override fun onStart() {
                                    connectDialog.show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        KKBleCentralImpl.destroy()
        super.onDestroy()
    }
}

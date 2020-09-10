package com.kk.hardware.demo.armlet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.kk.core.common.extensions.addPaddingBottomEqualNavBarHeight
import com.kk.core.common.extensions.addPaddingTopEqualStatusBarHeight
import com.kk.core.common.extensions.then
import com.kk.core.view.dialog.DataLoadingDialog
import com.kk.hardware.ble.KKBleCentralImpl
import com.kk.hardware.ble.peripheral.BleArmletPeripheralImpl
import com.kk.hardware.core.IConnectListener
import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.command.*
import com.kk.hardware.core.command.base.AbstractModelToBytes
import com.kk.hardware.core.command.base.DeviceElectricQuantityModel
import com.kk.hardware.core.peripheral.IReadElectricQuantityListener
import com.kk.hardware.core.peripheral.IWriteListener
import com.kk.hardware.demo.*
import com.kk.hardware.demo.armlet.WriteListener.Companion.commandWriteError
import com.kk.hardware.demo.other.ConnectListener
import com.kk.hardware.demo.other.IStateListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.UI
import org.jetbrains.anko.support.v4.nestedScrollView

/**
 * 臂带相关
 * @author zhangtao
 * @date 2020/6/23 18:08
 */
class ArmletFragment private constructor() : BaseFragment() {

    companion object {

        /**
         * 实例化臂带 Fragment
         * @param mac 设备 mac 地址
         */
        @JvmStatic
        fun newInstance(mac: String): ArmletFragment = ArmletFragment()
            .apply {
                arguments = Bundle().apply { this.mac = mac }
            }
    }

    private val mMac: String by lazy { arguments?.mac ?: "" }
    private var mPeripheral: BleArmletPeripheralImpl? = null

    /**
     * 连接 loading
     */
    private val dialog: Dialog by lazy { DataLoadingDialog(this).apply { setCancelable(true) } }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = UI {
        frameLayout {
            backgroundColor = 0xFFFCE5D6.toInt()
            linearLayout {
                nestedScrollView {
                    isFillViewport = true
                    backgroundColor = Color.WHITE
                    ViewCompat.setElevation(this, dip(1).toFloat())
                    verticalLayout {
                        topPadding = titleHeight
                        clipToPadding = false
                        addPaddingTopEqualStatusBarHeight()
                        addPaddingBottomEqualNavBarHeight()
                        textView("臂带设置") {
                            verticalPadding = dip(5)
                            horizontalPadding = dip(10)
                            topPadding = dip(10)
                            textColor = 0xFF333333.toInt()
                        }
                        styledButton("连接") {
                            onClick {
                                if (mPeripheral?.isConnected() == true) {
                                    ToastUtils.showShort("已连接")
                                    return@onClick
                                }
                                KKBleCentralImpl.connect(
                                    mMac, BleArmletPeripheralImpl::class.java, mConnectCallback
                                )
                            }
                        }
                        styledButton("断开连接", checkClick { KKBleCentralImpl.disconnect(this) })

                        styledButton(
                            "绑定设备",
                            checkClick {
                                val listener = object : IWriteListener {
                                    override fun onResult(
                                        command: Byte, e: KKPeripheralException?
                                    ) {
                                        if (e == null) mAdapter.addSimple("绑定设备", "请点击任一按钮绑定设备")
                                        else context?.commandWriteError(command, e)
                                    }
                                }
                                cancelBindJob()
                                job = lifecycleScope.launch {
                                    while (isActive) {
                                        sendCommand(ArmletBindDeviceCommand(), listener)
                                        delay(1000 * 2)
                                    }
                                }
                            }
                        )
                        styledButton(
                            "查找设备",
                            checkClick(ArmletFindDeviceCommand(), object : IWriteListener {
                                override fun onResult(command: Byte, e: KKPeripheralException?) {
                                    if (e == null) mAdapter.addSimple("查找设备", "目标设备正在震动")
                                    else context?.commandWriteError(command, e)
                                }
                            })
                        )
//                        styledButton(
//                            "设置用户信息",
//                            checkClick(ArmletSetUserCommand(userId, 172, 61f, 25, 1, 65))
//                        )
                        styledButton("设置亮度", checkClick {
                            val random = (1..10).random()
                            mAdapter.addSimple("设置亮度", "$random")
                            val command = ArmletSetBrightCommand(random, 0)
                            sendCommand(command, mWriteListener)
                        })
                        styledButton("设置时间", checkClick {
                            val current = System.currentTimeMillis() / 1000
                            val random = (current - 60 * 60..current + 60 * 60).random()
                            mAdapter.addSimple("设置时间", TimeUtils.millis2String(random * 1000))
                            val command = ArmletSetTimeCommand(UserData.userId, random.toInt())
                            sendCommand(command, mWriteListener)
                        })

                        textView("开始运动") {
                            verticalPadding = dip(5)
                            horizontalPadding = dip(10)
                            textColor = 0xFF333333.toInt()
                        }
                        styledButton("开始训练", checkClick {
                            val command = ArmletStartTrainCommand(9, getNowSeconds(), 1, 120, 21)
                            sendCommand(command, mWriteListener)
                        })
                        styledButton("结束训练", checkClick(ArmletEndTrainCommand(9, getNowSeconds())))

                        styledButton("获取心率数据", checkClick(ArmletHeartrateCommand(9, 10)))
                        styledButton("历史运动数据", checkClick(ArmletHistorySportDataCommand()))

//                        styledButton("设置设备 ID", checkClick(ArmletSetDeviceIdCommand(111)))
//                        styledButton("进入 2.4 G 模式", checkClick(ArmletJumpTwoPointFourCommand()))
//                        val actionCommand = ArmletActionCommand(
//                            1, 1, 3, 2, 10000,
//                            200, 20, 200, 20,
//                            200, 20, 200, 20,
//                            200, 20, 200, 20
//                        )
//                        styledButton("动作命令请求", checkClick(actionCommand))
//                        styledButton("切换工厂模式", checkClick(ArmletInOrOutFactoryCommand()))

                        textView("系统设置") {
                            verticalPadding = dip(5)
                            horizontalPadding = dip(10)
                            textColor = 0xFF333333.toInt()
                        }
                        styledButton("固件版本", checkClick {
                            mResultListener.isGetVersionOnly = true
                            sendCommand(ArmletVersionCommand(), mWriteListener)
                        })
                        styledButton("设备电量", checkClick {
                            readElectricQuantity(object : IReadElectricQuantityListener {
                                override fun onSuccess(model: DeviceElectricQuantityModel) {
                                    mAdapter.addSimple("当前电量", "${model.value}%")
                                }

                                override fun onFail(e: KKPeripheralException) {
                                    mAdapter.addSimple("电量获取失败", "${e.message}  ${e.code}")
                                }
                            })
                        })
                        styledButton("版本升级", checkClick {
                            mResultListener.isGetVersionOnly = false
                            sendCommand(ArmletVersionCommand(), mWriteListener)
                        })
                        styledButton("版本回退", checkClick(ArmletInOtaCommand()))
                    }.lparams(-2, -1)
                }
                recyclerView {
                    topPadding = titleHeight
                    clipToPadding = false
                    addPaddingTopEqualStatusBarHeight()
                    addPaddingBottomEqualNavBarHeight()
                    layoutManager = LinearLayoutManager(this@UI.ctx)
                        .apply { reverseLayout = true }
                    adapter = mAdapter
                }.lparams(-1, -2)
            }

            frameLayout {
                backgroundColor = 0xDDFFFFFF.toInt()
                addPaddingTopEqualStatusBarHeight()
                ViewCompat.setElevation(this, dip(2).toFloat())
                textView("返回") {
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0)
                    horizontalPadding = dip(10)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    textColor = 0xFF333333.toInt()
                    backgroundResource = R.drawable.selector_normal
                    onClick { activity?.onBackPressed() }
                }.lparams(-2, titleHeight)
                textView("智能臂带") {
                    textSize = 18f
                    textColor = 0xFF333333.toInt()
                }.lparams { gravity = Gravity.CENTER }
            }.lparams(-1, -2)
        }
    }.view

    override fun onResume() {
        super.onResume()
        if (mPeripheral?.isConnected() == true) return
        KKBleCentralImpl.connect(
            mMac, BleArmletPeripheralImpl::class.java, mConnectCallback
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UserDialog(view.context)
            .setOnOkListener { }
            .setOnCancelListener { activity?.onBackPressed() }
            .show()
    }

    /**
     * 检测设备连接状态并设置点击事件发送指令
     * @param command 需要发送的指令
     * @param click 点击事件
     * @return true：未连接；false：已连接
     */
    private fun checkClick(
        command: AbstractModelToBytes? = null,
        click: (BleArmletPeripheralImpl.() -> Unit)? = null
    ): (@AnkoViewDslMarker Button).() -> Unit =
        checkClick(command, writeListener = null, click = click)


    /**
     * 检测设备连接状态并设置点击事件发送指令
     * @param command 需要发送的指令
     * @param click 点击事件
     * @return true：未连接；false：已连接
     */
    private fun checkClick(
        command: AbstractModelToBytes? = null,
        writeListener: IWriteListener? = null,
        click: (BleArmletPeripheralImpl.() -> Unit)? = null
    ): (@AnkoViewDslMarker Button).() -> Unit {
        return {
            onClick {
                if (mPeripheral?.isConnected() == true) {
                    if (command != null) {
                        mPeripheral!!.sendCommand(command, writeListener ?: mWriteListener)
                    }
                    click?.invoke(mPeripheral!!)
                } else {
                    ToastUtils.showShort("请先连接设备")
                }
            }
        }
    }

    override fun onDestroy() {
        mPeripheral?.takeIf { it.isConnected() }?.run { KKBleCentralImpl.disconnect(this) }
        super.onDestroy()
    }

    /** 获取当前时间 秒 */
    private fun getNowSeconds() = (System.currentTimeMillis() / 1000).toInt()

    /** 指令回调记录 */
    private val mAdapter: MessageAdapter by lazy { MessageAdapter() }

    /** 指令写入回调 */
    private val mWriteListener: IWriteListener by lazy { WriteListener(requireContext()) }

    /**
     * 绑定设备命令
     */
    private var job: Job? = null

    /**
     * 取消绑定任务
     */
    private fun cancelBindJob() {
        job?.takeIf { !it.isCancelled && !it.isCompleted }?.cancel()
        job = null
    }

    /** 指令接收监听 */
    private val mResultListener: ArmletResultListener by lazy {
        object : ArmletResultListener(mAdapter, mWriteListener) {
            override fun onBindDeviceResult(result: ArmletBindDeviceModel) {
                super.onBindDeviceResult(result)
                cancelBindJob()
            }
        }
    }

    /** 设备连接回调 */
    private val mConnectCallback: IConnectListener<BleArmletPeripheralImpl> by lazy {
        val stateListener = IStateListener.DialogStateListener(dialog)
        return@lazy object :
            ConnectListener<BleArmletPeripheralImpl>(stateListener, {
                mPeripheral = apply { resultListener = mResultListener }
            }) {
            override fun onStart() {
                super.onStart()
                mAdapter.addSimple("连接", "开始")
            }

            override fun onConnectSuccess(peripheral: BleArmletPeripheralImpl) {
                super.onConnectSuccess(peripheral)
                UserData.lastBindTime = System.currentTimeMillis()
                mAdapter.addSimple("连接", "成功")
                mResultListener.armletPeripheral = peripheral
                checkClick(
                    ArmletSetUserCommand(
                        UserData.userId, UserData.height, UserData.weight,
                        UserData.age, (UserData.sex == 1) then { 1 } ?: 2, UserData.staticHeart
                    )
                )
            }

            override fun onConnectFail(
                peripheral: BleArmletPeripheralImpl?,
                exception: KKPeripheralException
            ) {
                super.onConnectFail(peripheral, exception)
                cancelBindJob()
                mAdapter.addDetails("连接：失败", "${exception.message}-${exception.code}")
            }

            override fun onDisconnected(isActive: Boolean, peripheral: BleArmletPeripheralImpl) {
                super.onDisconnected(isActive, peripheral)
                cancelBindJob()
                mAdapter.addSimple("连接", "断开（${if (isActive) "主动断开" else "非主动断开"}）")
                mResultListener.armletPeripheral = null
            }
        }
    }
}
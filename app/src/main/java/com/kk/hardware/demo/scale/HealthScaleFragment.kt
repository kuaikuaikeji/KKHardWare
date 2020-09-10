package com.kk.hardware.demo.scale

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Layout
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SpanUtils
import com.kk.core.common.extensions.*
import com.kk.core.view.dialog.DataLoadingDialog
import com.kk.hardware.ble.KKBleCentralImpl
import com.kk.hardware.ble.peripheral.BleHealthScalePeripheralImpl
import com.kk.hardware.core.IConnectListener
import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.command.UserModel
import com.kk.hardware.core.command.other.LFPeopleGeneral
import com.kk.hardware.core.net.HealthScaleApi
import com.kk.hardware.core.peripheral.KKHealthScalePeripheral
import com.kk.hardware.demo.*
import com.kk.hardware.demo.other.ConnectListener
import com.kk.hardware.demo.other.IStateListener
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.UI
import org.jetbrains.anko.support.v4.alert
import org.json.JSONObject
import java.util.*

/**
 * 健康秤相关
 * @author zhangtao
 * @date 2020/6/28 17:01
 */
class HealthScaleFragment : BaseFragment() {
    companion object {
        /**
         * 实例化健康秤 Fragment
         * @param mac 设备 mac 地址
         */
        @JvmStatic
        fun newInstance(mac: String): HealthScaleFragment = HealthScaleFragment()
            .apply { arguments = Bundle().apply { this.mac = mac } }
    }

    /** 所有测量值显示 */
    private lateinit var mTvResult: TextView

    /** 只显示测量体重 */
    private lateinit var mTvResultKg: TextView
    private var mPeripheral: BleHealthScalePeripheralImpl? = null
    private val mMac: String by lazy { arguments?.mac ?: "" }

    /**
     * 连接 loading
     */
    private val dialog: Dialog by lazy { DataLoadingDialog(this).apply { setCancelable(true) } }

    private lateinit var viewConnect: View

    /**
     * 结束测量的按钮
     */
    private lateinit var viewCloseMeasure: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = UI {
        frameLayout {
            backgroundColor = Color.WHITE
            onClick { }
            frameLayout {
                scrollView {
                    isFillViewport = true
                    verticalLayout {
                        horizontalPadding = dip(10)
                        topPadding = titleHeight + dip(10)
                        clipToPadding = false
                        addPaddingTopEqualStatusBarHeight()
                        addPaddingBottomEqualNavBarHeight()
                        mTvResultKg = textView {
                            hint = "拖鞋站于秤上测量"
                            textSize = 16f
                        }
                        mTvResult = textView().lparams { topMargin = dip(10) }
                    }
                }.lparams(-1, -1)
                viewCloseMeasure = imageView(android.R.drawable.ic_menu_close_clear_cancel) {
                    padding = dip(10)
                    onClick {
                        viewConnect.visibility = View.VISIBLE
                        mPeripheral?.resultListener(null)
                    }
                }.lparams {
                    gravity = Gravity.END
                    topMargin = getStatusBarHeight() + titleHeight
                }
                viewConnect = verticalLayout {
                    backgroundColor = Color.WHITE
                    gravity = Gravity.CENTER
                    onClick { }
                    styledButton("开始测量") {
                        onClick {
                            if (mPeripheral?.isConnected() == true) {
                                viewConnect.visibility = View.GONE
                                mPeripheral!!.resultListener(scaleResultListener)
                                return@onClick
                            }
                            KKBleCentralImpl.connect(
                                mMac, BleHealthScalePeripheralImpl::class.java, mConnectCallback
                            )
                        }
                    }
                    styledButton("断开连接") {
                        backgroundResource = R.drawable.selector_btn_border
                        setTextColor(
                            ContextCompat.getColorStateList(ctx, R.color.selector_color_primary)
                        )
                        onClick { activity?.onBackPressed() }
                    }
                }
            }

            frameLayout {
                backgroundColor = 0xDDFFFFFF.toInt()
                ViewCompat.setElevation(this, dip(2).toFloat())
                addPaddingTopEqualStatusBarHeight()
                textView("返回") {
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0)
                    horizontalPadding = dip(10)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    textColor = 0xFF333333.toInt()
                    backgroundResource = R.drawable.selector_normal
                    onClick { activity?.onBackPressed() }
                }.lparams(-2, titleHeight)
                textView("智能体重秤") {
                    textSize = 18f
                    textColor = 0xFF333333.toInt()
                }.lparams { gravity = Gravity.CENTER }
            }.lparams(-1, -2)
        }
    }.view

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KKBleCentralImpl.connect(
            mMac,
            BleHealthScalePeripheralImpl::class.java,
            object : IConnectListener<BleHealthScalePeripheralImpl> {
                override fun onConnectFail(
                    peripheral: BleHealthScalePeripheralImpl?,
                    exception: KKPeripheralException
                ) {
                }

                override fun onConnectSuccess(peripheral: BleHealthScalePeripheralImpl) {
                    mPeripheral = peripheral.apply { initUser(createUser()) }
                }

                override fun onDisconnected(
                    isActive: Boolean,
                    peripheral: BleHealthScalePeripheralImpl
                ) {
                }

                override fun onStart() {
                }
            }
        )
        UserDialog(view.context)
            .setOnOkListener { mPeripheral?.initUser(createUser()) }
            .setOnCancelListener { activity?.onBackPressed() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPeripheral?.takeIf { it.isConnected() }?.run { KKBleCentralImpl.disconnect(this) }
    }

    /** 健康秤测量返回监听 */
    private val scaleResultListener: KKHealthScalePeripheral.IResultListener by lazy {
        object : KKHealthScalePeripheral.IResultListener {
            private lateinit var general: LFPeopleGeneral
            override fun onLocked(general: LFPeopleGeneral) {
                this.general = general
                mTvResultKg.text =
                    String.format(Locale.getDefault(), "重量：%.02fkg", general.lfWeightKg)
                mTvResult.text = String.format(
                    Locale.getDefault(), "详细信息：\n%s",
                    JSONObject(GsonUtils.toJson(general)).toString(4)
                )
                viewCloseMeasure.performClick()

                alert {
                    message =
                        general.isCorrectMeasure then {
                            UserData.weight = general.lfWeightKg.toFloat()
                            val weight = String.format("%.02fkg", general.lfWeightKg)
                            SpanUtils()
                                .appendItem("体重        ", weight)
                                .appendItem("身体年龄", general.lfBodyAge)
                                .appendItem("理想体重", general.lfIdealWeightKg)
                                .appendItem("BMI         ", general.lfBMI)
                                .appendItem("BMR        ", general.lfBMR)
                                .appendItem("骨量含量", general.lfBoneKg)
                                .appendItem("脂肪率    ", general.lfBodyfatPercentage)
                                .appendItem("水分率    ", general.lfWaterPercentage)
                                .appendItem("肌肉含量", general.lfMuscleKg)
                                .appendItem("蛋白质率", general.lfProteinPercentage)
                                .appendItem("身体类型", general.lfBodyType)
                                .appendItem("身体得分", general.lfBodyScore)
                                .appendItem("肌肉率    ", general.lfMusclePercentage)
                                .appendItem("体脂量    ", general.lfBodyfatKg)
                                .appendItem("去脂体重", general.lfLoseFatWeightKg)
                                .appendItem("体重控制", general.lfControlWeightKg)
                                .appendItem("骨骼肌率", general.lfBonePercentage)
                                .appendItem("肌肉控制", general.lfBodyMuscleControlKg)
                                .appendItem("皮下脂肪", general.lfVFPercentage)
                                .appendItem("健康水平", general.lfHealthLevel)
                                .appendItem("肥胖等级", general.lfFatLevel)
                                .appendItem("腰臀比    ", general.lfWHR)
                                .appendItem("腰围        ", general.lfWaist)
                                .appendItem("臀围        ", general.lfHipLine)
                                .appendItem("脂肪控制量", general.lfFatControlKg)
                                .appendItem("内脏脂肪等级", general.lfVFAL)
                                .appendItem("健康评估", general.lfHealthReport)
                                .create()
                        } ?: SpanUtils().appendLine(mTvResultKg.text)
                            .setHorizontalAlign(Layout.Alignment.ALIGN_CENTER)
                            .append("当前阻力值过大，请光脚上秤").create()
                    isCancelable = false
                    okButton { }
                }.show()
            }

            private fun SpanUtils.appendItem(
                title: CharSequence, content: Any?
            ): SpanUtils =
                append(title).setForegroundColor(0xFF666666.toInt()).append("    ")
                    .appendLine(
                        (content as? Double)?.let { String.format("%.02f", it) }
                            ?: content.toString()
                    ).setForegroundColor(0xFF333333.toInt())

            override fun onProgress(general: LFPeopleGeneral) {
                if (mTvResultKg.text != null) mTvResultKg.text = null
                if (mTvResult.text != null) mTvResult.text = null
                mTvResultKg.hint =
                    String.format(Locale.getDefault(), "重量：%.02fkg", general.lfWeightKg)
                mTvResult.hint = "详细信息：\n${JSONObject(GsonUtils.toJson(general)).toString(4)}"
            }

            override fun onReport(uuid: String) {
                alert {
                    title = "测量报告生成"
                    message = uuid
                    isCancelable = false
                    cancelButton { }
                    positiveButton("获取报告") {
                        lateinit var getReport: () -> Unit
                        getReport = {
                            HealthScaleApi.getReport(uuid, {
                                alert {
                                    title = "健康秤报告获取失败"
                                    message = it
                                    isCancelable = false
                                    positiveButton("重试") {
                                        getReport.invoke()
                                    }
                                    cancelButton { }
                                }.show()
                            }) {
                                LogUtils.d(it.weight)
                                alert {
                                    title = "健康秤报告"
                                    message = JSONObject(GsonUtils.toJson(it)).toString(4)
                                    isCancelable = false
                                    okButton { }
                                }.show()
                            }
                        }
                        getReport.invoke()
                    }
                }.show()
            }

            override fun onReportFail(
                e: KKPeripheralException,
                reportGenerate: KKHealthScalePeripheral.IReportGenerate
            ) {
                alert {
                    title = "测量报告生成失败"
                    message = "${e.message}\n\n是否重新生成报告"
                    isCancelable = false
                    cancelButton { }
                    positiveButton("重新生成") {
                        reportGenerate.reportGenerate(general, scaleResultListener)
                    }
                }.show()
            }
        }
    }

    private fun createUser() =
        UserModel("${UserData.userId}", UserData.height, UserData.age, UserData.sex)

    /** 设备连接回调 */
    private val mConnectCallback: IConnectListener<BleHealthScalePeripheralImpl> by lazy {
        val stateListener = IStateListener.DialogStateListener(dialog)
        return@lazy object :
            ConnectListener<BleHealthScalePeripheralImpl>(stateListener, {
                mPeripheral = apply {
                    initUser(createUser())
                    viewConnect.visibility = View.GONE
                    resultListener(scaleResultListener)
                }
            }) {
            override fun onDisconnected(
                isActive: Boolean,
                peripheral: BleHealthScalePeripheralImpl
            ) {
                super.onDisconnected(isActive, peripheral)
                mPeripheral = null
                viewConnect.visibility = View.VISIBLE
                mPeripheral?.resultListener(null)
            }

            override fun onConnectFail(
                peripheral: BleHealthScalePeripheralImpl?,
                exception: KKPeripheralException
            ) {
                super.onConnectFail(peripheral, exception)
                mPeripheral = null
                viewConnect.visibility = View.VISIBLE
                mPeripheral?.resultListener(null)
            }
        }
    }
}
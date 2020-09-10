package com.kk.hardware.demo.armlet

import android.content.Context
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.TimeUtils
import com.kk.core.common.extensions.then
import com.kk.hardware.ble.AbstractKKBlePeripheral
import com.kk.hardware.ble.KKBleCentralImpl
import com.kk.hardware.ble.peripheral.BleArmletPeripheralImpl
import com.kk.hardware.ble.peripheral.BleOtaPeripheralImpl
import com.kk.hardware.core.IScanListener
import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.command.*
import com.kk.hardware.core.command.base.AbstractBytesToModel
import com.kk.hardware.core.command.base.BytesToResult
import com.kk.hardware.core.net.DeviceApi
import com.kk.hardware.core.net.upgrade.UpgradeCallback
import com.kk.hardware.core.net.upgrade.UpgradeVersionEntity
import com.kk.hardware.core.peripheral.IWriteListener
import com.kk.hardware.core.peripheral.armlet.IResultListener
import com.kk.hardware.demo.UserData
import com.kk.hardware.demo.ota.UpdateZipActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.okButton

/**
 * 臂带指令返回
 * @author zhangtao
 * @date 2020/6/23 17:22
 */
open class ArmletResultListener(
    private val adapter: MessageAdapter,
    private val writeListener: IWriteListener
) : IResultListener {
    /** 臂带设备 */
    internal var armletPeripheral: BleArmletPeripheralImpl? = null
    override fun onSetUserResult(result: ArmletResultModel) {
        addLogSimple("设置用户信息", result)
    }

    override fun onSetBrightResult(result: ArmletResultModel) {
        addLogSimple("设置亮度", result)
    }

    override fun onSetTimeResult(result: ArmletResultModel) {
        addLogSimple("设置时间", result)
    }

    override fun onSetDeviceIdResult(result: ArmletResultModel) {
        addLogSimple("设置设备 ID", result)
    }

    override fun onJumpTwoPointFourResult(result: ArmletResultModel) {
        addLogSimple("进入 2.4 G 模式", result)
    }

    override fun onInOtaResult(result: ArmletResultModel) {
        addLogSimple("进入 OTA 模式", result)
        adapter.weakRecyclerView.get()?.context?.apply {
            upgrade(this)
        }
    }

    override fun onActionResult(result: ArmletResultModel) {
        addLogSimple("动作命令请求", result)
    }

    override fun onStartTrainResult(result: ArmletResultModel) {
        addLogSimple("开始训练", result)
    }

    override fun onEndTrainResult(result: ArmletResultModel) {
        addLogSimple("结束训练", result)
    }

    override fun onFindDeviceResult(result: ArmletResultModel) {
        addLogSimple("查找设备", result)
    }


    override fun onBindDeviceResult(result: ArmletBindDeviceModel) {
        addLogDetails("绑定设备：${result.isGranted then "成功" ?: "失败"}", result)
    }

    override fun onDeviceVersionResult(result: ArmletDeviceVersionModel) {
        addLogDetails("设备版本号-${result.nordicVersion}", result)
        if (isGetVersionOnly) return
        DeviceApi.upgradeArmlet(result.nordicVersion, object : UpgradeCallback {
            override fun upgrade(entity: UpgradeVersionEntity?) {
                if (entity == null) {
                    adapter.addSimple("版本信息加载", "当前为最新版")
                    return
                }
                this@ArmletResultListener.entity = entity
                adapter.weakRecyclerView.get()?.context?.apply {
                    alert {
                        title = "固件升级"
                        message =
                            "检测到最新版本：${entity.version}\n更新时间：${TimeUtils.millis2String(entity.updateTime)}\n升级内容：\n${entity.detail}"
                        isCancelable = false
                        cancelButton { }
                        okButton {
                            armletPeripheral?.sendCommand(ArmletInOtaCommand(), writeListener)
                        }
                    }.show()
                }
            }

            override fun error(e: KKPeripheralException) {
                adapter.addDetails("版本信息加载：失败", e.message ?: "")
            }
        })
    }

    /**
     * 是否仅得到版本号
     */
    var isGetVersionOnly = true

    override fun onHeartrateResult(result: ArmletHeartrateModel) {
        UserData.sumCalorie = result.sumCalorie
        addLogDetails("心率数据", result)
        adapter.addSimple("心率", "${result.heartBeat}")
        adapter.addSimple("距离", "${result.sumDistance / 10} m")
        adapter.addSimple("步数", "${result.sumStep}")
        adapter.addSimple("卡路里", "${result.sumCalorie} cal")
        adapter.addSimple("配速（秒/公里）", "${result.pace}")
        adapter.addSimple("步频（步/分钟）", "${result.stepFrequency}")
    }

    override fun onInOrOutFactoryResult(result: ArmletInOrOutFactoryMode) {
        addLogDetails("进入退出工厂模式", result)
    }

    override fun onHistorySportDataResult(result: ArmletHistorySportDataModel) {
        if (result.startTime == 0) {
            addLogDetails("历史运动数据-暂无", result)
        } else {
            val size = result.duration / 180 + 1
            addLogDetails("历史运动数据：共${size}段 ", result)
            adapter.addSimple("总卡路里", "${result.sumCalorie} cal")
            adapter.addSimple("总里程", "${result.sumDistance/10} m")
            adapter.addSimple("总步数", "${result.sumStep}")
            adapter.addSimple("持续时间", "${result.duration} s")
            adapter.addSimple("运动类型", "${result.sportType}")
            val command = ArmletHistorySportDataSecondCommand(result.sportType, result.startTime, 1)
            armletPeripheral?.sendCommand(command, writeListener)
        }
    }

    override fun onHistorySportDataSecondResult(result: ArmletHistorySportDataSecondModel) {
        if (result.sportArray.isEmpty()) {
            // 删除当前历史运动数据
            val command =
                ArmletHistorySportDataSecondCommand(result.sportType, result.startTime, 0xFFFF)
            armletPeripheral?.sendCommand(command, writeListener)

            // 循环读取历史记录
            armletPeripheral?.sendCommand(ArmletHistorySportDataCommand(), writeListener)
            return
        }
        addLogDetails("历史运动数据：第${result.nSection}段", result)
        result.sportArray.forEachIndexed { index, data ->
            val title = "第${result.nSection}段${index + 1}节：距离：${data.pace}、步频：${data.stepFrequency}"
            adapter.addDetails(title, GsonUtils.toJson(data))
        }
        val command = ArmletHistorySportDataSecondCommand(
            result.sportType, result.startTime, result.nSection + 1
        )
        armletPeripheral?.sendCommand(command, writeListener)
    }


    override fun onStateResult(result: ArmletStateModel) {
        addLogDetails("设备状态：${result.state}", result)
    }

    override fun onActionResult(result: ArmletActionModel) {
        addLogDetails("动作命令", result)
    }

    private fun addLogSimple(title: CharSequence, result: BytesToResult) {
        adapter.addSimple("$title", if (result.success) "成功" else "失败")
    }

    private fun addLogDetails(title: CharSequence, result: AbstractBytesToModel) {
        adapter.addDetails("$title", GsonUtils.toJson(result))
    }

    private var entity: UpgradeVersionEntity? = null

    /** ota 升级 */
    private fun upgrade(context: Context) {
        KKBleCentralImpl.scan(
            true, object : IScanListener<AbstractKKBlePeripheral> {
                override fun onStart(success: Boolean) {
                }

                override fun onScaning(peripheral: AbstractKKBlePeripheral) {
                    if (peripheral is BleOtaPeripheralImpl) {
                        adapter.addSimple("进入 OTA 升级", entity?.version ?: "版本回退")
                        UpdateZipActivity.startUpdateZip(
                            context, peripheral.getMac(), entity
                        )
                        entity = null
                        KKBleCentralImpl.cancelScan()
                    }
                }

                override fun onEnd() {
                }
            }, BleArmletPeripheralImpl::class.java
        )
    }
}
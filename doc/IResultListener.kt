package com.kk.hardware.core.peripheral.armlet

import com.kk.hardware.core.command.*
import com.kk.hardware.core.command.base.*

/**
 * 臂带指令返回
 * @author zhangtao
 * @date 2020/6/23 17:33
 */
interface IResultListener {

    /**
     * 处理接收指令内容
     * @param bytes 原始指令数据
     */
    fun onResult(bytes: ByteArray) {
        when (bytes.getOrNull(0) ?: return) {
            SetUserCommand.COMMAND -> {
                onSetUserResult(ArmletResultModel(bytes))
            }
            SetBrightCommand.COMMAND -> {
                onSetBrightResult(ArmletResultModel(bytes))
            }
            SetTimeCommand.COMMAND -> {
                onSetTimeResult(ArmletResultModel(bytes))
            }
            SetDeviceIdCommand.COMMAND -> {
                onSetDeviceIdResult(ArmletResultModel(bytes))
            }
            JumpTwoPointFourCommand.COMMAND -> {
                onJumpTwoPointFourResult(ArmletResultModel(bytes))
            }
            InOtaCommand.COMMAND -> {
                onInOtaResult(ArmletResultModel(bytes))
            }
            ActionCommand.COMMAND -> {
                onActionResult(ArmletResultModel(bytes))
            }
            StartTrainCommand.COMMAND -> {
                onStartTrainResult(ArmletResultModel(bytes))
            }
            EndTrainCommand.COMMAND -> {
                onEndTrainResult(ArmletResultModel(bytes))
            }
            FindDeviceCommand.COMMAND -> {
                onFindDeviceResult(ArmletResultModel(bytes))
            }
            // --------------------------true/false 返回值---------------------------
            // ----------------------------------------------------------------------
            // --------------------------拥有具体的返回参数--------------------------
            BindDeviceCommand.COMMAND -> {
                onBindDeviceResult(ArmletBindDeviceModel(bytes))
            }
            ArmletHeartrateCommand.COMMAND -> {
                onHeartrateResult(ArmletHeartrateModel(bytes))
            }
            DeviceVersionCommand.COMMAND -> {
                onDeviceVersionResult(ArmletDeviceVersionModel(bytes))
            }
            InOrOutFactoryCommand.COMMAND -> {
                onInOrOutFactoryResult(ArmletInOrOutFactoryMode(bytes))
            }
            ArmletHistorySportDataCommand.COMMAND -> {
                onHistorySportDataResult(ArmletHistorySportDataModel(bytes))
            }
            ArmletHistorySportDataSecondCommand.COMMAND -> {
                onHistorySportDataSecondResult(ArmletHistorySportDataSecondModel(bytes))
            }
            StateModel.COMMAND -> {
                onStateResult(ArmletStateModel(bytes))
            }
            ActionModel.COMMAND -> {
                onActionResult(ArmletActionModel(bytes))
            }
        }
    }

    /** 配置用户信息返回 */
    fun onSetUserResult(result: ArmletResultModel)

    /** 亮度设置返回 */
    fun onSetBrightResult(result: ArmletResultModel)

    /** 时间设置返回 */
    fun onSetTimeResult(result: ArmletResultModel)

    /** id 设置返回 */
    fun onSetDeviceIdResult(result: ArmletResultModel)

    /** 跳转2.4G模式返回 */
    fun onJumpTwoPointFourResult(result: ArmletResultModel)

    /** 进入 OTA 模式返回 */
    fun onInOtaResult(result: ArmletResultModel)

    /** 动作命令请求返回 */
    fun onActionResult(result: ArmletResultModel)

    /** 开始训练命令返回 */
    fun onStartTrainResult(result: ArmletResultModel)

    /** 结束训练命令返回 */
    fun onEndTrainResult(result: ArmletResultModel)

    /** 查找设备返回 */
    fun onFindDeviceResult(result: ArmletResultModel)

    /**
     * 设备绑定返回
     * @param result 返回内容
     * @see ArmletBindDeviceModel 设备绑定返回
     */
    fun onBindDeviceResult(result: ArmletBindDeviceModel)

    /**
     * 设备固件版本返回
     * @param result 返回内容
     * @see ArmletDeviceVersionModel 设备固件版本返回
     */
    fun onDeviceVersionResult(result: ArmletDeviceVersionModel)

    /**
     * 心率消耗返回
     * @param result 返回内容
     * @see ArmletHeartrateModel 心率消耗返回
     */
    fun onHeartrateResult(result: ArmletHeartrateModel)

    /**
     * 进入/退出 工厂模式返回
     * @param result 返回内容
     * @see ArmletInOrOutFactoryMode 进入/退出 工厂模式返回
     */
    fun onInOrOutFactoryResult(result: ArmletInOrOutFactoryMode)

    /**
     * 第一段运动数据返回
     * @param result 返回内容
     * @see ArmletHistorySportDataModel 第一段运动数据返回
     */
    fun onHistorySportDataResult(result: ArmletHistorySportDataModel)

    /**
     * 第二段运动数据返回
     * @param result 返回内容
     * @see ArmletHistorySportDataSecondModel 第二段运动数据返回
     */
    fun onHistorySportDataSecondResult(result: ArmletHistorySportDataSecondModel)

    /**
     * 设备状态返回
     * @param result 返回内容
     * @see ArmletStateModel 设备状态返回
     */
    fun onStateResult(result: ArmletStateModel)

    /**
     * 动作命令请求返回
     * @param result 返回内容
     * @see ArmletActionModel 动作命令请求返回
     */
    fun onActionResult(result: ArmletActionModel)
}
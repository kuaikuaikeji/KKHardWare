package com.kk.hardware.core.command

import com.kk.hardware.core.command.base.*
import com.kk.hardware.core.command.other.ArmletHistorySportData
import com.kk.hardware.core.command.other.hexToTen
import com.kk.hardware.core.command.other.toInt

/**
 * 臂带指令
 * @author zhangtao
 * @date 2020/6/11 11:16
 */

//----------------------------------请求指令--------------------------------------
/** 配置用户信息 */
class ArmletSetUserCommand(
    userId: Int, height: Int, weight: Float, age: Int, sex: Int, staticHeart: Int
) : SetUserCommand(userId, height, weight, age, sex, staticHeart)

/** 设置最大亮度 */
class ArmletSetBrightCommand(setFirst: Int, setSecond: Int) : SetBrightCommand(setFirst, setSecond)

/** 设置时间 */
class ArmletSetTimeCommand(userId: Int, time: Int = (System.currentTimeMillis() / 1000).toInt()) :
    SetTimeCommand(userId, time)

/** 设置设备 id */
class ArmletSetDeviceIdCommand(deviceId: Int) : SetDeviceIdCommand(deviceId)

/** 跳转2.4G模式 */
class ArmletJumpTwoPointFourCommand : JumpTwoPointFourCommand()

/**
 * 进入/退出工厂模式
 * @see ArmletInOrOutFactoryMode
 */
class ArmletInOrOutFactoryCommand : InOrOutFactoryCommand()

/** 进入 OTA 模式 */
class ArmletInOtaCommand : InOtaCommand()

/** 动作命令请求 */
class ArmletActionCommand(
    actionCode: Int, actionSign: Int, sumNumber: Int, finishNumber: Int, period: Int,
    roll1_timedif: Int, roll1_valuedif: Int, picth1_timedif: Int, picth1_valuedif: Int,
    yaw1_valuedif: Int, yaw1_valuedif2: Int, roll2_timedif: Int, roll2_valuedif: Int,
    picth2_timedif: Int, picth2_valuedif: Int, yaw2_valuedif: Int, yaw2_valuedif2: Int
) : ActionCommand(
    actionCode, actionSign, sumNumber, finishNumber, period,
    roll1_timedif, roll1_valuedif, picth1_timedif, picth1_valuedif,
    yaw1_valuedif, yaw1_valuedif2, roll2_timedif, roll2_valuedif,
    picth2_timedif, picth2_valuedif, yaw2_valuedif, yaw2_valuedif2
)

/**
 * 设备绑定
 * @see ArmletBindDeviceModel 绑定设备返回
 */
class ArmletBindDeviceCommand : BindDeviceCommand(2)

/**
 * 心率消耗数据请求 [ bytes0 ] 0x16
 * @param sportType [ bytes1 ] 运动类型
 * @param consume [ Bytes3-bytes6] 消耗
 * @see ArmletHeartrateModel 心率返回
 */
class ArmletHeartrateCommand(private val sportType: Int, private val consume: Int) :
    AbstractModelToBytes() {
    companion object {
        const val COMMAND = 0x16.toByte()
    }

    override val bytes: ByteArray = byteArrayOf(
        COMMAND,
        sportType.toByte(),
        (consume shr 24 and 0xFF).toByte(),
        (consume shr 16 and 0xFF).toByte(),
        (consume shr 8 and 0xFF).toByte(),
        (consume and 0xFF).toByte()
    )
}

/**
 * 设备固件版本请求
 * @see ArmletDeviceVersionModel 设备固件版本返回
 */
class ArmletVersionCommand : DeviceVersionCommand()

/**
 * 第一阶段：获取一段运动数据的基本信息
 * 历史运动数据请求0x1E
 * 填充0
 * @see ArmletHistorySportDataModel 数据返回
 */
class ArmletHistorySportDataCommand : AbstractModelToBytes() {
    companion object {
        const val COMMAND = 0x1E.toByte()
    }

    override val bytes: ByteArray = byteArrayOf(COMMAND)
}

/**
 * 第二阶段：获取该段运动数据的心率和步频数据
 * 历史运动数据0x1F
 * 说明
 * 运动类型：1个字节
 * 开始时间：4个字节  UTC时间戳 代表这段运动开始的时间
 * 第N小段：2个字节
 * iOS设备——每小段为11*10秒的数据，总段数由持续时间换算。
 * Android设备——每小段为18*10秒的数据，总段数由持续时间换算。
 * 当APP下发0xFFFF时，代表完成这次运动所有数据传输。
 * 单段数据长度：1个字节  iOS固定为143；Android固定为234
 * 注：当无历史数据时，不会进行第二阶段请求。
 * 历史运动数据请求0x1F
 * @param sportType Byte2 运动类型
 * @param startTime Byte3~6 开始时间
 * @param nSection Byte7 Byte8 第N小段
 * @param singleDataLength Byte9 单段数据长度
 * Byte10~20 填充0
 * @see ArmletHistorySportDataSecondModel 数据返回
 */
class ArmletHistorySportDataSecondCommand(
    private val sportType: Int,
    private val startTime: Int,
    private val nSection: Int,
    private val singleDataLength: Int = 234
) : AbstractModelToBytes() {
    companion object {
        const val COMMAND = 0x1F.toByte()
    }

    override val bytes: ByteArray = byteArrayOf(
        COMMAND,
        sportType.toByte(),
        (startTime shr 24 and 0xFF).toByte(),
        (startTime shr 16 and 0xFF).toByte(),
        (startTime shr 8 and 0xFF).toByte(),
        (startTime and 0xFF).toByte(),
        (nSection shr 8 and 0xFF).toByte(),
        (nSection and 0xFF).toByte(),
        singleDataLength.toByte()
    )
}

/** 开始训练 */
class ArmletStartTrainCommand(
    sportType: Int, startTime: Int, beatSign: Int, stepRate: Int, dataPeriod: Int
) : StartTrainCommand(sportType, startTime, beatSign, stepRate, dataPeriod)

/** 结束训练 */
class ArmletEndTrainCommand(sportType: Int, endTime: Int) : EndTrainCommand(sportType, endTime)

/** 查找设备 */
class ArmletFindDeviceCommand : FindDeviceCommand()

//class ArmletCommand(override val bytes: ByteArray) : AbstractModelToBytes()
//----------------------------------返回指令--------------------------------------
/** 指令处理结果返回 */
class ArmletResultModel(bytes: ByteArray) : BytesToResult(bytes)

/**
 *  绑定设备返回
 *  @see ArmletBindDeviceCommand 绑定设备指令
 */
class ArmletBindDeviceModel(bytes: ByteArray) : BindDeviceModel(bytes)

/**
 * 设备固件版本返回
 * @see ArmletVersionCommand 设备固件版本请求指令
 */
class ArmletDeviceVersionModel(bytes: ByteArray) : DeviceVersionModel(bytes)

/**
 * 心率消耗数据返回 0x16
 * 说明
 * 运动类型：1个字节，臂带所处的运动类型
 * 心率：1个字节
 * 总步数：4个字节 当日总步数
 * 总卡路里：4个字节 当日总卡路里  单位：cal 卡
 * 总里程：4个字节 当日总行进里程  单位：dm分米
 * 配速+步频：3个字节，前12bit为配速 单位：秒；随后9bit为步频，每分钟的步数；最后3bit无效。
 * 运动时间：4个字节，从运动开始到现在的时长  单位：s秒
 * 注：若APP判断返回的运动类型不一致时，需重新下发开始运动命令。
 * [ bytes1 ] 运动类型
 * [ bytes2 ] 心率
 * [ bytes3-bytes6 ] 总步数
 * [ bytes7-bytes10 ] 总卡路里
 * [ bytes11-bytes14 ] 总里程
 * [ bytes15-bytes17 ] 配速+步频
 * [ bytes18-bytes21 ] 运动时间
 *
 * @see ArmletHeartrateCommand 获取心率指令
 */
class ArmletHeartrateModel(bytes: ByteArray) : AbstractBytesToModel(bytes) {
    /** 运动类型 */
    val sportType = bytes[1].toInt()

    /** 心率 */
    val heartBeat = bytes[2].toInt()

    /** 总步数 */
    val sumStep = bytes.toInt(3)

    /** 总卡路里 */
    val sumCalorie = bytes.toInt(7)

    /** 总里程 dm 分米*/
    val sumDistance = bytes.toInt(11)

    /** 配速 秒*/
    val pace = String.format("%02X%02X", bytes[15], (bytes[16].toInt() and 0xF0) shr 4).hexToTen()

    /** 步频 每分钟步数 */
    val stepFrequency =
        String.format("%02X%02X", (bytes[16].toInt() and 0x0F).toByte(), bytes[17]).hexToTen()

    /** 运动时间 秒 */
    val sportTime = bytes.toInt(18)
}

/**
 * 设备进入/退出工厂模式返回0x0
 * 配置结果：1个字节：
 * 成功进入工厂模式：0x01，成功退出工厂模式0x00;
 */
class ArmletInOrOutFactoryMode(bytes: ByteArray) : AbstractBytesToModel(bytes) {
    /**
     * true：成功进入工厂模式
     * false：成功退出工厂模式
     */
    val success = bytes.getOrNull(1)?.toInt() == 1
}

/**
 * 第一阶段：获取一段运动数据的基本信息
 * 历史运动数据返回0x1E
 * 说明
 * 运动类型：1个字节 ，当为0x00时说明设备繁忙，此时APP应停止同步数据。
 * 开始时间：4个字节  UTC时间戳 代表这段运动开始的时间
 * 持续时间：4个字节  UTC时间戳 代表这段运动持续的时间
 * 总步数：4个字节 当日总步数
 * 总卡路里：4个字节 当日总卡路里  单位：cal 卡
 * 总里程：4个字节 当日总行进里程  单位：dm分米
 * 注：当以上内容全为0xFF时，为无历史数据。
 * Byte2 运动类型
 * Byte3~6 开始时间
 * Byte7~10 持续时间
 * Byte11~14 总步数
 * Byte15~18 总卡路里
 * Byte19~22 总里程
 *
 * @see ArmletHistorySportDataCommand 获取指令
 */
class ArmletHistorySportDataModel(bytes: ByteArray) : AbstractBytesToModel(bytes) {
    /** 运动类型 */
    val sportType: Int

    /** 开始时间 为 0 时没数据 */
    val startTime: Int

    /** 持续时间 */
    val duration: Int

    /** 总步数 */
    val sumStep: Int

    /** 总卡路里 */
    val sumCalorie: Int

    /** 总里程 */
    val sumDistance: Int

    init {
        // 是否有历史运动数据
        if (bytes[2] != 0xFF.toByte()) {
            sportType = (bytes.getOrNull(1)?.toInt() ?: 0) and 0xFF
            startTime = bytes.toInt(2)
            duration = bytes.toInt(6)
            sumStep = bytes.toInt(10)
            sumCalorie = bytes.toInt(14)
            sumDistance = bytes.toInt(18)
        } else {
            sportType = 0
            startTime = 0
            duration = 0
            sumStep = 0
            sumCalorie = 0
            sumDistance = 0
        }
    }
}

/**
 * 第二阶段：获取该段运动数据的心率和步频数据
 * 历史运动数据请求返回0x1F
 * 说明
 * 运动类型：1个字节 ，当为0x00时说明设备繁忙，此时APP应停止同步数据。
 * 开始时间：4个字节  UTC时间戳 代表这段运动开始的时间
 * 第N小段：2个字节
 * 心率+配速+步频：
 * iOS设备——143个字节 每13字节一组，单包数据共11组；
 * Android设备——234个字节  每13字节一组，单包数据共18组；
 * 一组数据包括：10秒内产生的，10个单字节心率值，配速12bit（∈[0,4095]秒钟），步频9bit（∈[0,511]），臂带每10秒保存一组数据，最后3bit无效。配速单位为每公里所耗费的时间；步频代表每分钟行进的步数
 * 注：如果为最后一段，不足数据填充0xFF。
 * Byte2 运动类型
 * Byte3~6 开始时间
 * Byte7-Byte8 第N小段
 * iOS：Byte9~151 Android：Byte9~242 心率+配速+步频
 *
 * @see ArmletHistorySportDataSecondCommand 获取指令
 */
class ArmletHistorySportDataSecondModel(bytes: ByteArray) : AbstractBytesToModel(bytes) {
    /** 运动类型 */
    val sportType = bytes[1].toInt()

    /** 开始时间 */
    val startTime = bytes.toInt(2)

    /** 第 N 小段 */
    val nSection = String.format("%02X%02X", bytes[6], bytes[7]).hexToTen()

    /** 历史运动数据 */
    val sportArray = ArmletHistorySportData.decode(bytes)
}

/** 设备状态返回 */
class ArmletStateModel(bytes: ByteArray) : StateModel(bytes)

/** 动作命令请求返回 */
class ArmletActionModel(bytes: ByteArray) : ActionModel(bytes)

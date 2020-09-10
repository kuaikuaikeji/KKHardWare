package com.kk.hardware.core.peripheral

import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.command.base.DeviceElectricQuantityModel

/**
 * 读取电量回调
 * @author zhangtao
 * @date 2020/6/19 10:30
 */
interface IReadElectricQuantityListener {
    /**
     * 电量获取成功
     * @param model 电量信息
     */
    fun onSuccess(model: DeviceElectricQuantityModel)

    /**
     * 电量获取失败
     * @param e 失败信息
     */
    fun onFail(e: KKPeripheralException)
}
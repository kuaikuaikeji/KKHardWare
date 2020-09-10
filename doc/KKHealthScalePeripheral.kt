package com.kk.hardware.core.peripheral

import com.kk.hardware.core.KKPeripheralException
import com.kk.hardware.core.command.other.LFPeopleGeneral
import com.kk.hardware.core.command.UserModel

/**
 * 健康秤指令相关
 * @author zhangtao
 * @date 2020/6/9 18:47
 */
interface KKHealthScalePeripheral : IPeripheral {

    /**
     * 测量回调
     * @param listener 回调
     * @see IResultListener
     */
    fun resultListener(listener: IResultListener?)

    /**
     *初始化测量用户
     * @param userModel 用户数据
     */
    fun initUser(userModel: UserModel)

    /**
     * 健康秤测量，报告生成监听
     */
    interface IResultListener : IScaleListener, IReportListener

    /**
     * 测量结果监听
     */
    interface IScaleListener {
        /**
         * 测量锁定回调
         * @param general 测量数据
         */
        fun onLocked(general: LFPeopleGeneral)

        /**
         * 测量过程回调
         * @param general 测量数据
         */
        fun onProgress(general: LFPeopleGeneral)
    }

    /**
     * 健康秤报告返回
     */
    interface IReportListener {
        /**
         * 报告返回
         * @param uuid 报告 uuid，通过 uuid 获取报告详情
         */
        fun onReport(uuid: String)

        /**
         * 报告生成失败
         * @param e 错误说明
         * @param reportGenerate 报告重新生成接口
         */
        fun onReportFail(e: KKPeripheralException, reportGenerate: IReportGenerate)
    }

    /**
     * 健康秤报告生成
     */
    interface IReportGenerate {
        /**
         * 生成报告
         * @param general 测量数据
         * @param listener 生成报告回调
         */
        fun reportGenerate(general: LFPeopleGeneral, listener: IReportListener)
    }
}
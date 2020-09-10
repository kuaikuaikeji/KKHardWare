package com.kk.hardware.demo

import com.blankj.utilcode.util.SPUtils

/**
 * 用户数据
 * @author zhangtao
 * @date 2020/8/5 18:59
 */
object UserData {

    init {
        print("UserData init")
    }

    const val userId = 111

    var name: String? = SPUtils.getInstance().getString("name")
        set(value) {
            field = value
            SPUtils.getInstance().put("name", field)
        }

    var height = SPUtils.getInstance().getInt("height", 0)
        set(value) {
            field = value
            SPUtils.getInstance().put("height", field)
        }

    var age = SPUtils.getInstance().getInt("age", 0)
        set(value) {
            field = value
            SPUtils.getInstance().put("age", field)
        }

    var sex = SPUtils.getInstance().getInt("sex", 1)
        set(value) {
            field = value
            SPUtils.getInstance().put("sex", field)
        }

    var staticHeart = SPUtils.getInstance().getInt("staticHeart", 0)
        set(value) {
            field = value
            SPUtils.getInstance().put("staticHeart", field)
        }

    /**
     * 上次测量体重
     */
    var weight = SPUtils.getInstance().getFloat("weight", 0f)
        set(value) {
            field = value
            SPUtils.getInstance().put("weight", field)
        }

    /**
     * 上次绑定时间
     */
    var lastBindTime = SPUtils.getInstance().getLong("lastBindTime", 0L)
        set(value) {
            field = value
            SPUtils.getInstance().put("lastBindTime", field)
        }

    /**
     * 上次总消耗
     */
    var sumCalorie = SPUtils.getInstance().getInt("sumCalorie", 0)
        set(value) {
            field = value
            SPUtils.getInstance().put("sumCalorie", field)
        }
}
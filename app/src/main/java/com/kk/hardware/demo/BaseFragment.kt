package com.kk.hardware.demo

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * 基类 Fragment
 * @author zhangtao
 * @date 2020/6/28 16:56
 */
open class BaseFragment : Fragment() {
    /** 设备 mac 地址 */
    protected var Bundle.mac: String?
        get() = getString("mac")
        set(value) {
            putString("mac", value)
        }
}
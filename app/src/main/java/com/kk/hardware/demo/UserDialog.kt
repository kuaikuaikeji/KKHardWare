package com.kk.hardware.demo

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.kk.core.common.extensions.isNull
import com.kk.core.common.extensions.isNullOrEmptyByTrim
import com.kk.core.common.extensions.then
import kotlinx.android.synthetic.main.dialog_user_edit.view.*

/**
 * 用户基本信息
 * @author zhangtao
 * @date 2020/8/5 17:38
 */
class UserDialog(context: Context) : AlertDialog(context) {
    private val mView: View

    init {
        setCancelable(false)
        setView(View.inflate(context, R.layout.dialog_user_edit, null).apply {
            mView = this
            et_name.setText(UserData.name.isNullOrEmptyByTrim { })
            et_height.setText(UserData.height.takeIf { it > 0 }?.toString())
            tv_weight.text = String.format("%.02f", UserData.weight)
            et_age.setText(UserData.age.takeIf { it > 0 }?.toString())
            rg_sex.check((UserData.sex == 1) then R.id.rb_sexMan ?: R.id.rb_sexWoman)
            et_staticHeart.setText(UserData.staticHeart.takeIf { it > 0 }?.toString())
            tv_lastBindTime.text = TimeUtils.millis2String(UserData.lastBindTime)
            tv_sumCalorie.text = "${UserData.sumCalorie}"
        })
    }

    fun setOnCancelListener(listener: () -> Unit): UserDialog = apply {
        mView.btn_cancel.setOnClickListener {
            dismiss()
            listener.invoke()
        }
    }

    fun setOnOkListener(listener: () -> Unit): UserDialog = apply {
        mView.apply {
            btn_ok.setOnClickListener {
                UserData.name = (et_name.text.isNullOrEmptyByTrim {
                    ToastUtils.showShort("请输入正确姓名")
                } ?: return@setOnClickListener).toString()
                UserData.height =
                    et_height.text?.toString()?.toIntOrNull()?.takeIf { it > 0 }.isNull {
                        ToastUtils.showShort("请输入正确身高")
                    } ?: return@setOnClickListener
                UserData.age =
                    et_age.text?.toString()?.toIntOrNull()?.takeIf { it > 0 }.isNull {
                        ToastUtils.showShort("请输入正确年龄")
                    } ?: return@setOnClickListener
                UserData.sex = rb_sexMan.isChecked then 1 ?: 0
                UserData.staticHeart =
                    et_staticHeart.text?.toString()?.toIntOrNull()?.takeIf { it > 0 }.isNull {
                        ToastUtils.showShort("请输入正确静态心率")
                    } ?: return@setOnClickListener
                dismiss()
                listener.invoke()
            }
        }
    }
}

package com.kk.hardware.demo.armlet

import com.blankj.utilcode.util.JsonUtils
import com.blankj.utilcode.util.SpanUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.kk.hardware.demo.R
import kotlinx.android.synthetic.main.item_msg_armlet.view.*
import org.jetbrains.anko.alert

/**
 * 臂带指令记录
 * @author zhangtao
 * @date 2020/7/9 11:07
 */
class MessageAdapter : BaseQuickAdapter<CharSequence, BaseViewHolder>(R.layout.item_msg_armlet) {
    init {
        setOnItemClickListener { _, _, position ->
            val items = getItem(position).split("\n")
            val details = items.getOrNull(1)?.let { JsonUtils.formatJson(it) }
            val title = if (details == null) "详细信息" else items[0]
            val message = details ?: items[0]
            context.alert {
                this.title = title
                this.message = message
            }.show()
        }
    }

    internal fun addSimple(title: CharSequence, message: CharSequence) {
        addData(0, "$title：$message")
        weakRecyclerView.get()?.smoothScrollToPosition(0)
    }

    internal fun addDetails(title: CharSequence, message: CharSequence) {
        val cs = SpanUtils().appendLine(title)
            .append(message).setForegroundColor(0xFF999999.toInt())
            .setFontSize(12, true)
            .create()
        addData(0, cs)
        weakRecyclerView.get()?.smoothScrollToPosition(0)
    }

    override fun convert(helper: BaseViewHolder, item: CharSequence) {
        helper.itemView.tv_msg.text = item
    }
}
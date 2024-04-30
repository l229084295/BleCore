package com.bhm.demo.ui

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import androidx.annotation.ColorInt
import androidx.lifecycle.lifecycleScope
import com.bhm.ble.utils.BleLogger
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.constants.LOCATION_PERMISSION
import com.bhm.demo.databinding.ActivityOpenDoorBinding
import com.bhm.demo.vm.OpenDoorVewModel
import com.blankj.utilcode.util.SpanUtils
import com.blankj.utilcode.util.TimeUtils
import kotlinx.coroutines.launch
import org.angmarch.views.SpinnerTextFormatter
import java.util.Date


class OpenDoorActivity : BaseActivity<OpenDoorVewModel, ActivityOpenDoorBinding>() {

    private val doorList = arrayListOf(
        Door("21:DC:AA:00:94:69", "四单元"),
        Door("FE:19:EC:00:14:07", "小区门"),
    )

    private var currentDoor = doorList[0]

    override fun createViewModel() = OpenDoorVewModel(application)


    override fun initData() {
        viewModel.initBle()
        viewBinding.apply {
            val textFormat1 = MySpinnerTextFormat()
            doors.setSpinnerTextFormatter(textFormat1)
            doors.setSelectedTextFormatter(textFormat1)
            doors.attachDataSource(doorList)
            doors.setOnSpinnerItemSelectedListener { _, _, position, _ ->
                currentDoor = doorList[position]
            }

            openDoor.setOnClickListener {
                requestPermission(
                    LOCATION_PERMISSION,
                    {
                        viewModel.connect(currentDoor.mac)
                    }, {
                        BleLogger.w("缺少定位权限")
                    }
                )
            }
        }
    }

    override fun initEvent() {
        lifecycleScope.launch {
            //连接设备后 刷新列表
            viewModel.messageStateFlow.collect {
                addMessage(it.msg, it.color, it.textSize)
            }
        }
    }

    private fun addMessage(message: String) {
        addMessage(message, resources.getColor(R.color.colorPrimary), 14)
    }

    private fun addMessage(message: String, color: Int) {
        addMessage(message, color, 14)
    }

    private fun addMessage(message: String, color: Int, textSize: Int) {
        viewBinding.apply {
            info.append(getSpan(TimeUtils.date2String(Date(), "yyyy-dd-MM HH:mm:ss："), Color.parseColor("#333333"), 12))
            info.append("\n")
            info.append(getSpan(message, color, textSize))
            info.append("\n")
        }
    }

    private fun getSpan(message: String, @ColorInt color: Int, size: Int): SpannableStringBuilder? {
        val s = SpanUtils()
        s.append(message).setFontSize(size, true).setSpans(ForegroundAlphaColorSpan(color))
        return s.create()
    }

    internal class ForegroundAlphaColorSpan(@field:ColorInt var mColor: Int) : CharacterStyle(),
        UpdateAppearance {
        fun setAlpha(alpha: Int) {
            mColor = Color.argb(
                alpha, Color.red(mColor), Color.green(mColor), Color.blue(
                    mColor
                )
            )
        }

        override fun updateDrawState(tp: TextPaint) {
            tp.color = mColor
        }
    }

    data class Door(
        val mac: String = "",
        val name: String = ""
    )

    class MySpinnerTextFormat : SpinnerTextFormatter<Any> {
        override fun format(item: Any?): Spannable {
            return when (item) {
                is Door -> SpannableString(item.name)

                is Int ->
                    SpannableString(item.toString())

                is String ->
                    SpannableString(item)

                else -> {
                    SpannableString(item?.toString() ?: "")
                }
            }
        }
    }
}
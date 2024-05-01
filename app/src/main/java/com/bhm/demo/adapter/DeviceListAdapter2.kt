package com.bhm.demo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bhm.ble.BleManager
import com.bhm.ble.device.BleDevice
import com.bhm.demo.R
import com.bhm.demo.databinding.LayoutRecyclerItemBinding
import com.bhm.demo.databinding.LayoutScanItemBinding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder


/**
 * 设备列表
 *
 * @author Buhuiming
 * @date 2023年05月18日 11时06分
 */
class DeviceListAdapter2(
    data: MutableList<BleDevice>?
) : BaseQuickAdapter<BleDevice, DeviceListAdapter2.VH>(0, data) {

    class VH(
        parent: ViewGroup,
        val binding: LayoutScanItemBinding = LayoutScanItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : BaseViewHolder(binding.root)

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun convert(holder: VH, item: BleDevice) {
        holder.binding.tvName.text = buildString {
            append(item.deviceName)
            append(", ")
            append(item.deviceAddress)
        }
//        holder.binding.btnRssi.text = "${item.rssi ?: 0}"
        val rssi = item.rssi ?: 0
        when {
            rssi >= -65 -> {
                holder.binding.ivRssi.setImageResource(R.drawable.adddevice_device_signal_four_icon)
            }

            rssi >= -75 -> {
                holder.binding.ivRssi.setImageResource(R.drawable.adddevice_device_signal_three_icon)
            }

            rssi >= -85 -> {
                holder.binding.ivRssi.setImageResource(R.drawable.adddevice_device_signal_two_icon)
            }

            else -> {
                holder.binding.ivRssi.setImageResource(R.drawable.adddevice_device_signal_one_icon)
            }
        }
        holder.binding.btnAdd.setOnClickListener {
            onAddClick?.invoke(item)
        }
    }

    private var onAddClick: ((BleDevice) -> Unit)? = null
    fun setOnAddClick(onAddClick: ((BleDevice) -> Unit)?) {
        this.onAddClick = onAddClick
    }
}
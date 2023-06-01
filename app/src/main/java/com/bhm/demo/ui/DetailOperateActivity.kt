/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.ui

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.ble.BleManager
import com.bhm.ble.data.BleDevice
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.adapter.DetailsExpandAdapter
import com.bhm.demo.databinding.ActivityDetailBinding
import com.bhm.demo.entity.CharacteristicNode
import com.bhm.demo.entity.OperateType
import com.bhm.demo.entity.ServiceNode
import com.bhm.demo.vm.DetailViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.entity.MessageEvent
import com.chad.library.adapter.base.entity.node.BaseNode


/**
 * 服务，特征
 *
 * @author Buhuiming
 * @date 2023年06月01日 09时17分
 */
class DetailOperateActivity : BaseActivity<DetailViewModel, ActivityDetailBinding>(){

    override fun createViewModel() = DetailViewModel(application)

    private var bleDevice: BleDevice? = null

    private var expandAdapter: DetailsExpandAdapter? = null

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        bleDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("data", BleDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")
        }

        if (bleDevice == null) {
            finish()
            return
        }
        viewBinding.tvName.text = buildString {
            append("设备广播名：")
            append(bleDevice?.deviceName)
            append("\r\n")
            append("地址：${bleDevice?.deviceAddress}")
        }
        initList()
    }

    private fun initList() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.recyclerView.layoutManager = layoutManager
        viewBinding.recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        expandAdapter = DetailsExpandAdapter(viewModel.getListData(bleDevice!!)) {
                _, operateType, isChecked, node ->

            val msg: String = when (operateType) {
                is OperateType.Write -> {
                    if (isChecked) {
                        "写： ${node.characteristicName}"
                    } else {
                        "取消写： ${node.characteristicName}"
                    }
                }
                is OperateType.Read -> {
                    if (isChecked) {
                        "读： ${node.characteristicName}"
                    } else {
                        "取消读： ${node.characteristicName}"
                    }
                }
                is OperateType.Notify -> {
                    if (isChecked) {
                        "Notify： ${node.characteristicName}"
                    } else {
                        "取消Notify： ${node.characteristicName}"
                    }
                }
                is OperateType.Indicate -> {
                    if (isChecked) {
                        "Indicate： ${node.characteristicName}"
                    } else {
                        "取消Indicate： ${node.characteristicName}"
                    }
                }
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        viewBinding.recyclerView.adapter = expandAdapter
        expandAdapter?.expand(0)
    }

    /**
     * 接收到断开通知
     */
    override fun onMessageEvent(event: MessageEvent?) {
        super.onMessageEvent(event)
        event?.let {
            val device = event.data as BleDevice
            if (bleDevice == device) {
                finish()
            }
        }
    }
}
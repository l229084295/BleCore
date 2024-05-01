package com.bhm.demo.ui

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.adapter.DeviceListAdapter2
import com.bhm.demo.databinding.ActivityBleScanBinding
import com.bhm.demo.databinding.EditTextLayoutBinding
import com.bhm.demo.storage.entity.Door
import com.bhm.demo.vm.BleScanViewModel
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class BleScanActivity : BaseActivity<BleScanViewModel, ActivityBleScanBinding>() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BleScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    private var listAdapter: DeviceListAdapter2? = null
    private val editBinding by lazy { EditTextLayoutBinding.inflate(layoutInflater) }

    override fun createViewModel() = BleScanViewModel(application)

    override fun initData() {
        super.initData()
        initList()
        viewBinding.apply {
            toolbar.setNavigationOnClickListener {
                finish()
            }
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.refresh -> {
                        viewModel.stopScan()
                        listAdapter?.notifyItemRangeRemoved(0, viewModel.listDRData.size)
                        viewModel.listDRData.clear()
                        viewModel.startScan(this@BleScanActivity)
                    }
                }
                return@setOnMenuItemClickListener true
            }
            toolbar.inflateMenu(R.menu.scan_toolbar_menu)
        }

        listAdapter?.setOnAddClick {
            AlertDialog.Builder(this)
                .setTitle("设置备注")
                .setCancelable(true)
                .setView(editBinding.root)
                .setPositiveButton("确定") { _, _ ->
                    viewModel.addLocalDoor(
                        Door(
                            mac = it.deviceAddress ?: "",
                            name = editBinding.etEditText.text.toString(),
                            deviceName = it.deviceName ?: ""
                        )
                    )
                }
                .setNegativeButton("取消") { _, _ ->
                }.create().show()
        }
    }

    override fun initEvent() {
        super.initEvent()
        lifecycleScope.launch {
            //添加扫描到的设备 刷新列表
            viewModel.listDRStateFlow.collect {
                if (it.deviceName != null && it.deviceAddress != null) {
                    val position = (listAdapter?.itemCount ?: 1) - 1
                    listAdapter?.notifyItemInserted(position)
                    viewBinding.rec.smoothScrollToPosition(position)
                }
            }
        }
        lifecycleScope.launch {
            //连接设备后 刷新列表
            viewModel.refreshStateFlow.collect {
                delay(300)
                dismissLoading()
                if (it?.bleDevice == null) {
                    listAdapter?.notifyDataSetChanged()
                    return@collect
                }
                it.bleDevice.let { bleDevice ->
                    val position = listAdapter?.data?.indexOf(bleDevice) ?: -1
                    if (position >= 0) {
                        listAdapter?.notifyItemChanged(position)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.messageStateFlow.collect{
                ToastUtils.showShort(it)
            }
        }
    }

    private fun initList() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.rec.setHasFixedSize(true)
        viewBinding.rec.layoutManager = layoutManager
        viewBinding.rec.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        //解决RecyclerView局部刷新时闪烁
        (viewBinding.rec.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        listAdapter = DeviceListAdapter2(viewModel.listDRData)
        viewBinding.rec.adapter = listAdapter
    }

    override fun onResume() {
        super.onResume()
        viewModel.startScan(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopScan()
    }

}
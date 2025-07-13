package com.bhm.demo.vm

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.graphics.Color
import android.util.Log
import android.util.SparseArray
import androidx.lifecycle.viewModelScope
import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.Constants
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import com.bhm.demo.R
import com.bhm.demo.storage.entity.Door
import com.bhm.demo.utils.ByteUtils
import com.bhm.support.sdk.common.BaseViewModel
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class OpenDoorVewModel(val app: Application) : MyBaseViewModel(app) {

    private val messageMutableStateFlow = MutableStateFlow(
        Message("")
    )
    private val doorMutableStateFlow = MutableStateFlow(
        listOf<Door>()
    )

    val doorStateFlow: StateFlow<List<Door>> = doorMutableStateFlow

    val messageStateFlow: StateFlow<Message> = messageMutableStateFlow

    private val deleteMutableStateFlow = MutableStateFlow(
        0
    )
    val deleteStateFlow = deleteMutableStateFlow

    private var data = byteArrayOf()
    private var currentDeviceName = ""

    /**
     * 初始化蓝牙组件
     */
    fun initBle() {
        BleManager.get().init(
            app,
            BleOptions.Builder()
                .setScanMillisTimeOut(5000)
                .setConnectMillisTimeOut(5000)
                //一般不推荐autoSetMtu，因为如果设置的等待时间会影响其他操作
//                .setMtu(100, true)
                .setMaxConnectNum(2)
                .setConnectRetryCountAndInterval(2, 1000)
                .build()
        )
        BleManager.get().registerBluetoothStateReceiver {
            onStateOff {
                messageMutableStateFlow.value = Message("初始化...")
            }
        }
    }

    fun getLocalDoors() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doors = dbRepository.DoorDao().getDoors()
                doorMutableStateFlow.value = doors
            } catch (_: Exception) {

            }
        }
    }

    fun deleteDoor(door: Door) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dbRepository.DoorDao().delete(door)
                deleteStateFlow.value = 1
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 开始连接
     */
    fun connect(door: Door) {
        currentDeviceName = door.deviceName
        connect(BleManager.get().buildBleDeviceByDeviceAddress(door.mac))
    }

    /**
     * 开始连接
     */
    fun connect(bleDevice: BleDevice?) {
        bleDevice?.let { device ->
            sendOpenDoorMessage("开始连接蓝牙：" + device.deviceName, Color.GREEN)
            BleManager.get().connect(device, false, connectCallback)
        }
    }

    private val connectCallback: BleConnectCallback.() -> Unit = {
        onConnectStart {
            BleLogger.e("-----onConnectStart")
        }
        onConnectFail { bleDevice, connectFailType ->
            val msg: String = when (connectFailType) {
                is BleConnectFailType.UnSupportBle -> "设备不支持蓝牙"
                is BleConnectFailType.NoBlePermission -> "权限不足，请检查"
                is BleConnectFailType.NullableBluetoothDevice -> "设备为空"
                is BleConnectFailType.BleDisable -> "蓝牙未打开"
                is BleConnectFailType.ConnectException -> "连接异常(${connectFailType.throwable.message})"
                is BleConnectFailType.ConnectTimeOut -> "连接超时"
                is BleConnectFailType.AlreadyConnecting -> "连接中"
                is BleConnectFailType.ScanNullableBluetoothDevice -> "连接失败，扫描数据为空"
            }
            BleLogger.e(msg)
            messageMutableStateFlow.value = Message(msg)
        }
        onDisConnecting { isActiveDisConnected, bleDevice, _, _ ->
            BleLogger.e("-----${bleDevice.deviceAddress} -> onDisConnecting: $isActiveDisConnected")
            messageMutableStateFlow.value =
                Message("-----${bleDevice.deviceAddress} -> onDisConnecting: $isActiveDisConnected")
        }
        onDisConnected { isActiveDisConnected, bleDevice, _, _ ->
            BleLogger.e("-----${bleDevice.deviceAddress} -> onDisConnected: $isActiveDisConnected")
            messageMutableStateFlow.value = Message(
                "断开连接(${bleDevice.deviceAddress}，isActiveDisConnected: " +
                        "$isActiveDisConnected)",
                Color.RED
            )
        }
        onConnectSuccess { device, gatt ->
            messageMutableStateFlow.value = Message("连接成功:(mac:${device.deviceAddress},deviceName:${device.deviceName})")
            try {
                data = ByteUtils.hexStr2Bytes(createOpenDoorData(currentDeviceName))
                for (g in gatt!!.services) { //轮询蓝牙下的服务
                    val uuid = g.uuid.toString().uppercase(Locale.getDefault())
                    sendOpenDoorMessage("轮询蓝牙服务uuid：$uuid", Color.parseColor("#f43e06"))
                    sendOpenDoorMessage(
                        "=>服务类型：" + g.type + ",uuid是否包含FFF0:" + uuid.contains("FFF0"),
                        Color.parseColor("#f43e06")
                    )
                    if (g.type == BluetoothGattService.SERVICE_TYPE_PRIMARY && uuid.contains("FFF0")) {
                        sendOpenDoorMessage(
                            "==>轮询写入特征值，特征值数量：" + g.characteristics.size,
                            Color.parseColor("#f43e06")
                        )
                        for (bc in g.characteristics) { //轮询特征值
                            val canRead = bc.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                            val canWrite = bc.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                            sendOpenDoorMessage("===>轮询写入特征值：" + bc.uuid + " ,canRead: " + canRead + " ,canWrite: " + canWrite)
                            if (canWrite) {
                                sendOpenDoorMessage(
                                    "====>开始写入数据,deviceName：" + device.deviceName + ",data: " + ByteUtils.bytes2HexStr(
                                        data
                                    ) + ",uuid:" + g.uuid + ",CharacteristicsUUid:" + bc.uuid, Color.parseColor("#f43e06")
                                )
                                sendOpenDoorMessage(
                                    "====>开始写入数据,Thread name：" + Thread.currentThread().name,
                                    Color.parseColor("#f43e06")
                                )
                                //开始写入
                                writeData(device, g.uuid.toString(), bc.uuid.toString(), data)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                sendOpenDoorMessage("openDoor onError:" + Log.getStackTraceString(e), Color.RED)
                e.printStackTrace()
            }
        }
    }

    private fun writeData(
        bleDevice: BleDevice,
        serviceUUID: String,
        characteristicUUID: String,
        bytes: ByteArray
    ) {

        BleLogger.i("data is: ${BleUtil.bytesToHex(bytes)}")
        val mtu = BleManager.get().getOptions()?.mtu ?: Constants.DEFAULT_MTU
        //mtu长度包含了ATT的opcode一个字节以及ATT的handle2个字节
        val maxLength = mtu - 3
        val listData: SparseArray<ByteArray> = BleUtil.subpackage(bytes, maxLength)
        BleManager.get().writeData(bleDevice, serviceUUID, characteristicUUID, listData) {
            onWriteFail { _, currentPackage, _, t ->
                sendOpenDoorMessage("第${currentPackage}包数据写失败：${t.message}")
            }
            onWriteSuccess { _, currentPackage, _, justWrite ->
                sendOpenDoorMessage(
                    "$characteristicUUID -> 第${currentPackage}包数据写成功：" +
                            BleUtil.bytesToHex(justWrite)
                )
            }
            onWriteComplete { _, allSuccess ->
                //代表所有数据写成功，可以在这个方法中处理成功的逻辑
                sendOpenDoorMessage("$characteristicUUID -> 写数据完成，是否成功：$allSuccess")
            }
        }
    }

    /**
     * 断开连接
     */
    fun disConnect(bleDevice: BleDevice?) {
        bleDevice?.let { device ->
            BleManager.get().disConnect(device)
        }
    }

    /**
     * 断开所有连接 释放资源
     */
    fun close() {
        BleManager.get().closeAll()
    }

    /**
     * 生成开门数据
     *
     * @param macAddress
     * @return
     */
    private fun createOpenDoorData(macAddress: String): String {
        var data = ""
        val y: String
        val mon: String
        val d: String
        val h: String
        val m: String
        val c: Calendar = Calendar.getInstance()
        val year: Int = c.get(Calendar.YEAR)
        val month: Int = c.get(Calendar.MONTH) + 1
        val day: Int = c.get(Calendar.DAY_OF_MONTH)
        val hours: Int = c.get(Calendar.HOUR_OF_DAY)
        val minutes: Int = c.get(Calendar.MINUTE)
        y = year.toString()
        mon = time_0(month)
        d = time_0(day)
        h = time_0(hours)
        m = time_0(minutes)
        val timeStr: String = y.substring(2) + mon + d + h + m
        val key = macAddress + timeStr + "55AA5A5AA5"
        val content = timeStr + macAddress.substring(6)
        val param = "30" + cryptByDes(key, content).substring(0, 16) + timeStr + "FA34DD0001"
        var crc = 0
        var i = 0
        while (i < param.length - 1) {
            crc = crc xor param.substring(i, i + 2).toInt(16)
            i += 2
        }
        data = param + Integer.toString(crc, 16)
        sendOpenDoorMessage("content:$content", Color.BLACK)
        sendOpenDoorMessage("key:$key", Color.BLACK)
        sendOpenDoorMessage("data:$data", Color.BLACK)
        return data.uppercase(Locale.getDefault())
    }

    private fun time_0(timeValue: Int): String {
        return if (timeValue > 10) timeValue.toString() else "0$timeValue"
    }

    private fun cryptByDes(key: String, content: String): String {
        return EncryptUtils.encrypt3DES2HexString(
            ByteUtils.hexStr2Bytes(content),
            ByteUtils.hexStr2Bytes(key),
            "DESede/ECB/PKCS5Padding",
            null
        )
    }


    private fun sendOpenDoorMessage(message: String) {
        messageMutableStateFlow.value = Message(message)
    }

    private fun sendOpenDoorMessage(message: String, color: Int) {
        messageMutableStateFlow.value = Message(message, color)
    }

    private fun sendOpenDoorMessage(message: String, color: Int, textSize: Int) {
        messageMutableStateFlow.value = Message(message, color, textSize)
    }

    data class Message(
        val msg: String,
        val color: Int = ColorUtils.getColor(R.color.colorPrimary),
        val textSize: Int = 14
    )

    companion object {
        /**
         * 生成开门数据
         *
         * @param macAddress
         * @return
         */
        fun createOpenDoorData(macAddress: String): String {
            var data = ""
            val y: String
            val mon: String
            val d: String
            val h: String
            val m: String
            val c: Calendar = Calendar.getInstance()
            val year: Int = c.get(Calendar.YEAR)
            val month: Int = c.get(Calendar.MONTH) + 1
            val day: Int = c.get(Calendar.DAY_OF_MONTH)
            val hours: Int = c.get(Calendar.HOUR_OF_DAY)
            val minutes: Int = c.get(Calendar.MINUTE)
            y = year.toString()
            mon = time_0(month)
            d = time_0(day)
            h = time_0(hours)
            m = time_0(minutes)
            val timeStr: String = y.substring(2) + mon + d + h + m
            val key = macAddress + timeStr + "55AA5A5AA5"
            val content = timeStr + macAddress.substring(6)
            val param = "30" + cryptByDes(key, content).substring(0, 16) + timeStr + "FA34DD0001"
            var crc = 0
            var i = 0
            while (i < param.length - 1) {
                crc = crc xor param.substring(i, i + 2).toInt(16)
                i += 2
            }
            data = param + Integer.toString(crc, 16)
            return data.uppercase(Locale.getDefault())
        }

        private fun time_0(timeValue: Int): String {
            return if (timeValue > 10) timeValue.toString() else "0$timeValue"
        }

        private fun cryptByDes(key: String, content: String): String {
            return EncryptUtils.encrypt3DES2HexString(
                ByteUtils.hexStr2Bytes(content),
                ByteUtils.hexStr2Bytes(key),
                "DESede/ECB/PKCS5Padding",
                null
            )
        }

        fun test() {
            LogUtils.d("加密后：" + createOpenDoorData("21DCAA009469"))
        }
    }
}
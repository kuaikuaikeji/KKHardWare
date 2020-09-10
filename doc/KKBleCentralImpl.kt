package com.kk.hardware.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.data.BleScanState
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.kk.hardware.ble.KKBleCentralImpl.isBlueEnable
import com.kk.hardware.ble.KKBleCentralImpl.isLocationEnabled
import com.kk.hardware.ble.KKBleCentralImpl.isSupportBle
import com.kk.hardware.ble.peripheral.BleArmletPeripheralImpl
import com.kk.hardware.ble.peripheral.BleHealthScalePeripheralImpl
import com.kk.hardware.ble.peripheral.BleKneeCapPeripheralImpl
import com.kk.hardware.ble.peripheral.BleOtaPeripheralImpl
import com.kk.hardware.core.*

/**
 * ble 连接
 * 蓝牙扫描连接设备需要满足：支持蓝牙，蓝牙已打开，位置服务已打开
 * @see isSupportBle 是否支持蓝牙
 * @see isBlueEnable 蓝牙是否已打开
 * @see isLocationEnabled 位置服务是否已打开
 * @author zhangtao
 * @date 2020/6/10 15:27
 */
object KKBleCentralImpl : IKKCentral {

    override fun init() {
        BleManager.getInstance().init(KKHardWare.app)
    }

    override fun <T : IKKPeripheral> scan(clazz: Class<T>, callback: IScanListener<T>) {
        // 扫描回调
        val scanListener: IScanListener<AbstractKKBlePeripheral> by lazy {
            object : IScanListener<AbstractKKBlePeripheral> {
                override fun onStart(success: Boolean) {
                    callback.onStart(success)
                }

                override fun onScaning(peripheral: AbstractKKBlePeripheral) {
                    callback.onScaning(peripheral as T)
                }

                override fun onEnd() {
                    callback.onEnd()
                }
            }
        }
        when (clazz) {
            BleArmletPeripheralImpl::class.java ->
                scan(false, scanListener, BleArmletPeripheralImpl::class.java)
            BleHealthScalePeripheralImpl::class.java ->
                scan(false, scanListener, BleHealthScalePeripheralImpl::class.java)
            BleKneeCapPeripheralImpl::class.java ->
                scan(false, scanListener, BleKneeCapPeripheralImpl::class.java)
            else -> callback.onStart(false)
        }
    }

    /**
     * 扫描蓝牙设备
     * @param isOta 是否扫描 ota 设备
     */
    fun scan(
        isOta: Boolean,
        callback: IScanListener<AbstractKKBlePeripheral>,
        vararg clazz: Class<out AbstractKKBlePeripheral>
    ) {
        // 是否支持蓝牙并且是否开启
        if (!isSupportBle() || !isBlueEnable()) {
            callback.onStart(false)
            return
        }

        // 得到需要检测的设备类型名称
        val deviceNameArray =
            clazz.mapNotNull {
                when (it) {
                    BleArmletPeripheralImpl::class.java -> BleArmletPeripheralImpl.DEVICE_NAME
                    BleHealthScalePeripheralImpl::class.java -> BleHealthScalePeripheralImpl.DEVICE_NAME
                    BleKneeCapPeripheralImpl::class.java -> BleKneeCapPeripheralImpl.DEVICE_NAME
                    else -> null
                }
            }.toTypedArray()
        // 是否有符合扫描条件的设备
        if (deviceNameArray.isEmpty()) {
            callback.onStart(false)
            return
        }
        val names = if (isOta) arrayOf(AbstractKKBlePeripheral.DEVICE_NAME_OTA, *deviceNameArray)
        else deviceNameArray
        // 根据设备名称进行扫描 - OTA模式设备一起扫描
        val scanRuleConfig = BleScanRuleConfig.Builder()
            .setDeviceName(true, *names)
            .build()
        BleManager.getInstance().initScanRule(scanRuleConfig)

        val convert: (BleDevice) -> AbstractKKBlePeripheral? = {
            when (it.name) {
                BleArmletPeripheralImpl.DEVICE_NAME -> BleArmletPeripheralImpl(it)
                BleHealthScalePeripheralImpl.DEVICE_NAME -> BleHealthScalePeripheralImpl(it)
                BleKneeCapPeripheralImpl.DEVICE_NAME -> BleKneeCapPeripheralImpl(it)
                AbstractKKBlePeripheral.DEVICE_NAME_OTA -> {
                    when {
                        clazz.contains(BleArmletPeripheralImpl::class.java) ->
                            BleOtaPeripheralImpl(it)
                        else -> null
                    }
                }
                else -> null
            }
        }
        cancelScan()
        BleManager.getInstance().scan(ScanCallback(callback, convert))
    }

    override fun <T : IKKPeripheral> connect(
        peripheral: T,
        callback: IConnectListener<T>
    ) {
        if (!checkBlueStatus(callback)) return

        when (peripheral) {
            is AbstractKKBlePeripheral -> {
                BleManager.getInstance()
                    .connect(peripheral.device, ConnectCallback(peripheral, callback))
            }
            else -> {
                callback.onStart()
                callback.onConnectFail(peripheral, KKPeripheralException.unknown())
            }
        }
    }

    override fun <T : IKKPeripheral> connect(
        mac: String, clazz: Class<T>, callback: IConnectListener<T>
    ) {
        if (!checkBlueStatus(callback)) return

        val remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
        remoteDevice.name
        val bleDevice = BleManager.getInstance().convertBleDevice(remoteDevice)
        val peripheral = when (clazz) {
            BleArmletPeripheralImpl::class.java -> BleArmletPeripheralImpl(bleDevice)
            BleHealthScalePeripheralImpl::class.java -> BleHealthScalePeripheralImpl(bleDevice)
            BleKneeCapPeripheralImpl::class.java -> BleKneeCapPeripheralImpl(bleDevice)
            else -> {
                callback.onStart()
                callback.onConnectFail(null, KKBleException.unsupportedDevices())
                return
            }
        }
        connect(peripheral as T, callback)
    }

    override fun disconnect(peripheral: IKKPeripheral) {
        if (peripheral is AbstractKKBlePeripheral) {
            BleManager.getInstance().disconnect(peripheral.device)
        }
    }

    override fun cancelScan() {
        if (BleManager.getInstance().scanSate == BleScanState.STATE_SCANNING) {
            BleManager.getInstance().cancelScan()
        }
    }

    override fun destroy() {
        cancelScan()
        BleManager.getInstance().destroy()
    }

    /**
     * @return 蓝牙是否可用
     */
    fun isSupportBle(): Boolean = BleManager.getInstance().isSupportBle

    /**
     * 是否已开启蓝牙
     */
    fun isBlueEnable(): Boolean = BleManager.getInstance().isBlueEnable

    /**
     * 开启蓝牙
     */
    fun enableBluetooth() {
        if (isSupportBle()) {
            BleManager.getInstance().enableBluetooth()
        }
    }

    /**
     * 判断定位服务是否开启
     *
     * @return true 表示开启
     */
    fun isLocationEnabled(context: Context): Boolean {
        val service =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (VERSION.SDK_INT >= VERSION_CODES.P) {
            service.isLocationEnabled
        } else {
            (service.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    || service.isProviderEnabled(LocationManager.GPS_PROVIDER))
        }
    }

    /**
     * 设备连接时检测蓝牙状态
     */
    private fun <T : IKKPeripheral> checkBlueStatus(callback: IConnectListener<T>): Boolean {
        if (!isSupportBle()) {
            callback.onStart()
            callback.onConnectFail(null, KKBleException.unsupportedBle())
            return false
        }
        if (!isBlueEnable()) {
            callback.onStart()
            callback.onConnectFail(null, KKBleException.disenableBle())
            return false
        }
        return true
    }
    //----------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------
    /**
     * 扫描回调
     * @param callback 回调
     * @param convert 将蓝牙设备转换为需要的设备
     */
    private class ScanCallback<out T : AbstractKKBlePeripheral>(
        private val callback: IScanListener<T>,
        private val convert: (BleDevice) -> T?
    ) : BleScanCallback() {
        override fun onScanFinished(scanResultList: MutableList<BleDevice>?) {
            callback.onEnd()
        }

        override fun onScanStarted(success: Boolean) {
            callback.onStart(success)
        }

        override fun onScanning(bleDevice: BleDevice?) {
            callback.onScaning(convert(bleDevice ?: return) ?: return)
        }
    }

    /**
     * 连接回调
     * @param peripheral 连接的设备
     * @param callback 连接回调
     */
    private class ConnectCallback<out T : IKKPeripheral>(
        private val peripheral: T,
        private val callback: IConnectListener<T>
    ) : BleGattCallback() {
        override fun onStartConnect() {
            callback.onStart()
        }

        override fun onDisConnected(
            isActiveDisConnected: Boolean, device: BleDevice?,
            gatt: BluetoothGatt?, status: Int
        ) {
            callback.onDisconnected(isActiveDisConnected, peripheral)
        }

        override fun onConnectSuccess(
            bleDevice: BleDevice?, gatt: BluetoothGatt?, status: Int
        ) {
            callback.onConnectSuccess(peripheral)
            if (peripheral is AbstractKKBlePeripheral) {
                peripheral.connectSuccess()
            }
        }

        override fun onConnectFail(
            bleDevice: BleDevice?,
            exception: BleException?
        ) {
            val e = if (exception == null) {
                KKPeripheralException.unknown()
            } else {
                KKBleException.ble(exception)
            }
            callback.onConnectFail(peripheral, e)
        }
    }
}
# 快快硬件连接-Android
## 导入

1. 项目 `build.gradle` 添加仓库地址
```
    allprojects {
        repositories {
            // 快快maven仓库地址
            maven { url 'http://36.110.31.137:8082/nexus/content/groups/public/' }
            ...
        }
    }
```

2. 需要应用的模块 `build.gradle` 添加引用
```
    dependencies {
        implementation 'com.kuaikuai.hardware:ble:1.0.0-SNAPSHOT'
    }
```

## 使用

### 连接控制

#### 蓝牙

>
* 蓝牙使用需动态申请位置权限，且需开启位置服务
* 蓝牙实现类：[`KKBleCentralImpl`](http://)
* 具体使用请参照 `demo`

>
```
/** 硬件连接 */
interface IKKCentral {

    /** 初始化 */
    fun init()

    /** 开始扫描 */
    fun <T : IKKPeripheral> scan(clazz: Class<T>, callback: (IScanListener<T>))

    /** 设备连接 */
    fun <T : IKKPeripheral> connect(peripheral: T, callback: IConnectListener<T>)

    /** 设备连接 */
    fun <T : IKKPeripheral> connect(mac: String, clazz: Class<T>, callback: IConnectListener<T>)

    /** 设备断开连接 */
    fun disconnect(peripheral: IKKPeripheral)

    /** 取消扫描 */
    fun cancelScan()

    /** 资源释放 */
    fun destroy()
}

/** 设备扫描回调接口 */
interface IScanListener<T : IKKPeripheral> {
    /** 扫描开始 */
    fun onStart(success: Boolean)

    /** 扫描到设备 */
    fun onScaning(peripheral: T)

    /** 扫描结束 */
    fun onEnd()
}

/** 设备连接回调接口 */
interface IConnectListener<in T : IKKPeripheral> {
    /** 开始连接 */
    fun onStart()

    /** 连接成功 */
    fun onConnectSuccess(peripheral: T)

    /** 连接失败 */
    fun onConnectFail(peripheral: T?, exception: KKPeripheralException)

    /** 连接断开 */
    fun onDisconnected(isActive: Boolean, peripheral: T)
}
```

### 设备类型
> * 扫描到设备成功连接后方可调用设备方法；
> * 具体使用请参照 `demo`
> * 性别标识
    * 健康秤：男：`1`，女：`0`
    * 其他：男：`1`，女：`2`

#### 健康秤
>
```
/** 健康秤相关 */
interface KKHealthScalePeripheral {
    /** 测量回调 */
    fun resultListener(userModel: UserModel, listener: IResultListener)
}

```
* 测量回调接口：[`IResultListener`](http://)

#### 家用臂带
>
```
/** 家用臂带相关 */
interface KKArmletPeripheral {

    /** 指令接收回调 */
    var resultListener: IResultListener?

    /** 读取电量信息 */
    fun readElectricQuantity(listener: IReadElectricQuantityListener)

    /** 发送指令 */
    fun sendCommand(command: AbstractModelToBytes, listener: IWriteListener)
}
```
* 发送指令内容：[`ArmletCommand`](http://)
* 指令接收回调接口：[`IResultListener`](http://)
* 读取电量回调接口：[`IReadElectricQuantityListener`](http://)

##### 固件升级
具体使用方法请查看 demo ota 升级部分内容
> 1. 首先通过 `ArmletVersionCommand` 指令得到固件版本号
> 2. 然后通过 `DeviceApi.upgradeArmlet(version,callback)` 判断是否需要升级
> 3. 再开启设备 OTA 模式，扫描 OTA 设备，选择设备
> 4. 最后通过 `DfuHelper.upgrade(mac,entity)` 方法发起升级

#### 膝带(待更新)

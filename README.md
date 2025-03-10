<br>

<br>



<div style='text-align:center'>
    <img style='width:100px;height:100px' src='https://sdpro.top/pub/img/spay/logo.png'/>
    <p>个人也可实现在线支付</p>
    <div>
        👉<a href='https://github.com/1711680493/SPay'>https://github.com/1711680493/SPay</a>👈
        <br>
        👉<a href='https://gitee.com/hackshendi/SPay'>https://gitee.com/hackshendi/SPay</a>👈
    </div>
</div>

<br>

---

<br>


# 前言

本APP是为了解决个人无法接入在线支付的问题，接入第三方支付需要经营许可证，如微信、支付宝。除此之外还有一些中间商代理的方式（手续费高）

这些方法对于个人小打小闹来说行不通，于是改变思路，直接通过收款码方式收款，通过监听APP通知的方式知晓知否支付，这样也可以实现类似效果，**仅供学习交流**。

<br>

**优点**

* 不需要任何资质
* 无手续费
* 相比接入更简单

<br>

**缺点**

* APP需要一直打开，并一直在线（手机基本上都是一直打开，问题不大）
* 不能指定固定金额，需要用户手动输入（不怕麻烦可以生成固定金额的二维码，可能有其他更好的方法）
* 因使用监听通知的方式，必须要有通知，当手机打开微信/支付宝时，可能不会发送通知，可能存在漏单情况，比如通知内容有更改等，需要有反馈入口以及手动完成此笔交易功能。
    * 部分常见APP已使用无障碍解决

* 不能同一时间有两笔相同金额的订单，因为无法知晓支付对应的是哪笔订单，需要有排队机制，或随机增加0.01或减少0.01等方式解决。
* 可能在支付时收到其他款项，金额与订单金额一致，导致用户没有付款但通知服务器的情况。
* 支付只能通过扫二维码（后续再看有没有其他办法）

<br>

<br>

# 体验

可通过以下链接进行体验，其中支付的金额就当赞赏咯~

https://pay.sdpro.top/spay.html

<br>

<br>

# 流程与设计

用户下单，服务端创建订单，用户付款后，APP监听到通知，将金额等信息发送至服务端，服务端进行验证处理。

验证参考 **支付回调接口**

<br>

<br>

## 服务端接口

目前需要两个接口，一个是获取APP基本信息的接口，一个是服务端用于接收支付通知，url都在APP上动态设置。

可通过APP的测试模块来测试对应接口

<br>

### 基础信息接口

为了方便使用，配置信息放置在服务端上，所以首先需要拿到基础信息。

因是监听通知的方式，通知字符串内容可能会有改变，所以将其作为配置来增加可用性

<br>

**接口请求类型 GET**，无需参数与验证，只需请求url即可获取基础信息（使用SSL来说，相对安全，而且基础信息并不需要保密）

> 微信将金额信息放到通知内容中，支付宝将金额信息放到通知标题中，所以如下设计

接口响应类型为JSON，数据如下

```json
{
    // 状态码,10000为成功
    "code" : "10000",
    // 数据,获取成功则为下方数据,失败则为错误提示.
    "msg" : {
        // 确认字符串为支付字符串的配置
        "paystr" : {
            // 格式为 类型:内容，下方是微信的示例
            "微信" : {
                // v1.0.1新增，是否上交服务器，默认true
                "isUp" : true,
                // app包名,数组中的都将匹配
                "packName" : ["com.tencent.mm"],
                // 匹配通知与截取金额的信息列表
                "list" : [
                    // 为兼容通知标题与内容都可能携带金额的情况，将按顺需依次处理下方JSON对象
                    {
                        // 金额是在通知标题还是在内容，true代表在标题 false代表在内容
                        "isTitle" : false,
                        // 通知内容的字符串匹配,将截取start与end之间的内容，如果有.
                        // (截取的金额将被转double,转换失败则代表非支付)
                        // 例如微信支付是:(微信支付收款xxx元)，那么可以如下.
                        // (如果是以金额开头，那么start为""，如果是以金额结尾，那么end为"")
                        "start" : "微信支付收款",
                        "end" : "元",
                        // v1.0.2新增，校验标题和校验内容，可选
                        // 存在的话会判断标题/内容是否包含指定内容，不包含则匹配失败，空字符串等价于不存在
                        "checkTitle" : "微信支付",
                        "checkContent" : ""
                    },
                    {
                        "isTitle" : false,
                        "start" : "收款",
                        "end" : "元"
                    }
                ]
            }
        },
        "purl" : "支付回调接口的地址"
    }
}
```

<br>

#### 注意事项

不知从什么时候开始（目前发现问题是2024-06-09），微信的通知进行了更改，带上了一个¥符号，需要注意，配置中符号的编码（UTF-8），这个符号是两个横线的，如果是ASCII等其他编码，将无法正确匹配

（最好是通过复制粘贴上面符号，与键盘上的￥不同，一般在输入法表情中的¥）

> 后面不知道是否还有更改，但这里需要注意的就是编码，以及匹配的start和end字符是需要与通知中的字符是完全一致的

<br>

### 支付回调接口

**接口请求类型 POST**

需要接收以下参数

| 参数名 | 描述                                                         |
| ------ | ------------------------------------------------------------ |
| amount | 金额，单位分（这样不易出现精度丢失问题）                     |
| type   | 基础信息中匹配的类型，例如 微信                              |
| time   | 提交此请求的时间戳                                           |
| nonce  | 随机字符串，可通过与time结合为唯一字符串                     |
| sign   | 消息认证码                                                   |
| ...    | 配置中的自定义参数，将直接追加在参数中，格式 `key=val&key=val...`  v1.0.3新增 |

<br>

拿到参数后首先要验证`sign`与`time`，`sign` 正确，时间差合理（比如在订单失效期前支付，失效期两分钟，那么设置为两分半即可）则代表此次请求是APP所发送。

而后通过`amount`与`type`来确认对应的订单，完成支付。（如果没有订单那可能代表此请求不是APP所发送，）

> `sign` 与 `time` 验证不通过则直接验证失败，如果验证成功但没有没有订单，那么同样也返回验证失败，并且需要记录日志并提醒开发者，更新一个更复杂的密钥，不要带多余的信息，防止暴力破解密钥。

> （可选）每次处理完将time与nonce存入数据库，处理前先验证相同的time与nonce是否存在，存在则验证失败，这样可以防止重放攻击，也就减少了密钥被暴力破解的可能

<br>

---

**sign验证方式：**

使用 **HMAC-SHA256** 通过**APP配置的密钥对数据计算出消息认证码**，然后对其**Base64编码**，其中数据是直接转字符串进行相加：`amount+type+time+nonce`

其中密钥自行生成，存储在服务器与APP上配置，不在网络中传输与暴露

**生成代码示例（Java）：**

```java
public class HMACSHA256Util {

    /**
     * 根据密钥与数据生成消息认证码
     * @param priKey    密钥
     * @param data      数据
     * @return  消息认证码
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static String hmacSHA256(byte[] priKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(priKey, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data));
    }

}
```

```java
// 生成sign
StringBuilder data = new StringBuilder();
data.append(amount).append(type).append(time).append(nonce);

String sign = HMACSHA256Util.hmacSHA256("密钥".getBytes(), data.toString().getBytes());
```

---

<br>

接口响应如下，用于APP记录此次请求。

```json
{
    "code" : "10000",
    "msg" : "当code非10000时，此为错误信息，将在APP上可见"
}
```

<br>

<br>

## 时区

在 1.0.1 版本，默认时区为 `GMT+08:00`

<br>

<br>

## 无障碍

当前设备在使用支付APP时收款，支付APP不会发送通知，于是通过无障碍监听解决

<br>

目前拥有无障碍处理的APP如下

<br>

**微信（com.tencent.mm）**

没有通知的页面

* 首页
    * 通过监听消息列表，与通知基本一致
    * 其中消息item左边图标，右边上面标题，下面内容，右上角时间（只接受 时:分）
    * 规则：除了基础信息匹配外，右上角时分与当前时分一致且时分或金额与上一次提交不一致才匹配
        * 同一分钟有重复的金额收款则会漏单
    * 金额是¥符号
* 微信支付消息页
    * 只检测最后一个消息卡片
    * 其中左上角是标题，中间部分是内容（如收款金额￥0.01），卡片上方有时间（只接受 时:分）
    * 规则：除基础信息匹配外，当前消息卡片上方必须要有时间，时间与当前时间一致，且金额和时间与上一次提交不一致才匹配才匹配（一段时间内[大概两分半]收到的多个消息会叠加，且只在最上面的卡片上方现实时间）
        * 在卡片重叠时间间隔都会漏单

    * 金额符号是键盘上的￥


<br>

<br>

# 使用

打开APP会自动跳转开启通知权限，将其开启，可在测试中进行发送通知测试。

<br>

## 权限

* 无障碍
* 通知
* 通知监听

<br>

在APP的测试界面中点击开始测试通知，然后点击发送通知按钮，以此来测试APP是否拥有通知监听权限。

> 如果开启了监听通知权限仍然监听不到通知，可尝试重新开关监听通知权限和重启手机

<br>

## 加入白名单

为了让APP不被系统优化，需要将当前APP加入白名单，不同手机操作方法不同，但大致类似。

设置中

电池 -> 应用耗电管理 -> SPay -> 允许完全后台行为，允许应用自启动，允许应用关联启动

应用管理 -> 自启动管理 -> SPay打开

应用管理 -> 关联启动管理 -> SPay打开

<br>

打开最近任务（底部有三按键点击正方形那个，如果是左右滑动上一级的在底部往上滑），给打开的SPay加个锁定

<br>

## 配置

打开 SPay APP，点击设置，**输入基础信息接口地址**后点击设置与更新，**输入密钥信息**后点击设置

<br>

**自定义参数配置**项可选，格式为`key=val&key=val`，当请求支付回调接口时会将此配置值追加在参数中携带，通常用于区分当前设备等

<br>

<br>

对于基础信息接口地址，我制作了一份JSON放置我的服务器上，可供测试： https://sdpro.top/json/spay_test_base.json

JSON内容如下

```json
{
    "code" : "10000",
    "msg" : {
        "paystr" : {
            "SPay" : {
                "packName" : ["shendi.pay"],
                "list" : [
                    {
                        "isTitle" : false,
                        "start" : "测试支付收款",
                        "end" : "元"
                    }
                ]
            },
            "微信" : {
                "packName" : ["com.tencent.mm"],
                "list" : [
                    {
                        "isTitle" : false,
                        "start" : "微信支付收款",
                        "end" : "元"
                    }
                ]
            }
        },
        "purl" : "支付回调接口的地址"
    }
}
```

<br>

在APP的测试页面中，通知部分，通知标题输入`测试支付`，通知内容输入`测试支付收款1.11元`，而后即可在记录与通知页面中看到效果。

<br>

<br>

## 手动支付

如果手机是正在打开微信，在这个时候，用户使用微信扫码支付，那么微信将不会发送系统通知，于是APP就无法自动处理，导致用户在线支付失败。

为了解决这种情况，在 APP 的测试页面中，支付回调接口部分，提供了一个 `使用配置测试` 的按钮，即我们知晓用户付款了多少金额，知晓支付类型，输入后点击按钮同样将请求服务端的回调接口，这是一种补救办法。

但对于服务端来说，拥有操作超时时间，对于服务端设计，实际超时时间应该大于用户超时时间20秒。以及手动操作需要在失效期前。

<br>

除此之外，还可以发展到其他场景，例如线上虚拟商品，但线下收款，通过手动支付进行核销...

<br>

<br>

## 关闭无障碍

设置界面中可以关闭无障碍，默认是打开的，如果开启，每次打开APP都会检查无障碍服务是否运行。

无障碍还会导致匹配的APP使用起来带一点卡顿，如果没有对应类型的需求，可以将其关闭。

<br>

<br>
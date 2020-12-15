### Socket通信数据帧Frame的构建和相关知识点

#### Frame（通信的最小单位）结构
```
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+
```

##### 结构说明
- FIN： 1 bit

    表示这是一个消息的最后的一帧。第一个帧也可能是最后一个。  
    0 : 还有后续帧  
    1 : 最后一帧
    
- RSV1、2、3： 1 bit each

    除非一个扩展经过协商赋予了非零值以某种含义，否则必须为0
    如果没有定义非零值，并且收到了非零的RSV，则websocket链接会失败
    
- Opcode： 4 bit

    解释说明 “Payload data” 的用途/功能
    如果收到了未知的opcode，最后会断开链接
    定义了以下几个opcode值:
    - 0x0 : 代表连续的帧
    - 0x1 : text帧
    - 0x2 ： binary帧
    - 0x3-7 ： 为非控制帧而预留的
    - 0x8 ： 关闭握手帧
    - 0x9 ： ping帧
    - 0xA :  pong帧
    -  0xB-F ： 为非控制帧而预留的

- Mask： 1 bit

    定义“payload data”是否被添加掩码
    如果置1， “Masking-key”就会被赋值
    所有从客户端发往服务器的帧都会被置1

- Payload length： 7 bit | 7+16 bit | 7+64 bit

    “payload data” 的长度如果在0~125 bytes范围内，它就是“payload length”，
    如果是126 bytes， 紧随其后的被表示为16 bits的2 bytes无符号整型就是“payload length”，
    如果是127 bytes， 紧随其后的被表示为64 bits的8 bytes无符号整型就是“payload length”

- Masking-key： 0 or 4 bytes

    所有从客户端发送到服务器的帧都包含一个32 bits的掩码（如果“mask bit”被设置成1），否则为0 bit。一旦掩码被设置，所有接收到的payload data都必须与该值以一种算法做异或运算来获取真实值。（见下文）
    
- Payload data: (x+y) bytes

    它是"Extension data"和"Application data"的总和，一般扩展数据为空。

- Extension data: x bytes

    除非扩展被定义，否则就是0
    任何扩展必须指定其Extension data的长度

- Application data: y bytes

    占据"Extension data"之后的剩余帧的空间
    
### 构建Frame
```java
Frame frame = new Frame();
frame.fin = true;
frame.hasMask = true;
frame.writeTo(mBufferedOutputStream);
```

```java
public void writeTo(BufferedOutputStream output) throws IOException {
    // 0~7 FIN + RSV1、2、3 + Opcode
    output.write(encodeFirstByte());
    // 8~15 MASK + Payload len
    byte[] lengthAndMaskBit = encodeLength(payloadLen);
    if (hasMask) {
      // MASK值
      lengthAndMaskBit[0] |= 0x80;
    }
    output.write(lengthAndMaskBit, 0, lengthAndMaskBit.length);
    
    if (hasMask) {
      // 生成随机4位MASK_KEY
      byte[] b = new byte[4];
      new Random().nextBytes(b);
      output.write(b, 0, b.length);
      // 掩码操作
      MaskingHelper.mask(payloadData, b);
    }
    
    // 写入用户输入文本
    output.write(payloadData, 0, (int) payloadLen);
}
```


#### 参考
- [WebSocket协议](https://github.com/abbshr/abbshr.github.io/issues/22)
- [数据掩码的作用](https://www.infoq.cn/article/deep-in-websocket-protocol/)
- [原码, 反码, 补码 详解](https://www.cnblogs.com/zhangziqiu/archive/2011/03/30/computercode.html)
- [Android位运算](https://conorlee.top/2019/12/08/Android-bits-operation/)
- [基于WebSocket没有掩码的攻击](https://tools.ietf.org/html/rfc6455#section-10.3)

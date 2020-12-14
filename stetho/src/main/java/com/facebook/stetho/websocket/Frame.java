/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * WebSocket frame as per RFC6455.
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-------+-+-------------+-------------------------------+
 *  |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 *  |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 *  |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 *  | |1|2|3|       |K|             |                               |
 *  +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 *  |     Extended payload length continued, if payload len == 127  |
 *  + - - - - - - - - - - - - - - - +-------------------------------+
 *  |                               |Masking-key, if MASK set to 1  |
 *  +-------------------------------+-------------------------------+
 *  | Masking-key (continued)       |          Payload Data         |
 *  +-------------------------------- - - - - - - - - - - - - - - - +
 *  :                     Payload Data continued ...                :
 *  + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 *  |                     Payload Data continued ...                |
 *  +---------------------------------------------------------------+
 *
 * 按照RFC中的描述：
 *
 * FIN： 1 bit
 *
 * 表示这是一个消息的最后的一帧。第一个帧也可能是最后一个。
 *  %x0 : 还有后续帧
 *  %x1 : 最后一帧
 *
 * RSV1、2、3： 1 bit each
 *
 *  除非一个扩展经过协商赋予了非零值以某种含义，否则必须为0
 *  如果没有定义非零值，并且收到了非零的RSV，则websocket链接会失败
 *
 * Opcode： 4 bit
 *
 *  解释说明 “Payload data” 的用途/功能
 *  如果收到了未知的opcode，最后会断开链接
 *  定义了以下几个opcode值:
 *     %x0 : 代表连续的帧
 *     %x1 : text帧
 *     %x2 ： binary帧
 *     %x3-7 ： 为非控制帧而预留的
 *     %x8 ： 关闭握手帧
 *     %x9 ： ping帧
 *     %xA :  pong帧
 *     %xB-F ： 为非控制帧而预留的
 *
 * Mask： 1 bit
 *
 *  定义“payload data”是否被添加掩码
 *  如果置1， “Masking-key”就会被赋值
 *  所有从客户端发往服务器的帧都会被置1
 *
 * Payload length： 7 bit | 7+16 bit | 7+64 bit
 *
 *  “payload data” 的长度如果在0~125 bytes范围内，它就是“payload length”，
 *  如果是126 bytes， 紧随其后的被表示为16 bits的2 bytes无符号整型就是“payload length”，
 *  如果是127 bytes， 紧随其后的被表示为64 bits的8 bytes无符号整型就是“payload length”
 *
 * Masking-key： 0 or 4 bytes
 *
 *  所有从客户端发送到服务器的帧都包含一个32 bits的掩码（如果“mask bit”被设置成1），否则为0 bit。一旦掩码被设置，所有接收到的payload data都必须与该值以一种算法做异或运算来获取真实值。（见下文）
 *
 * Payload data: (x+y) bytes
 *
 *  它是"Extension data"和"Application data"的总和，一般扩展数据为空。
 *
 * Extension data: x bytes
 *
 *  除非扩展被定义，否则就是0 任何扩展必须指定其Extension data的长度
 *
 * Application data: y bytes
 *
 *  占据"Extension data"之后的剩余帧的空间
 * 注意：这些数据都是以二进制形式表示的，而非ascii编码字符串
 * 参考link{https://github.com/abbshr/abbshr.github.io/issues/22}
 * 原码、反码、补码{https://www.cnblogs.com/zhangziqiu/archive/2011/03/30/computercode.html}
 */
public class Frame {
  public static final byte OPCODE_TEXT_FRAME = 0x1;
  public static final byte OPCODE_BINARY_FRAME = 0x2;
  public static final byte OPCODE_CONNECTION_CLOSE = 0x8;
  public static final byte OPCODE_CONNECTION_PING = 0x9;
  public static final byte OPCODE_CONNECTION_PONG = 0xA;

  public boolean fin;
  public boolean rsv1;
  public boolean rsv2;
  public boolean rsv3;
  public byte opcode;
  public boolean hasMask;
  public long payloadLen;
  public byte[] maskingKey;
  public byte[] payloadData;

  public void readFrom(BufferedInputStream input) throws IOException {
    // 读第一个字节，8位内容：0~7 FIN + RSV1、2、3 + Opcode
    // 调试的时候拿到input中buf第一个字节是-127，因为计算机中用补码进行运算，-127=0b11111111，第一位是符号位，
    // 减1然后除符号位取反，得到原码为0b10000001，就是FIN为true，RSV1、2、3为false，Opcode为0x1
    decodeFirstByte(readByteOrThrow(input));
    // 读第二个字节，8位内容： 8~15 MASK + Payload len
    byte maskAndFirstLengthBits = readByteOrThrow(input);
    // MASK值 在8位的最高位用0或1表示一个布尔值
    hasMask = (maskAndFirstLengthBits & 0x80) != 0;
    // 用户输入文本长度 后7位最大值为0b1111111=127, 125的话就是长度，126、127都是标识值，需要继续read后面2字节或者8字节拿到长度
    payloadLen = decodeLength((byte)(maskAndFirstLengthBits & ~0x80), input);
    // 如果有掩码的话，拿到4位是掩码的key
    maskingKey = hasMask ? decodeMaskingKey(input) : null;
    // 拿到用户输入文本
    payloadData = new byte[(int)payloadLen];
    readBytesOrThrow(input, payloadData, 0, (int)payloadLen);
    if (maskingKey != null) {
      // 掩码解密文本
      MaskingHelper.unmask(maskingKey, payloadData, 0, (int)payloadLen);
    }
  }

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

  // 0x80 = 0b10000000 0x10 = 0b00010000 0xf = 0b00001111
  private void decodeFirstByte(byte b) {
    fin = (b & 0x80) != 0;
    rsv1 = (b & 0x40) != 0;
    rsv2 = (b & 0x20) != 0;
    rsv3 = (b & 0x10) != 0;
    opcode = (byte)(b & 0xf);
  }

  private byte encodeFirstByte() {
    byte b = 0;
    if (fin) {
      b |= 0x80;
    }
    if (rsv1) {
      b |= 0x40;
    }
    if (rsv2) {
      b |= 0x20;
    }
    if (rsv3) {
      b |= 0x10;
    }
    b |= (opcode & 0xf);
    return b;
  }

  // 解码拿到长度
  private long decodeLength(byte firstLenByte, InputStream in) throws IOException {
    if (firstLenByte <= 125) {
      return firstLenByte;
    } else if (firstLenByte == 126) {
      // 高8位 | 低8位 得到126~65535的区间数
      return (readByteOrThrow(in) & 0xff) << 8 | (readByteOrThrow(in) & 0xff);
    } else if (firstLenByte == 127) {
      long len = 0;
      for (int i = 0; i < 8; i++) {
        len <<= 8;
        len |= (readByteOrThrow(in) & 0xff);
      }
      return len;
    } else {
      throw new IOException("Unexpected length byte: " + firstLenByte);
    }
  }

  // 对长度做编码
  private static byte[] encodeLength(long len) {
    if (len <= 125) {
      return new byte[] { (byte)len };
    } else if (len <= 0xffff) { // 0xffff = 65535
      return new byte[] {
          126, // 标志位 ，表示数据长度是126到65535之间，用接下去的16位表示值
          (byte)((len >> 8) & 0xff), // len >> 8就是去掉低8位，得到的值放高8位
          (byte)((len) & 0xff) // 放低8位
      };
    } else {
      return new byte[] {
          127, // 标志位 ，表示数据长度是65536到2的64次方之间
          (byte)((len >> 56) & 0xff),
          (byte)((len >> 48) & 0xff),
          (byte)((len >> 40) & 0xff),
          (byte)((len >> 32) & 0xff),
          (byte)((len >> 24) & 0xff),
          (byte)((len >> 16) & 0xff),
          (byte)((len >> 8) & 0xff),
          (byte)((len) & 0xff)
      };
    }
  }

  private static byte[] decodeMaskingKey(InputStream in) throws IOException {
    byte[] key = new byte[4];
    readBytesOrThrow(in, key, 0, key.length);
    return key;
  }

  private static void readBytesOrThrow(InputStream in, byte[] buf, int offset, int count)
      throws IOException {
    while (count > 0) {
      int n = in.read(buf, offset, count);
      if (n == -1) {
        throw new EOFException();
      }
      count -= n;
      offset += n;
    }
  }

  private static byte readByteOrThrow(InputStream in) throws IOException {
    int b = in.read();
    if (b == -1) {
      throw new EOFException();
    }
    return (byte)b;
  }
}

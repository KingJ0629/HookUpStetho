/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.websocket;

class MaskingHelper {
  public static void unmask(byte[] key, byte[] data, int offset, int count) {
    int index = 0;
    while (count-- > 0) {
      data[offset++] ^= key[index++ % key.length];
    }
  }
  
  /**
   * 异或运算
   * 数值相同则为 0，数值不同则为 1
   * 3 ^ 5 = 6,而 6 ^ 5 = 3
   */
  public static void mask(byte[] original, byte[] maskKey) {
    for (int i = 0; i < original.length; i++) {
      original[i] = (byte) (original[i] ^ maskKey[i % 4]);
    }
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.websocket;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class ReadHandler {
  private final BufferedInputStream mBufferedInput;
  private final SimpleEndpoint mEndpoint;

  /**
   * Used to build a larger payload over multiple frames.
   */
  private final ByteArrayOutputStream mCurrentPayload = new ByteArrayOutputStream();

  public ReadHandler(InputStream bufferedInput, SimpleEndpoint endpoint) {
    mBufferedInput = new BufferedInputStream(bufferedInput, 1024);
    mEndpoint = endpoint;
  }

  /**
   * Enter a loop processing incoming frames until orderly shutdown or a socket exception is
   * thrown.  This method returns normally on orderly shutdown, throws otherwise.
   *
   * @throws IOException Socket exception during the read loop.
   */
  public void readLoop(ReadCallback readCallback) throws IOException {
    Frame frame = new Frame();
    do {
      Log.i("247144", "ReadHandler frame.readFrom————>阻塞");
      // 阻塞等待客户端命令
      frame.readFrom(mBufferedInput);
      mCurrentPayload.write(frame.payloadData, 0, (int)frame.payloadLen);
      Log.i("247144", "ReadHandler mCurrentPayload————>" + mCurrentPayload.toString());
      if (frame.fin) {
        byte[] completePayload = mCurrentPayload.toByteArray();
        readCallback.onCompleteFrame(frame.opcode, completePayload, completePayload.length);
        mCurrentPayload.reset();
      }
    } while (frame.opcode != Frame.OPCODE_CONNECTION_CLOSE);
  }
}

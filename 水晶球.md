### 手机和PC端数据通信流程

1. 手机通过USB方式连接电脑
2. 运行一个程序，集成`stetho`（程序运行时会启动一个`socket`的服务端，等待`Client`连接）
3. 在命令行输入 `adb shell cat /proc/net/unix | grep --text stetho`
    - `/proc/net/unix` 下面有所有的`unix domain socket`信息，比如我们需要的是`stetho`服务：
    `0000000000000000: 00000002 00000000 00010000 0001 01 3890307 @stetho_com.freedom2_devtools_remote`
    - Path字段前面有@符号的表示它是一个`ABSTRACT`类型的`socket`
    
4. 转发端口: `adb forward tcp:9224 localabstract:stetho_com.freedom2_devtools_remote`
5. 测试: `curl localhost:9224/json`, 拿到返回值
```
[{
    type: "app",
    title: "Freedom2 (powered by Stetho)",
    id: "1",
    description: "",
    webSocketDebuggerUrl: "ws:///inspector",
    devtoolsFrontendUrl: "http://chrome-devtools-frontend.appspot.com/serve_rev/@188492/devtools.html?ws=%2Finspector"
}]
```
返回一个数组，里面是所有可以远程调试的页面，其中包含以下字段信息：
- description： 页面信息描述
- devtoolsFrontendUrl：调试url地址，这个基于chrome 云服务器提供的inspector来调试，你也可以用开源的chromium中的inspector或者自己网上找下有人提取出来的调试server。
- id：页面id
- webSocketDebuggerUrl：android webview debug server 的 websocket， 这个地址的host和端口号是根据当前的访问tcp链接动态生成的，因为unix domain socket是没有ip地址和端口的

#### 补充说明
PC端的应用与手机端应用通信建立的过程：
- （1）执行`adb forward tcp:9224 localabstract:stetho_com.freedom2_devtools_remote`
- （2）启动手机端应用，建立`localabstract:stetho_com.freedom2_devtools_remote`的服务，并处于监听状态（LISTENING）
- （3）启动PC端应用，连接端口为`9224`的server（`adb`创建的）
之后，就可以传输数据了。

PC端的应用与手机端应用之间传输数据的过程：
- （1）PC端应用将数据发送给端口为`9224`的server（adb创建的）
- （2）`adb`将数据转发给手机端`adbd`进程（通过USB传输）
- （3）`adbd`进程将数据发送给`localabstract:stetho_com.freedom2_devtools_remote`（手机端app的`stetho`创建的）
-  传递是双向的，第（1）和第（3）步是通过socket实现的，所以通过socket的读和写就完成了PC端应用和手机端应用的数据传递。

#### adb相关概念
- `adb devices`看到的是所有启动了`adbd`服务的设备
- 一旦`adb`服务启动，会自动连接所有`adbd`服务，这样`adb`端就可以控制所有连接的移动设备


#### 参考
- [stetho通信原理](https://cloud.tencent.com/developer/article/1145376)
- [Chrome远程调试Android](https://www.fed123.com/pwa/2105.html)
- [Android adb](https://developer.android.com/studio/command-line/adb)

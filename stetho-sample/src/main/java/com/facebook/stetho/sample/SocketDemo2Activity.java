package com.facebook.stetho.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.facebook.stetho.server.LeakyBufferedInputStream;
import com.facebook.stetho.server.http.LightHttpServer;
import com.facebook.stetho.websocket.Frame;
import com.facebook.stetho.websocket.FrameHelper;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Jin on 2020/11/18.
 * Description
 */
public class SocketDemo2Activity extends Activity {
	
	private LocalSocket localSocket;
	private int commandIndex = 1;
	private List<String> commandList;
	
	private void handshake(OutputStream out) throws IOException {
		LightHttpServer.HttpMessageWriter writer = new LightHttpServer.HttpMessageWriter(new BufferedOutputStream(out));
		writer.writeLine("GET /inspector HTTP/1.1");
		writer.writeLine("Connection: Upgrade");
		writer.writeLine("Host: 127.0.0.1:5037");
		writer.writeLine("Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits");
		writer.writeLine("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==");
		writer.writeLine("Sec-WebSocket-Version: 13");
		writer.writeLine("Upgrade: WebSocket");
		writer.writeLine();
		writer.flush();
		Log.i("247144", "发送到服务端去————>/inspector handshake");
	}
	
	private void initMonitor(OutputStream out) throws IOException {
		BufferedOutputStream mBufferedOutputStream = new BufferedOutputStream(out);
		for (int i = 0; i < commandList.size(); i++) {
			String command = getCommand(i);
			Frame frame = FrameHelper.createTextFrame(command);
			frame.writeTo(mBufferedOutputStream);
			Log.i("247144", "发送到服务端去————>command::" + command);
		}
		mBufferedOutputStream.flush();
	}
	
	private String getCommand(int index) {
		Command command = new Command();
		command.id = commandIndex++;
		command.method = commandList.get(index);
		return new Gson().toJson(command);
	}
	
	private void sendJsonStr(OutputStream out, String str) throws IOException {
		BufferedOutputStream mBufferedOutputStream = new BufferedOutputStream(out);
		Frame frame = FrameHelper.createTextFrame(str);
		frame.writeTo(mBufferedOutputStream);
		mBufferedOutputStream.flush();
		Log.i("247144", "发送到服务端去————>command::" + str);
	}
	
	/**
	 * {"id":1,"method":"Page.canScreencast"}
	 * {"id":2,"method":"Page.canEmulate"}
	 * {"id":3,"method":"Worker.canInspectWorkers"}
	 * {"id":4,"method":"Console.setTracingBasedTimeline","params":{"enabled":true}}
	 * {"id":5,"method":"Console.enable"}
	 * {"id":6,"method":"Network.enable"}
	 * {"id":7,"method":"Page.enable"}
	 * {"id":8,"method":"Page.getResourceTree"}
	 * {"id":9,"method":"Debugger.enable"}
	 * {"id":10,"method":"Debugger.setPauseOnExceptions","params":{"state":"none"}}
	 * {"id":11,"method":"Debugger.setAsyncCallStackDepth","params":{"maxDepth":0}}
	 * {"id":12,"method":"Debugger.skipStackFrames","params":{"script":"","skipContentScripts":false}}
	 * {"id":13,"method":"Runtime.enable"}
	 * {"id":14,"method":"DOM.enable"}
	 * {"id":15,"method":"CSS.enable"}
	 * {"id":16,"method":"Worker.enable"}
	 * {"id":17,"method":"Timeline.enable"}
	 * {"id":18,"method":"Database.enable"}
	 * {"id":19,"method":"DOMStorage.enable"}
	 * {"id":20,"method":"Profiler.enable"}
	 * {"id":21,"method":"Profiler.setSamplingInterval","params":{"interval":1000}}
	 * {"id":22,"method":"IndexedDB.enable"}
	 * {"id":23,"method":"Page.getNavigationHistory"}
	 * {"id":24,"method":"Page.startScreencast","params":{"format":"jpeg","quality":80,"maxWidth":1261,"maxHeight":857}}
	 * {"id":25,"method":"Page.stopScreencast"}
	 * {"id":26,"method":"Worker.setAutoconnectToWorkers","params":{"value":true}}
	 * {"id":27,"method":"Inspector.enable"}
	 * {"id":28,"method":"Page.setShowViewportSizeOnResize","params":{"show":true,"showGrid":false}}
	 * {"id":29,"method":"IndexedDB.requestDatabaseNames","params":{"securityOrigin":""}}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo2_activity);
		
		commandList = new ArrayList<>();
		commandList.add("Page.canScreencast");
		commandList.add("Page.canEmulate");
		commandList.add("Worker.canInspectWorkers");
		commandList.add("Console.enable");
		commandList.add("Network.enable");
		commandList.add("Page.enable");
		commandList.add("Page.getResourceTree");
		commandList.add("Debugger.enable");
		commandList.add("Runtime.enable");
		commandList.add("DOM.enable");
		commandList.add("CSS.enable");
		commandList.add("Worker.enable");
		commandList.add("Timeline.enable");
		commandList.add("Database.enable");
		commandList.add("DOMStorage.enable");
		commandList.add("Profiler.enable");
		commandList.add("IndexedDB.enable");
		commandList.add("Inspector.enable");
		
		OkHttpClient client =  new OkHttpClient.Builder()
				.addNetworkInterceptor(new StethoInterceptor())
				.build();
		
		final Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("https://www.huaxiaotech.cn/")
				.addConverterFactory(GsonConverterFactory.create())
				.client(client)
				.build();
		
		findViewById(R.id.request).setOnClickListener(v -> {
			GitHubService service = retrofit.create(GitHubService.class);
			service.getData().enqueue(new Callback<String>() {
				@Override
				public void onResponse(Call<String> call, Response<String> response) {
					Log.i("msgTag", response.body() + "  -----onResponse--");
				}
				
				@Override
				public void onFailure(Call<String> call, Throwable t) {
					Log.i("msgTag", call.toString() + "  -----onFailure--");
				}
			});
		});
		
		findViewById(R.id.init_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						super.run();
						
						try {
							localSocket = new LocalSocket();
							localSocket.connect(new LocalSocketAddress("stetho_com.facebook.stetho.sample4_devtools_remote"));
							
							handshake(localSocket.getOutputStream());
							
							LeakyBufferedInputStream leakyIn = new LeakyBufferedInputStream(
									localSocket.getInputStream(),
									256);
							LeakyBufferedInputStream input = new LeakyBufferedInputStream(leakyIn.leakBufferAndStream(), 1024);
							LightHttpServer.HttpMessageReader reader = new LightHttpServer.HttpMessageReader(input);
							String handshakeResult = reader.readLine();
							while (handshakeResult != null && handshakeResult != "") {
								handshakeResult = reader.readLine();
								Log.i("247144", "handshake Result:" + handshakeResult);
							}
							
							initMonitor(localSocket.getOutputStream());
							
							BufferedInputStream mBufferedInput = new BufferedInputStream(localSocket.getInputStream(), 1024);
							Frame frame = new Frame();
							
							new Thread() {
								@Override
								public void run() {
									super.run();
									
									while(true) {
										try {
											frame.readFrom(mBufferedInput);
											String result = new String(frame.payloadData, 0, (int)frame.payloadLen);
											Log.i("247144", "result......" + result);
										} catch (Exception e) {
											Log.i("247144", "Exception????......");
											e.printStackTrace();
										}
									}
								}
							}.start();
							
							Log.i("247144", "end......");
						} catch (IOException e) {
							Log.i("247144", "IOException------");
							e.printStackTrace();
						}
					}
				}.start();
			}
		});

		findViewById(R.id.invoke_btn).setOnClickListener(v -> {
			try {
				sendJsonStr(localSocket.getOutputStream(), "{\"id\":26,\"method\":\"DOM.getDocument\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		findViewById(R.id.jump).setOnClickListener(v -> {
			startActivity(new Intent(SocketDemo2Activity.this, SecondActivity.class));
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (localSocket != null) {
			try {
				localSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

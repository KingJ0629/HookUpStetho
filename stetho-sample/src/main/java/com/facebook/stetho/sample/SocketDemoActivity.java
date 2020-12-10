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
import com.facebook.stetho.server.http.HttpHeaders;
import com.facebook.stetho.server.http.LightHttpServer;
import com.facebook.stetho.websocket.Frame;
import com.facebook.stetho.websocket.FrameHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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
public class SocketDemoActivity extends Activity {
	
	private LocalSocket localSocket;
	
	private void sendVersion(OutputStream out) throws IOException {
		LightHttpServer.HttpMessageWriter writer = new LightHttpServer.HttpMessageWriter(new BufferedOutputStream(out));
		writer.writeLine("GET /json/version HTTP/1.1");
		writer.writeLine("Host: 127.0.0.1:5037");
		writer.writeLine();
		writer.flush();
		Log.i("247144", "发送到服务端去————>/json/version");
	}
	
	private void sendJson(OutputStream out) throws IOException {
		LightHttpServer.HttpMessageWriter writer = new LightHttpServer.HttpMessageWriter(new BufferedOutputStream(out));
		writer.writeLine("GET /json HTTP/1.1");
		writer.writeLine("Host: 127.0.0.1:5037");
		writer.writeLine();
		writer.flush();
		Log.i("247144", "发送到服务端去————>/json");
	}
	
	
	private void sendJson2(OutputStream out) throws IOException {
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
		Log.i("247144", "发送到服务端去————>/json2");
	}
	
	private void sendJson3(BufferedOutputStream out) throws IOException {
		Frame frame = FrameHelper.createTextFrame("{\"id\":1,\"method\":\"DOM.enable\"}");
		frame.writeTo(out);
		out.flush();
		Log.i("247144", "发送到服务端去————>/json3");
	}
	
	private void sendJsonStr(OutputStream out, String str) throws IOException {
		BufferedOutputStream mBufferedOutputStream = new BufferedOutputStream(out);
		Frame frame = FrameHelper.createTextFrame(str);
		frame.writeTo(mBufferedOutputStream);
		mBufferedOutputStream.flush();
		Log.i("247144", "发送到服务端去————>/json str::" + str);
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_activity);
		
		OkHttpClient client =  new OkHttpClient.Builder()
				.addNetworkInterceptor(new StethoInterceptor())
				.build();
		
		final Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("https://www.huaxiaotech.cn/")
				.addConverterFactory(GsonConverterFactory.create())
				.client(client)
				.build();
		
		findViewById(R.id.apod_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
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
				
				try {
					sendJsonStr(localSocket.getOutputStream(), "{\"id\":31,\"method\":\"Network.enable\"}");
//					sendJsonStr(localSocket.getOutputStream(), "{\"id\":32,\"method\":\"Network.getResponseBody\",\"params\":{\"requestId\":\"1\"}}");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
			}
		});
		
		findViewById(R.id.settings_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						super.run();
						
						try {
							localSocket = new LocalSocket();
							localSocket.connect(new LocalSocketAddress("stetho_com.facebook.stetho.sample4_devtools_remote"));
							
							sendVersion(localSocket.getOutputStream());
							
							LeakyBufferedInputStream leakyIn = new LeakyBufferedInputStream(
									localSocket.getInputStream(),
									256);
							
							LeakyBufferedInputStream input = new LeakyBufferedInputStream(leakyIn.leakBufferAndStream(), 1024);
							LightHttpServer.HttpMessageReader reader = new LightHttpServer.HttpMessageReader(input);
							String mLine = reader.readLine();
							
							InputStream inputStream = localSocket.getInputStream();
							
							Map<String, String> map = new HashMap<>();
							while (mLine != null && mLine != "") {
								mLine = reader.readLine();
								String[] arrayLine = mLine.split(": ");
								if (arrayLine.length == 2) {
									map.put(arrayLine[0], arrayLine[1]);
								}
								Log.i("247144", "version mLine:" + mLine);
							}
							
							readFile(inputStream, Integer.parseInt(map.get(HttpHeaders.CONTENT_LENGTH)));
							
							sendJson(localSocket.getOutputStream());
							mLine = reader.readLine();
							
							Map<String, String> map2 = new HashMap<>();
							while (mLine != null && mLine != "") {
								mLine = reader.readLine();
								String[] arrayLine = mLine.split(": ");
								if (arrayLine.length == 2) {
									map2.put(arrayLine[0], arrayLine[1]);
								}
								Log.i("247144", "json mLine:" + mLine);
							}
							
							readFile(inputStream, Integer.parseInt(map2.get(HttpHeaders.CONTENT_LENGTH)));
							
							sendJson2(localSocket.getOutputStream());
							
							mLine = reader.readLine();
							while (mLine != null && mLine != "") {
								mLine = reader.readLine();
								Log.i("247144", "json mLine:" + mLine);
							}
							
							sendJsonStr(localSocket.getOutputStream(), "{\"id\":1,\"method\":\"DOM.enable\"}");
							
							BufferedInputStream mBufferedInput = new BufferedInputStream(localSocket.getInputStream(), 1024);
							Frame frame = new Frame();
							ByteArrayOutputStream mCurrentPayload = new ByteArrayOutputStream();
							
							new Thread() {
								@Override
								public void run() {
									super.run();
									
									while(true) {
										try {
											frame.readFrom(mBufferedInput);
										} catch (Exception e) {
											Log.i("247144", "Exception????......");
											e.printStackTrace();
										}
										mCurrentPayload.write(frame.payloadData, 0, (int)frame.payloadLen);
										byte[] completePayload = mCurrentPayload.toByteArray();
										String result = new String(completePayload, 0, completePayload.length);
										mCurrentPayload.reset();
										Log.i("247144", "result......" + result);
									}
								}
							}.start();
							
							sendJsonStr(localSocket.getOutputStream(), "{\"id\":26,\"method\":\"DOM.getDocument\"}");
							sendJsonStr(localSocket.getOutputStream(), "{\"id\":24,\"method\":\"Page.startScreencast\",\"params\":{\"format\":\"jpeg\",\"quality\":80,\"maxWidth\":1261,\"maxHeight\":857}}");
							sendJsonStr(localSocket.getOutputStream(), "{\"id\":25,\"method\":\"Page.stopScreencast\"}");
							
							Log.i("247144", "end......");
						} catch (IOException e) {
							Log.i("247144", "IOException------");
							e.printStackTrace();
						}
					}
				}.start();
			}
		});

		findViewById(R.id.irc_btn).setOnClickListener(v -> {
			try {
				sendJsonStr(localSocket.getOutputStream(), "{\"id\":26,\"method\":\"DOM.getDocument\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		findViewById(R.id.jump).setOnClickListener(v -> {
			startActivity(new Intent(SocketDemoActivity.this, SecondActivity.class));
		});
	}
	
	public void readFile(InputStream input, int length) throws IOException {
		// 创建一个FileInputStream对象:
		StringBuffer sb = new StringBuffer();
		for (int i = 0;i < length; i++) {
			int n = input.read(); // 反复调用read()方法，直到返回-1
			sb.append((char)n);
		}
		Log.i("247144", sb.toString()); // 打印byte的值
	}
}

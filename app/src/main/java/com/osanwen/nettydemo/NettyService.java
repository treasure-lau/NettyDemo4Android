package com.osanwen.nettydemo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import timber.log.Timber;

/**
 *
 * Created by LiuSaibao on 11/17/2016.
 */
public class NettyService extends Service implements NettyListener {

    private NetworkReceiver receiver;
    private static String sessionId = null;

    private ScheduledExecutorService mScheduledExecutorService;
    private void shutdown() {
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdown();
            mScheduledExecutorService = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new NetworkReceiver();
        IntentFilter filter=new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        // 自定义心跳，每隔20秒向服务器发送心跳包
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                byte[] requestBody = {(byte) 0xFE, (byte)0xED, (byte)0xFE, 5};
                NettyClient.getInstance().sendMsgToServer(requestBody, new ChannelFutureListener() {    //3
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {                //4
                            Timber.d("Write heartbeat successful");

                        } else {
                            Timber.e("Write heartbeat error");
                            WriteLogUtil.writeLogByThread("heartbeat error");
                        }
                    }
                });
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NettyClient.getInstance().setListener(this);
        connect();
        return START_NOT_STICKY;
    }

    @Override
    public void onServiceStatusConnectChanged(int statusCode) {		//连接状态监听
        Timber.d("connect status:%d", statusCode);
        if (statusCode == NettyListener.STATUS_CONNECT_SUCCESS) {
            authenticData();
        } else {
            WriteLogUtil.writeLogByThread("tcp connect error");
        }
    }

    /**
     * 认证数据请求
     */
    private void authenticData() {
        AuthModel auth = new AuthModel();
        auth.setI(1);
        auth.setU("sn");
        auth.setN("name");
        auth.setF("1");
        auth.setT((int)(System.currentTimeMillis() / 1000));
        byte[] content = RequestUtil.getEncryptBytes(auth);
        byte[] requestHeader = RequestUtil.getRequestHeader(content, 1, 1001);
        byte[] requestBody = RequestUtil.getRequestBody(requestHeader, content);
        NettyClient.getInstance().sendMsgToServer(requestBody, new ChannelFutureListener() {    //3
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {                //4
                    Timber.d("Write auth successful");
                } else {
                    Timber.d("Write auth error");
                    WriteLogUtil.writeLogByThread("tcp auth error");
                }
            }
        });
    }

    @Override
    public void onMessageResponse(ByteBuf byteBuf) {
        byte[] bytes = byteBuf.array();
        Timber.d("tcp receive data:%s", ByteUtil.bytesToHex(bytes));
        // 接收
        if (0xED == ByteUtil.unsignedByteToInt(bytes[0])
                && 0xFE == ByteUtil.unsignedByteToInt(bytes[1])) {
            if (1 == bytes[2]) {
                int cardinal = (int)ByteUtil.unsigned4BytesToInt(bytes, 5);
                int realLen = cardinal + 9;
                int len = byteBuf.writerIndex();
                // 接收到的数据有可能会粘包，只需要判断数据的长度大于或者等于真实的长度即可
                if (len >= realLen) {

                    int word = ByteUtil.bytesToShort(ByteUtil.subBytes(bytes, 3, 2));
                    if (word == 1001) {
                        byte[] data = new byte[cardinal];
                        System.arraycopy(bytes, 9, data, 0, data.length);
                        Blowfish blowfish = new Blowfish();
                        String result = new String(blowfish.decryptByte(data));
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            sessionId = jsonObject.getString("s");
                        } catch (JSONException e) {
                            Timber.e(e, e.getMessage());
                        }
                    } else if (word == 2002) {
                        byte[] data = new byte[cardinal];
                        System.arraycopy(bytes, 9, data, 0, data.length);
                        Blowfish blowfish = new Blowfish();
                        String result = new String(blowfish.decryptByte(data));
                        Timber.d(result);
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            handle(word, jsonObject.getInt("i"), jsonObject.getInt("r"));
                        } catch (JSONException e) {
                            Timber.e(e, e.getMessage());
                        }
                    } else {
                        String log = "undefined request type";
                        Timber.e(log);
                        WriteLogUtil.writeLogByThread(log);
                    }
                } else {
                    String log = String.format("request byte array content length inequality, realLen=%d, len=%d", realLen, len);
                    Timber.e(log);
                    WriteLogUtil.writeLogByThread(log);
                }
            } else if (5 == bytes[2]) {
                Timber.e("heartbeat");
            }

            // 响应
        } else if (0xFE == ByteUtil.unsignedByteToInt(bytes[0])
                && 0xED == ByteUtil.unsignedByteToInt(bytes[1])
                && 0xFE == ByteUtil.unsignedByteToInt(bytes[2])) {
            if (1 == bytes[3]) {
                // 忽略bytes[4],bytes[5]。作用是接口升级
                int cardinal = (int)ByteUtil.unsigned4BytesToInt(bytes, 8);
                int len = byteBuf.writerIndex();
                // 前12个字节是请求头，后4个字节是校验值
                int realLen = cardinal + 12 + 4;
                // 返回的数据有可能会粘包，只需要判断数据的长度大于或者等于真实的长度即可
                if (len >= realLen) {
                    int word = ByteUtil.bytesToShort(ByteUtil.subBytes(bytes, 6, 2));
                    if (word == 2001) {
                        byte[] data = new byte[cardinal];
                        System.arraycopy(bytes, 12, data, 0, data.length);
                        byte[] crc32 = new byte[4];
                        System.arraycopy(bytes, realLen - 4, crc32, 0, crc32.length);

                        // 对内容进行CRC校验
                        if (CRC32Util.getCRC32Long(data) == ByteUtil.unsigned4BytesToInt(crc32, 0)) {
                            Blowfish blowfish = new Blowfish();
                            String result = new String(blowfish.decryptByte(data));
                            try {
                                JSONObject jsonObject = new JSONObject(result);
                                int i = jsonObject.getInt("i");
                                if (sessionId == null) {
                                    WriteLogUtil.writeLogByThread("sessionId is null");
                                    authenticData();
                                    handle(word, i, 0);
                                    return;
                                }
                                byte[] session = sessionId.getBytes();
                                byte[] sign = "WiseUC@2016".getBytes();
                                byte[] content = new byte[session.length + sign.length];
                                System.arraycopy(session, 0, content, 0, session.length);
                                System.arraycopy(sign, 0, content, session.length, sign.length);

                                // 对Session ID进行CRC校验
                                if (jsonObject.getLong("c") == CRC32Util.getCRC32(content)) {
                                    handle(word, i, 1);
                                } else {
                                    String log = "open the door session id crc32 verification failure";
                                    Timber.e(log);
                                    WriteLogUtil.writeLogByThread(log);
                                }
                            } catch (JSONException e) {
                                Timber.e(e, e.getMessage());
                            }
                        } else {
                            String log = "open the door crc32 data verification failure";
                            Timber.e(log);
                            WriteLogUtil.writeLogByThread(log);
                        }
                    } else {
                        String log = "undefined response type";
                        Timber.e(log);
                        WriteLogUtil.writeLogByThread(log);
                    }
                } else {
                    String log = String.format("response byte array content length inequality, realLen=%d, len=%d", realLen, len);
                    Timber.e(log);
                    WriteLogUtil.writeLogByThread(log);
                }
            } else if (5 == bytes[3]) {
                Timber.e("heartbeat");
            }
        } else {
            Timber.e("unknown");
            WriteLogUtil.writeLogByThread("unknown");
        }
    }

    private void handle(int t, int i, int f) {
        // TODO 实现自己的业务逻辑
    }

    private void connect(){
        if (!NettyClient.getInstance().getConnectStatus()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NettyClient.getInstance().connect();//连接服务器
                }
            }).start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        shutdown();
        NettyClient.getInstance().setReconnectNum(0);
        NettyClient.getInstance().disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) { // connected to the internet
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI
                        || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    connect();
                }
            }
        }
    }
}

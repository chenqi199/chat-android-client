package com.taoyiyi.chat.client;


import android.os.Handler;
import android.util.Log;

import com.example.newbrain.ChatUiActivity;
import com.taoyiyi.chat.proto.DeviceAuthReq;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ChatClient {

    private static final int HEART_TIME = 30;
    private static final int MAX_HEAT_LOST = 3;
    private int lostHeart = 0;
    private volatile long lastHeartTime = 0L;
    private volatile long lastCheckTime = 0L;
    public String mAccessToken;
    private static ChatClient instance;
    private boolean deviceNoRight = false;
    private final NettyClient socketClient;
    public MsgSendThread msgSender;

    public Handler mHandler;

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public static ChatClient get() {
        if (instance == null) {
            synchronized (ChatClient.class) {
                if (instance == null) {
                    instance = new ChatClient();
                }
            }
        }
        return instance;
    }


    private ChatClient() {
        this.socketClient = NettyClient.getClient();
        this.msgSender = this.socketClient.getMsgSender();
        startWxHeart();
    }

    public boolean isConnected() {
        return this.socketClient.isConnect();
    }

    private boolean isAccredit() {
        Log.e("socketClient==" , "");
        if (!this.socketClient.isConnect()) {

            connect();
            return false;
        }
        return this.msgSender.isAccredit();
    }

    public void connect() {

        if (!this.socketClient.isConnect())
            this.socketClient.connect();
    }

    public void closeConnect() {
        this.socketClient.close();


    }

    public void checkConnect() {
        if (!isConnected()) {
            connect();
        }
    }




    public void deviceAuth() {

        DeviceAuthReq.DeviceAuthReqMessage.Builder builder = DeviceAuthReq.DeviceAuthReqMessage.newBuilder()
                .setCredential("U18ce81029b4XslwF8B5t") //
                .setAuthType(DeviceAuthReq.DeviceAuthReqMessage.EnumAuthType.UserCode);
        this.msgSender.sendMsgSync(TransportMessageOuterClass.EnumMsgType.DeviceAuthReq,  builder.build(), true);

    }

    public void regChannel( String userCode) {
        System.out.println("开始注册通道：ucode:{}"+userCode);
        DeviceAuthReq.DeviceAuthReqMessage.Builder builder = DeviceAuthReq.DeviceAuthReqMessage.newBuilder()
                .setCredential(userCode) //
                .setAuthType(DeviceAuthReq.DeviceAuthReqMessage.EnumAuthType.UserCode);
        Optional.ofNullable(userCode).ifPresent(builder::setCredential);
        this.msgSender.sendMsgSync(TransportMessageOuterClass.EnumMsgType.DeviceAuthReq,  builder.build(), true);

    }


    public void startWxHeart() {
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        scheduledExecutorService.scheduleAtFixedRate(new PcTimerTask(), 1L, 1L, TimeUnit.MINUTES);
    }

    private void heartBeat() {
        if (!isAccredit()) {
            return;
        }



        this.lastHeartTime = System.currentTimeMillis();
        this.msgSender.sendMsgSync(TransportMessageOuterClass.EnumMsgType.HeartBeatReq, null);
        this.lostHeart++;
    }


    public class PcTimerTask
            implements Runnable {
        public void run() {
            ChatClient.this.heartBeat();
        }
    }
}

package com.taoyiyi.chat.client;


import android.util.Log;

import com.example.newbrain.TestLocalMsgConstant;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class MsgSendThread {

    private final BlockingQueue<ProtobufMsg> sendMsgList;
    private NettyClient client;
    private long msgId;
    private String accessToken;
    private volatile boolean isRunning = false;
    private SendTread sender;
    private final Object mLocked = new Object();

    public MsgSendThread(NettyClient client) {
        this.client = client;
        this.msgId = 0L;
        this.accessToken = null;
        this.sendMsgList = new LinkedBlockingQueue<>();
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        if (accessToken == null) {
            return;
        }
        synchronized (this.mLocked) {
            this.mLocked.notify();
        }
    }


    public boolean sendMsgSync(TransportMessageOuterClass.EnumMsgType msgType, Message content) {

        if (this.client.isConnect()) {
            TransportMessageOuterClass.TransportMessage.Builder builder = TransportMessageOuterClass.TransportMessage.newBuilder().setMsgType(msgType).setId(this.msgId++).setAccessToken(this.accessToken);
            if (content != null) {
                builder.setContent(Any.pack(content, "TYY"));
            }
            int ret = this.client.sendMsg(builder.build());
            System.out.println("SyncMsg " + msgType + " " + this.msgId);
            return (ret > 0);
        }
        return false;
    }

    public boolean sendMsgSync(TransportMessageOuterClass.EnumMsgType msgType, Message content, boolean idReset) {

        if (this.client.isConnect()) {
            if (idReset) {
                this.msgId = 0L;
            }
            TransportMessageOuterClass.TransportMessage.Builder builder = TransportMessageOuterClass.TransportMessage.newBuilder().setMsgType(msgType).setId(this.msgId++);
            if (content != null) {
                builder.setContent(Any.pack(content, "TYY"));
            }
            int ret = this.client.sendMsg(builder.build());
            return (ret > 0);
        }
        return false;
    }


    public long addMsgToSendList(TransportMessageOuterClass.EnumMsgType msgType, Message content) {
        return addMsgToSendList(msgType, content, false);
    }


    // 批量标记场景
    public long addMsgToSendList(TransportMessageOuterClass.EnumMsgType msgType, Message content, boolean insert) {


        ProtobufMsg msg = new ProtobufMsg(msgType, content, insert ? ++msgId : msgId);
        try {
            sendMsgList.put(msg);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return msgId;
    }

    public boolean isAccredit() {
        return true;
    }

    private int sendMsg(ProtobufMsg message) {
        TransportMessageOuterClass.TransportMessage.Builder builder = TransportMessageOuterClass.TransportMessage.newBuilder();
        builder.setMsgType(message.msgType).setAccessToken(this.accessToken).setId(message.msgId);
        if (message.content != null) {
            builder.setContent(Any.pack(message.content, "TYY"));
        }
        TransportMessageOuterClass.TransportMessage req = builder.build();
        int ret = this.client.sendMsg(req);
        return ret;
    }

    public void startRun() {
        if (!this.isRunning) {
            this.sender = new SendTread();
            this.sender.start();
        } else {
            Log.e("startRun", "Sender is runing");
        }
    }

    public void stopRun() {
        synchronized (this.mLocked) {
            this.accessToken = null;
            if (this.sender != null) {
                this.sender.interrupt();
            }
            this.mLocked.notify();
        }
        this.sender = null;
    }

    class SendTread extends Thread {
        public void run() {

            MsgSendThread.this.isRunning = true;

            while (!isInterrupted()) {
                try {
                    ProtobufMsg message = sendMsgList.take();
                    // 发送消息的逻辑代码
                    Log.i("Sending message batch: ", message.toString());
                    client.sendMsg(message);
                } catch (InterruptedException e) {
                    Log.e("sendThreadErr", e.getMessage());
                    // 处理线程中断
                    interrupt();
                }
            }
        }
    }
}

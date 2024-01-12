package com.taoyiyi.chat.client;

import android.util.Log;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.taoyiyi.chat.proto.DeviceAuthRsp;
import com.taoyiyi.chat.proto.ErrorMessageOuterClass;
import com.taoyiyi.chat.proto.TalkToOther;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


public class ReceiveHandle {

    private static final ReceiveHandle instance = new ReceiveHandle();

    public static ReceiveHandle getInstance() {
        return instance;
    }

    private Map<TransportMessageOuterClass.EnumMsgType, Consumer<Any>> consumerMap;


    public void handleMsgType(Any any, TransportMessageOuterClass.EnumMsgType msgType) {
        Consumer<Any> anyConsumer = consumerMap.get(msgType);
        if (Objects.isNull(anyConsumer)) {
            Log.e("handleMsgType ", msgType.name());
        }
        anyConsumer.accept(any);
    }

    public ReceiveHandle() {
        this.consumerMap = new HashMap<>();

        consumerMap.put(TransportMessageOuterClass.EnumMsgType.DeviceAuthRsp, this::deviceAuthRspCus);
        consumerMap.put(TransportMessageOuterClass.EnumMsgType.Error, this::errCus);
        consumerMap.put(TransportMessageOuterClass.EnumMsgType.OnlineNotice, this::errCus);
        consumerMap.put(TransportMessageOuterClass.EnumMsgType.TalkToOther, this::talkOther);
        consumerMap.put(TransportMessageOuterClass.EnumMsgType.HeartBeatReq, this::heartBat);
        consumerMap.put(TransportMessageOuterClass.EnumMsgType.MsgReceivedAck, this::msgReceivedAck);
    }


    public void deviceAuthRspCus(Any any) {
        DeviceAuthRsp.DeviceAuthRspMessage rspMessage = unpack(any, DeviceAuthRsp.DeviceAuthRspMessage.class);
        ChatClient.get().msgSender.setAccessToken(rspMessage.getAccessToken());
        // todo 发送上线事件 拉消息
        // todo 业务

    }

    public void errCus(Any any) {
        ErrorMessageOuterClass.ErrorMessage rspMessage = unpack(any, ErrorMessageOuterClass.ErrorMessage.class);

        // todo 业务

    }

    public void onlineNotice(Any any) {
        ErrorMessageOuterClass.ErrorMessage rspMessage = unpack(any, ErrorMessageOuterClass.ErrorMessage.class);

        // todo 业务

    }


    public void talkOther(Any any) {
        TalkToOther.TalkToOtherMessage rspMessage = unpack(any, TalkToOther.TalkToOtherMessage.class);
        String content = rspMessage.getContent().toStringUtf8();

        // 将消息发送到主线程
        android.os.Message message = ChatClient.get().mHandler.obtainMessage();
        message.obj = content;
        ChatClient.get().mHandler.sendMessage(message);
        // todo 业务

    }

    public void heartBat(Any any) {

        // todo 业务

    }

    public void msgReceivedAck(Any any) {

        // todo 业务

    }


    public <T extends Message> T unpack(Any any, Class<T> tClass) {

        try {
            T unpack = any.unpack(tClass);
            Log.i("unpack ", unpack.toString());
            return unpack;
        } catch (InvalidProtocolBufferException e) {
            Log.e("unpack , any err", e.getMessage());

            throw new RuntimeException(e);
        }

    }


}

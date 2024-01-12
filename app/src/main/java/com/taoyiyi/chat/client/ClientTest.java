package com.taoyiyi.chat.client;

import com.google.protobuf.ByteString;
import com.taoyiyi.chat.proto.DeviceAuthReq;
import com.taoyiyi.chat.proto.TalkToOther;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


public class ClientTest {



    public void user1Chat() {
        ChatClient.get().checkConnect();
//        ChatClient.get().regChannel("U18ce81029b4XslwF8B5f");// 注册一个连接通道
        sendMsg("U18ce81029b4XslwF8B5f","U18ce81029b4XslwF8B5t");
    }




    public void u2Chat() {
        ChatClient.get().checkConnect();
//        ChatClient.get().regChannel("U18ce81029b4XslwF8B5t");// 注册一个连接通道
        sendMsg("U18ce81029b4XslwF8B5t","U18ce81029b4XslwF8B5f");
    }




    public void sendMsg(String formUser, String toUser) {



        String userInput;
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                if ((userInput = stdIn.readLine()) == null) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                } else {

                    // 发送一个信息
                    TalkToOther.TalkToOtherMessage.Builder talkBuild = TalkToOther.TalkToOtherMessage.newBuilder().setToUser(toUser)
                            .setFormUser(formUser)
                            .setContentType(TransportMessageOuterClass.EnumContentType.Text)
                            .setContent(ByteString.copyFrom(userInput.getBytes(StandardCharsets.UTF_8)));
                    ChatClient.get().msgSender.sendMsgSync(TransportMessageOuterClass.EnumMsgType.TalkToOther, talkBuild.build(), true);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (userInput.equals("exit")) {
                break;
            }
        }

    }


}

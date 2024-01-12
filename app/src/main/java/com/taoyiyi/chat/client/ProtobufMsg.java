 package com.taoyiyi.chat.client;


 import com.google.protobuf.Message;
 import com.taoyiyi.chat.proto.TransportMessageOuterClass;


 public class ProtobufMsg
 {
   public TransportMessageOuterClass.EnumMsgType msgType;
   public Message content;
   public long msgId;
   
   public ProtobufMsg(TransportMessageOuterClass.EnumMsgType msgType, Message content, long msgId) {
     this.msgType = msgType;
     this.content = content;
     this.msgId = msgId;
   }
 }



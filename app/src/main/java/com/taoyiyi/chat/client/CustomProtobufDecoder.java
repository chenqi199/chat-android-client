package com.taoyiyi.chat.client;


import com.google.protobuf.MessageLite;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;


public class CustomProtobufDecoder
        extends ByteToMessageDecoder {
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() > 4) {
            byte[] array;
            int offset;
            in.markReaderIndex();


            byte b3 = in.readByte();
            byte b2 = in.readByte();
            byte b1 = in.readByte();
            byte b0 = in.readByte();

            int length = b0 & 0xFF | b1 << 8 & 0xFF00 | b2 << 16 & 0xFF0000 | b3 << 24 & 0xFF000000;


            if (in.readableBytes() < length) {
                in.resetReaderIndex();

                return;
            }

            ByteBuf bodyByteBuf = in.readBytes(length);


            int readableLen = bodyByteBuf.readableBytes();
            if (bodyByteBuf.hasArray()) {
                array = bodyByteBuf.array();
                offset = bodyByteBuf.arrayOffset() + bodyByteBuf.readerIndex();
            } else {
                array = new byte[readableLen];
                bodyByteBuf.getBytes(bodyByteBuf.readerIndex(), array, 0, readableLen);
                offset = 0;
            }


            MessageLite result = decodeBody(array, offset, readableLen);
            out.add(result);
        }
    }


    public MessageLite decodeBody(byte[] array, int offset, int length) throws Exception {
        return (MessageLite) TransportMessageOuterClass.TransportMessage.getDefaultInstance()
                .getParserForType().parseFrom(array, offset, length);
    }
}


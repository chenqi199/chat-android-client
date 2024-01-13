package com.taoyiyi.chat.client;


import android.util.Log;

import com.example.newbrain.TestLocalMsgConstant;
import com.google.protobuf.Any;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;


public class NettyClient {

    private static String host = "192.168.1.9";
    private static int port = 19888;
    private static final int RECONNECT_TIME = 5;
    private static NettyClient client;
    private ChannelHandlerContext channel;
    private MsgSendThread sendThread;
    private volatile Bootstrap mB;
    private volatile boolean isClosed;
    private volatile boolean isConnecting;
    private volatile EventLoopGroup workerGroup;

    public static NettyClient getClient() {
        if (client == null) {
            synchronized (NettyClient.class) {
                if (client == null) {
                    client = new NettyClient();
                }
            }
        }
        return client;
    }

    private NettyClient() {
        this.sendThread = new MsgSendThread(this);
        long l = System.currentTimeMillis();
        initBootstrap();
        Log.i("initBootstrap", "NettyClient: " + (System.currentTimeMillis() - l));
    }

    public MsgSendThread getMsgSender() {
        return this.sendThread;
    }

    public static void setHost(String host, int port, boolean isSave) {
        NettyClient.host = host;
        if (port != 0) {
            NettyClient.port = port;
        }
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    private void initBootstrap() {
        this.workerGroup = new NioEventLoopGroup();
        this.mB = new Bootstrap();
        this.mB.group(this.workerGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE).option(ChannelOption.TCP_NODELAY, Boolean.TRUE).option(ChannelOption.SO_SNDBUF, 32 * 1024)  // 设置发送缓冲大小
                .option(ChannelOption.SO_RCVBUF, 32 * 1024)       // 这是接收缓冲大小
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new CustomProtobufDecoder()).addLast(new CustomProtobufEncoder()).addLast(new NettyClientHandler());
                    }
                }).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(10000));
    }

    public void connect() {
        (new Thread() {
            public void run() {
                NettyClient.this.connectServer();
            }
        }).start();
    }

    public void connectServer() {
        try {
            connect(port, host);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        this.isClosed = true;
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
    }

    public void connect(int port, String host) throws Exception {
        Log.i("connect", "connect: start_port_host");
        synchronized (this) {
            if (this.channel != null && !this.channel.isRemoved()) {
                return;
            }
        }
        if (this.isConnecting) {
            return;
        }
        this.isConnecting = true;
        this.isClosed = false;
        try {
            ChannelFuture f = this.mB.connect(host, port).sync();
            f.addListener((ChannelFutureListener) future -> {
                if (future.isDone()) {
                    NettyClient.this.isConnecting = false;
                    if (future.isSuccess()) {
                        Log.i("  netty_connect", "connect: success");
                        ChannelPipeline pipeline = channel.pipeline();
                        ChannelHandlerContext ctx = pipeline.firstContext();
                        this.channel = ctx;
                        this.sendThread.startRun();
                        ChatClient.get().regChannel(TestLocalMsgConstant.fromUser);
                    } else {
                        future.channel().eventLoop().schedule(NettyClient.this::connectServer, 3L, TimeUnit.SECONDS);
                    }
                }
            });
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            Log.d("netty_connect_err", e.toString());
            TimeUnit.SECONDS.sleep(5L);
            this.isConnecting = false;
            connect();
        }
    }

    public boolean isConnect() {
        return (this.channel != null);
    }

    public int sendMsg(byte[] bytes) {
        ByteBuffer writeBuffer = ByteBuffer.wrap(bytes);
        if (this.channel != null) {
            this.channel.writeAndFlush(writeBuffer);
            return bytes.length;
        }
        return 0;
    }

    public int sendMsg(Object request) {

        Log.i("sendMsg request: {}", request.toString());
        if (this.channel != null) {
            this.channel.writeAndFlush(request);
            return 1;
        }
        return 0;
    }

    public class NettyClientHandler<T> extends ChannelHandlerAdapter {
        public void channelActive(ChannelHandlerContext ctx) {
            Log.i("channelActive", "channelActive");
            synchronized (NettyClient.client) {
                if (NettyClient.this.channel == null) {
                    NettyClient.this.channel = ctx;
                    NettyClient.this.sendThread.startRun();
                }
                NettyClient.this.isConnecting = true;
            }

        }

        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            synchronized (this) {
                if (ctx.equals(NettyClient.this.channel)) {
                    NettyClient.this.channel = null;
                    NettyClient.this.sendThread.stopRun();
                    if (!NettyClient.this.isClosed) {
                        ctx.channel().eventLoop().schedule(new Runnable() {
                            public void run() {
                                NettyClient.this.connectServer();
                            }
                        }, 5L, TimeUnit.SECONDS);
                    }
                    NettyClient.this.isConnecting = false;
                }
            }

        }

        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

            cause.printStackTrace();
            ctx.close();
        }

        public Map<TransportMessageOuterClass.EnumMsgType, BiConsumer<ChannelHandlerContext, Class<T>>> consumerMap = new HashMap<>();


        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            super.channelRead(ctx, msg);
            TransportMessageOuterClass.TransportMessage transportMessage = (TransportMessageOuterClass.TransportMessage) msg;
            Any any = transportMessage.getContent();
            TransportMessageOuterClass.EnumMsgType msgType = transportMessage.getMsgType();
            ReceiveHandle.getInstance().handleMsgType(any, msgType);
        }

        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() != IdleState.READER_IDLE)
                    if (event.state() == IdleState.WRITER_IDLE) {

                    } else if (event.state() == IdleState.ALL_IDLE) {
                    }
            }
        }
    }
}

package com.osanwen.nettydemo;


import io.netty.buffer.ByteBuf;

/**
 *
 * Created by LiuSaibao on 11/23/2016.
 */
public interface NettyListener {

    public final static byte STATUS_CONNECT_SUCCESS = 1;

    public final static byte STATUS_CONNECT_CLOSED = 0;

    public final static byte STATUS_CONNECT_ERROR = 0;


    /**
     * 当接收到系统消息
     */
    void onMessageResponse(ByteBuf byteBuf);

    /**
     * 当服务状态发生变化时触发
     */
    public void onServiceStatusConnectChanged(int statusCode);
}

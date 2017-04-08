package com.osanwen.nettydemo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import timber.log.Timber;

/**
 * Netty客户端
 * Created by LiuSaibao on 11/23/2016.
 */
public class NettyClient {

	private static NettyClient nettyClient = new NettyClient();

	private EventLoopGroup group;

	private NettyListener listener;

	private Channel channel;
	
	private boolean isConnect = false;

	private int reconnectNum = Integer.MAX_VALUE;

	private long reconnectIntervalTime = 5000;
	
	public static NettyClient getInstance(){
		return nettyClient;
	}
	
	public synchronized NettyClient connect() {
		if (!isConnect) {
			group = new NioEventLoopGroup();
			Bootstrap bootstrap = new Bootstrap().group(group)
					.option(ChannelOption.SO_KEEPALIVE,true)
					.channel(NioSocketChannel.class)
					.handler(new NettyClientInitializer(listener));
			
			try {
				bootstrap.connect(Const.HOST, Const.TCP_PORT).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture channelFuture) throws Exception {
						if (channelFuture.isSuccess()) {
							isConnect = true;
							channel = channelFuture.channel();
						} else {
							isConnect = false;
						}
					}
				}).sync();

			} catch (Exception e) {
				Timber.e(e, e.getMessage());
				listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_ERROR);
				reconnect();
			}
		}
		return this;
	}

	public void disconnect() {
		group.shutdownGracefully();
	}

	public void reconnect() {
		if(reconnectNum >0 && !isConnect){
			reconnectNum--;
			try {
				Thread.sleep(reconnectIntervalTime);
			} catch (InterruptedException e) {}
			Timber.e("重新连接");
			disconnect();
			connect();
		}else{
			disconnect();
		}
	}

	public boolean sendMsgToServer(byte[] data, ChannelFutureListener listener) {
		boolean flag = channel != null && isConnect;
		if (flag) {
			ByteBuf buf = Unpooled.copiedBuffer(data);
			channel.writeAndFlush(buf).addListener(listener);
		}
		return flag;
	}

	public void setReconnectNum(int reconnectNum) {
		this.reconnectNum = reconnectNum;
	}

	public void setReconnectIntervalTime(long reconnectIntervalTime) {
		this.reconnectIntervalTime = reconnectIntervalTime;
	}
	
	public boolean getConnectStatus(){
		return isConnect;
	}
	
	public void setConnectStatus(boolean status){
		this.isConnect = status;
	}
	
	public void setListener(NettyListener listener) {
		this.listener = listener;
	}
}

package com.osanwen.nettydemo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 *
 * Created by LiuSaibao on 11/23/2016.
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private NettyListener listener;
    public NettyClientHandler(NettyListener listener){
        this.listener = listener;
    }
    
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		NettyClient.getInstance().setConnectStatus(true);
		listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_SUCCESS);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		NettyClient.getInstance().setConnectStatus(false);
		listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_CLOSED);
		NettyClient.getInstance().reconnect();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
		listener.onMessageResponse(byteBuf);
	}

	/*@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE){
				ctx.close();
			}else if (event.state() == IdleState.WRITER_IDLE){
				try{
					ctx.channel().writeAndFlush("Chilent-Ping\r\n");
				} catch (Exception e){
					Timber.e(e.getMessage());
				}
			}
		}
		super.userEventTriggered(ctx, evt);
	}*/
}

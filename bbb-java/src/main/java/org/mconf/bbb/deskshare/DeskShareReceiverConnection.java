package org.mconf.bbb.deskshare;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.RtmpConnection;
import org.mconf.bbb.video.VideoReceiverConnection;
import org.red5.server.api.so.ISharedObjectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.RtmpDecoder;
import com.flazr.rtmp.RtmpEncoder;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.client.ClientHandshakeHandler;
import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.message.Command;
import com.flazr.rtmp.message.MessageType;
import com.flazr.rtmp.message.Video;

public abstract class DeskShareReceiverConnection extends RtmpConnection {

    @SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(VideoReceiverConnection.class);
    
	public DeskShareReceiverConnection(ClientOptions options, BigBlueButtonClient context) {
		super(options, context);
	}
	
	@Override
	protected ChannelPipelineFactory pipelineFactory() {
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
		        final ChannelPipeline pipeline = Channels.pipeline();
		        pipeline.addLast("handshaker", new ClientHandshakeHandler(options));
		        pipeline.addLast("decoder", new RtmpDecoder());
		        pipeline.addLast("encoder", new RtmpEncoder());
		        pipeline.addLast("handler", DeskShareReceiverConnection.this);
		        return pipeline;
			}
		};
	}
	
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        options.setArgs((Object[]) null);
        writeCommandExpectingResult(e.getChannel(), Command.connect(options));
        onConnected(this, e.getChannel());
	}
	
	@Override
	protected void onMultimedia(Channel channel, RtmpMessage message) {
		super.onMultimedia(channel, message);
		if (message.getHeader().getMessageType() == MessageType.VIDEO) {
			onVideo((Video) message);
		}
	}
	
	abstract protected void onVideo(Video video);
	abstract protected void onConnected(RtmpConnection handler, Channel channel);
}


package org.mconf.bbb.deskshare;

import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.RtmpConnection;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.so.IClientSharedObject;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.message.Video;
import com.flazr.util.Utils;

public class BbbDeskShareReceiver implements ISharedObjectListener {

	protected class DeskShareConnection extends DeskShareReceiverConnection {

		public DeskShareConnection(ClientOptions options,
				BigBlueButtonClient context) {
			super(options, context);
		}

		@Override
		protected void onVideo(Video video) {
			BbbDeskShareReceiver.this.onVideo(video);
		}

		@Override
		protected void onConnected(RtmpConnection handler, Channel channel) {
			BbbDeskShareReceiver.this.onConnected(handler, channel);
		}
	}
	
	private static final Logger log = LoggerFactory.getLogger(BbbDeskShareReceiver.class);

	private String streamName;
	private DeskShareConnection deskShareConnection;
	private IClientSharedObject deskShareSO;
	
	public BbbDeskShareReceiver(BigBlueButtonClient context) {
		ClientOptions opt = new ClientOptions();
		opt.setClientVersionToUse(Utils.fromHex("00000000"));
		opt.setHost(context.getJoinService().getApplicationService().getServerUrl());
		opt.setAppName("deskShare/");
		
		streamName = context.getJoinService().getJoinedMeeting().getRoom();		
		opt.setWriterToSave(null);
		opt.setStreamName(context.getJoinService().getJoinedMeeting().getConference());

		deskShareConnection = new DeskShareConnection(opt, context);
	}
	
	protected void onVideo(Video video) {
		log.debug("received video package: {}", video.getHeader().getTime());
	}
	
	protected void onConnected(RtmpConnection handler, Channel channel) {
		deskShareSO = deskShareConnection.getSharedObject(handler.getContext().getJoinService().getJoinedMeeting().getRoom()+"-deskSO", false);
		deskShareSO.addSharedObjectListener(this);
		deskShareSO.connect(channel);
	}
	
	public void start() {
		if (deskShareConnection != null)
			deskShareConnection.connect();
	}
	
	public void stop() {
		if (deskShareConnection != null)
			deskShareConnection.disconnect();
	}
		
	public String getStreamName() {
		return streamName;
	}

	@Override
	public void onSharedObjectConnect(ISharedObjectBase so) {
		log.debug("onSharedObjectConnect");
	}

	@Override
	public void onSharedObjectDisconnect(ISharedObjectBase so) {
		log.debug("onSharedObjectDisconnect");
	}

	@Override
	public void onSharedObjectUpdate(ISharedObjectBase so, String key,
			Object value) {
		log.debug("onSharedObjectUpdate");
	}

	@Override
	public void onSharedObjectUpdate(ISharedObjectBase so,
			IAttributeStore values) {
		log.debug("onSharedObjectUpdate");
	}

	@Override
	public void onSharedObjectUpdate(ISharedObjectBase so,
			Map<String, Object> values) {
		log.debug("onSharedObjectUpdate");
	}

	@Override
	public void onSharedObjectDelete(ISharedObjectBase so, String key) {
		log.debug("onSharedObjectDelete");
	}

	@Override
	public void onSharedObjectClear(ISharedObjectBase so) {
		log.debug("onSharedObjectClear");
	}

	@Override
	public void onSharedObjectSend(ISharedObjectBase so, String method,
			List<?> params) {
		log.debug("onSharedObjectSend: method "+method);
		if (method.equals("startViewing")) {
			log.info("Desk share started sharing");
		} else if (method.equals("deskshareStreamStopped")) {
			log.info("Desk share stopped sharing");
		}
	}
}

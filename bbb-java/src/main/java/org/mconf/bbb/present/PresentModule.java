package org.mconf.bbb.present;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.channel.Channel;
import org.mconf.bbb.MainRtmpConnection;
import org.mconf.bbb.Module;
import org.mconf.bbb.api.ApplicationService;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.so.IClientSharedObject;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.message.Command;
import com.flazr.rtmp.message.CommandAmf0;

public class PresentModule extends Module implements ISharedObjectListener {
	private static final Logger log = LoggerFactory.getLogger(PresentModule.class);

	private final IClientSharedObject presentationSO;
	
	private String presentationName;
	private int slideNumber;
	
	public PresentModule(MainRtmpConnection handler, Channel channel) {
		super(handler, channel);
		
		presentationSO = handler.getSharedObject("presentationSO", false);
		presentationSO.addSharedObjectListener(this);
		presentationSO.connect(channel);
	}
		
	public void doGetPresentationInfo() {
    	Command cmd = new CommandAmf0("presentation.getPresentationInfo", null);
    	handler.writeCommandExpectingResult(channel, cmd);
	}

	@SuppressWarnings("unchecked")
	public boolean onGetPresentationInfo(String resultFor, Command command) {
		if (resultFor.equals("presentation.getPresentationInfo")) {
			log.debug("onGetPresentationInfo");
			Map<String, Object> presentationInfo = (Map<String, Object>) command.getArg(0);
			// TODO parse out presentations and presenter info from presentationInfo object 
			// and store it somewhere
			Object[] presentations = (Object[]) presentationInfo.get("presentations");
			//String[] presentations = (String[]) presentationInfo.get("presentations");
			Map<String,Object> presentation = (Map<String,Object>) presentationInfo.get("presentation");
			Boolean sharing = (Boolean) presentation.get("sharing");
			if (sharing) {
				presentationName = (String) presentation.get("currentPresentation");
				slideNumber = ((Double) presentation.get("slide")).intValue()+1;
				// Problem loading slides here. When its a new room the slides aren't ready yet
				loadPresentation();
			}
			return true;
		}
		return false;
	}
	
	private void loadPresentation() {
		// GET slides xml
		String url = "http://"+handler.getContext().getJoinService().getApplicationService().getServerUrl();
		url = url + "/bigbluebutton/presentation/" 
				+ handler.getContext().getJoinService().getJoinedMeeting().getConference() + "/"
				+ handler.getContext().getJoinService().getJoinedMeeting().getRoom() + "/" 
				+ presentationName + "/slides";
		log.info("Loading presentation info for "+presentationName);
		String result;
		HttpClient client = new DefaultHttpClient();
		HttpGet method = new HttpGet(url);
		try {
			HttpResponse httpResponse = client.execute(method);
		
			if (httpResponse.getStatusLine().getStatusCode() == 500) {
				log.debug("Slides not ready yet. Waiting for conversion.");
			} else if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("HTTP GET {} return {}", url, httpResponse.getStatusLine().getStatusCode());
			} else {
				// TODO parse out the slide info from result rather than just guessing slide info (i.e. textfile/1)
				result = EntityUtils.toString(httpResponse.getEntity()).trim();

				loadSlideContent("thumbnail");
				loadSlideContent("textfile");
				loadSlideContent("slide");
			}
		} catch (ClientProtocolException pe) {
			log.error("Client Protocol Exception", pe);
		} catch (IOException ie) {
			log.error("IO Exception", ie);
		} catch (Exception e) { // there are undeclared exceptions thrown by client.execute
			log.error("Unknown exception", e);
		}
	}
		
	private void loadSlideContent(String contentType) {
		// GET thumbnail
		// GET textfile
		// GET slide
		String url = "http://" + handler.getContext().getJoinService().getApplicationService().getServerUrl();
		url = url + "/bigbluebutton/presentation/" 
				+ handler.getContext().getJoinService().getJoinedMeeting().getConference() + "/"
				+ handler.getContext().getJoinService().getJoinedMeeting().getRoom() + "/" 
				+ presentationName + "/" + contentType + "/" +slideNumber;
		log.info("Loading slide content for "+presentationName+"/"+contentType+"/"+slideNumber);
		
		HttpClient client = new DefaultHttpClient();
		HttpGet method = new HttpGet(url);
		try {
			HttpResponse httpResponse = client.execute(method);
		
			if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("HTTP GET {} return {}", url, httpResponse.getStatusLine().getStatusCode());
			} else {
				EntityUtils.consume(httpResponse.getEntity());
			}
		} catch (ClientProtocolException pe) {
			log.error("Client Protocol Exception", pe);
		} catch (IOException ie) {
			log.error("IO Exception", ie);
		}
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
		this.doGetPresentationInfo();
	}

	@Override
	public void onSharedObjectSend(ISharedObjectBase so, String method,
			List<?> params) {
		log.debug("onSharedObjectSend");
		log.debug("Method="+method);
		if (method.equals("conversionUpdateMessageCallback") && params != null) {
			
		} else if (method.equals("pageCountExceededUpdateMessageCallback") && params != null) {
			
		} else if (method.equals("generatedSlideUpdateMessageCallback") && params != null) {

		} else if (method.equals("conversionCompletedUpdateMessageCallback") && params != null) {
			presentationName = (String) params.get(3);
			slideNumber = 1;
			loadPresentation();
		} else if (method.equals("gotoSlideCallback") && params != null) {
			slideNumber = ((Double)params.get(0)).intValue()+1;
			
			loadSlideContent("thumbnail");
			loadSlideContent("textfile");
			loadSlideContent("slide");
		} else if (method.equals("moveCallback") && params != null) {
	
		} else if (method.equals("removePresentationCallback") && params != null) {
			String removePresentationName = (String)params.get(0);
			if (removePresentationName.equals(presentationName)) {
				
			}
		} else if (method.equals("sharePresentationCallback") && params != null) {
			String shareName = (String) params.get(0); // name of presentation being shared
			Boolean shared = (Boolean) params.get(1); // whether or not it is shared
			if (shared) {
				presentationName = shareName;
				slideNumber = 1;
				loadPresentation();
			}
		}
	}

	@Override
	public boolean onCommand(String resultFor, Command command) {
		log.debug("onCommand");
		if (onGetPresentationInfo(resultFor, command)) {
			return true;
		} else {
			return false;
		}
	}
}

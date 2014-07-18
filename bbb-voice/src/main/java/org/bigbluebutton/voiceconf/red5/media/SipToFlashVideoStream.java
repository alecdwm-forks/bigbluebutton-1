/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.voiceconf.red5.media;

import org.red5.server.api.scope.IScope;
import java.net.DatagramSocket;
import org.bigbluebutton.voiceconf.red5.media.transcoder.SipToFlashTranscoder;
import org.red5.server.net.rtmp.event.VideoData;
import org.bigbluebutton.voiceconf.red5.media.transcoder.TranscodedMediaDataListener;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.scope.Scope;
import org.red5.server.stream.IProviderService;
import org.bigbluebutton.voiceconf.red5.media.SipToFlashStream;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class SipToFlashVideoStream implements SipToFlashStream, RtpStreamReceiverListener {
	private static final Logger log = Red5LoggerFactory.getLogger(SipToFlashAudioStream.class, "sip");

	private BroadcastStream videoBroadcastStream;
	private IScope scope;
	private final String videoReceiverStreamName;
	private RtpStreamReceiver rtpStreamReceiver;
	private StreamObserver observer;

	private SipToFlashTranscoder transcoder;
	private boolean sentMetadata = false;
	private IoBuffer mBuffer;

	private VideoData videoData;

	private final byte[] fakeMetadata = new byte[] {
			0x02, 0x00, 0x0a, 0x6f, 0x6e, 0x4d, 0x65, 0x74, 0x61, 0x44, 0x61, 0x74, 0x61, 0x08, 0x00, 0x00,  
			0x00, 0x06, 0x00, 0x08, 0x64, 0x75, 0x72, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x00, 0x40, 0x31, (byte)0xaf,  
			0x5c, 0x28, (byte)0xf5, (byte)0xc2, (byte)0x8f, 0x00, 0x0f, 0x61, 0x75, 0x64, 0x69, 0x6f, 0x73, 0x61, 0x6d, 0x70, 
			0x6c, 0x65, 0x72, 0x61, 0x74, 0x65, 0x00, 0x40, (byte)0xe5, (byte)0x88, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00,  
			0x0f, 0x61, 0x75, 0x64, 0x69, 0x6f, 0x73, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x73, 0x69, 0x7a, 0x65,  
			0x00, 0x40, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x73, 0x74, 0x65, 0x72, 0x65,  
			0x6f, 0x01, 0x00, 0x00, 0x0c, 0x61, 0x75, 0x64, 0x69, 0x6f, 0x63, 0x6f, 0x64, 0x65, 0x63, 0x69,  
			0x64, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x66, 0x69, 0x6c, 0x65,  
			(byte)0xc8, 0x73, 0x69, 0x7a, 0x65, 0x00, 0x40, (byte)0xf3, (byte)0xf5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  
	};


	public SipToFlashVideoStream(IScope scope, SipToFlashTranscoder transcoder, DatagramSocket socket) {
		this.transcoder = transcoder;
		this.scope = scope;		
		rtpStreamReceiver = new RtpStreamReceiver(socket, transcoder.getIncomingEncodedFrameSize());
		rtpStreamReceiver.setRtpStreamReceiverListener(this);

		videoReceiverStreamName = "freeswitchToBbbVideoStream_" + System.currentTimeMillis();	
		mBuffer = IoBuffer.allocate(1024);
		mBuffer = mBuffer.setAutoExpand(true);

		videoData = new VideoData();
		transcoder.setTranscodedMediaDataListener(this);
	}


	@Override
	public String getStreamName() {
		return videoReceiverStreamName;
	}

	@Override
	public void addListenStreamObserver(StreamObserver o) {
		observer = o;
	}

	@Override
	public void stop() {
			if (log.isDebugEnabled()) 
				log.debug("Stopping VIDEO stream for {}", videoReceiverStreamName);

			transcoder.stop();
			rtpStreamReceiver.stop();

			if (log.isDebugEnabled()) 
				log.debug("Stopped RTP VIDEO Stream Receiver for {}", videoReceiverStreamName);

			if (videoBroadcastStream != null) {
				videoBroadcastStream.stop();

				if (log.isDebugEnabled()) 
					log.debug("Stopped videoBroadcastStream for {}", videoReceiverStreamName);

				videoBroadcastStream.close();

			    if (log.isDebugEnabled()) 
			    	log.debug("Closed videoBroadcastStream for {}", videoReceiverStreamName);
			} 

			else
				if (log.isDebugEnabled()) 
					log.debug("videoBroadcastStream is null, couldn't stop");

		    if (log.isDebugEnabled()) 
		    	log.debug("VIDEO Stream(s) stopped");

	}	

	@Override
	public void start() {
		if (log.isDebugEnabled()) 
			log.debug("started publishing VIDEO stream in scope=[" + scope.getName() + "] path=[" + scope.getPath() + "]");

		videoBroadcastStream = new BroadcastStream(videoReceiverStreamName);
		videoBroadcastStream.setPublishedName(videoReceiverStreamName);
		videoBroadcastStream.setScope(scope);
		
		IContext context = scope.getContext();
		
		IProviderService providerService = (IProviderService) context.getBean(IProviderService.BEAN_NAME);
		if (providerService.registerBroadcastStream(scope, videoReceiverStreamName, videoBroadcastStream)){
			// Do nothing. Successfully registered a live broadcast stream. (ralam Sept. 4, 2012)
		} else{
			log.error("could not register broadcast stream");
			throw new RuntimeException("could not register broadcast stream");
		}
		
	    videoBroadcastStream.start();	    
		transcoder.start();   	
	    rtpStreamReceiver.start();
	}

	@Override
	public void onStoppedReceiving() {
		if (observer != null) observer.onStreamStopped();
	}

	@Override
	public void onMediaDataReceived(byte[] videoData, int offset, int len, long timestampDelta) {
		transcoder.handleData(videoData, offset, len, timestampDelta);
	}	


	@Override
	public void handleTranscodedMediaData(byte[] videoData, long timestamp) {
		if (videoData != null) {
			pushVideo(videoData, timestamp);
		} else {
			log.warn("Transcoded VIDEO is null. Discarding.");
		}
	}

	private void sendFakeMetadata(long timestamp) {
		if (!sentMetadata) {

			//O COMENTÁRIO ABAIXO VALE PRA VIDEO TAMBÉM???!!!
			/*
			* Flash Player 10.1 requires us to send metadata for it to play audio.
			* We create a fake one here to get it going. Red5 should do this automatically
			* but for Red5 0.91, doesn't yet. (ralam Sept 24, 2010).
			*/
			mBuffer.clear();	
			mBuffer.put(fakeMetadata);
			mBuffer.flip();

			Notify notifyData = new Notify(mBuffer);
			notifyData.setTimestamp((int)timestamp);
			notifyData.setSourceType(Constants.SOURCE_TYPE_LIVE);
			videoBroadcastStream.dispatchEvent(notifyData);
			notifyData.release();
			sentMetadata = true;
		}	
	}

	private void pushVideo(byte[] video, long timestamp) {	
		sendFakeMetadata(timestamp);
		        mBuffer.clear();
		        mBuffer.put((byte) transcoder.getCodecId());
		mBuffer.put(video);
		mBuffer.flip();
		videoData.setSourceType(Constants.SOURCE_TYPE_LIVE);

		// O COMENTÁRIO ABAIXO VALE PRA VIDEO TAMBÉM???!!!
		/*
		* Use timestamp increments passed in by codecs (i.e. 32 for nelly). This will force
		* Flash Player to playback audio at proper timestamp. If we calculate timestamp using
		* System.currentTimeMillis() - startTimestamp, the audio has tendency to drift and
		* introduce delay. (ralam dec 14, 2010)
		*/
        videoData.setTimestamp((int)(timestamp));
        videoData.setData(mBuffer);
		videoBroadcastStream.dispatchEvent(videoData);
		videoData.release();
    }	

}
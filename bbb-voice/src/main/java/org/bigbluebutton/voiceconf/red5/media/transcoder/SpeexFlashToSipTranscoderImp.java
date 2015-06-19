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
package org.bigbluebutton.voiceconf.red5.media.transcoder;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bigbluebutton.voiceconf.red5.media.FlashToSipAudioStream;
import org.red5.app.sip.codecs.Codec;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * Speex wideband to Speex wideband Flash to SIP transcoder.
 * This class is just a passthrough transcoder.
 *
 */
public class SpeexFlashToSipTranscoderImp implements FlashToSipTranscoder {
	protected static Logger log = Red5LoggerFactory.getLogger(SpeexFlashToSipTranscoderImp.class, "sip");
	
	private Codec audioCodec;
	private long timestamp = 0;
	private final static int TS_INCREMENT = 320; // Determined from PCAP traces.
	
	private final Executor exec = Executors.newSingleThreadExecutor();
	private Runnable mediaDataProcessor;
	private volatile boolean processMediaData = false;
	private BlockingQueue<RtpData> mediaDataQ;
	
	private TranscodedMediaDataListener transcodedMediaDataListener;
	
	public SpeexFlashToSipTranscoderImp(Codec audioCodec) {
		mediaDataQ = new LinkedBlockingQueue<RtpData>();
		this.audioCodec = audioCodec;
        Random rgen = new Random();
        timestamp = rgen.nextInt(1000);
	}
	
	public void transcode(byte[] audioData, int startOffset, int length) {
		byte[] transcodedAudio = new byte[length];
		// Just copy the audio data removing the codec id which is the first-byte
		// represented by the startOffset var.
		System.arraycopy(audioData, startOffset, transcodedAudio, 0, length);
		
		RtpData srad = new RtpData(transcodedAudio, timestamp += TS_INCREMENT);
		try {
			mediaDataQ.offer(srad, 100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.warn("Failed to add speex audio data into queue.");
		}
	}
	
	public int getCodecId() {
		return audioCodec.getCodecId();
	}

	public int getOutgoingEncodedFrameSize() {
		return audioCodec.getOutgoingEncodedFrameSize();
	}

	public int getOutgoingPacketization() {
		return audioCodec.getOutgoingPacketization();
	}

	@Override
	public void handlePacket(byte[] data, int begin, int end) {
		transcode(data, begin, end);		
	}

	@Override
	public void setTranscodedMediaDataListener(FlashToSipAudioStream flashToSipAudioStream) {
		this.transcodedMediaDataListener = flashToSipAudioStream;		
	}
	
	private void processMediaData() {
		while (processMediaData) {		
			RtpData srad;
			try {
				srad = mediaDataQ.take();
				transcodedMediaDataListener.handleTranscodedMediaData(srad.data, srad.timestamp);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}
	}

    @Override
    public void start() {
    	processMediaData = true;	 
	    mediaDataProcessor = new Runnable() {
    		public void run() {
    			processMediaData();   			
    		}
    	};
    	exec.execute(mediaDataProcessor);
    }
	
	@Override
    public void stop() {
    	processMediaData = false;
    }
}

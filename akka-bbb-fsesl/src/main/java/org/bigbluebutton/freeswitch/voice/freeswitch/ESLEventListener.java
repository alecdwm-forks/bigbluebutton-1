package org.bigbluebutton.freeswitch.voice.freeswitch;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bigbluebutton.freeswitch.voice.events.ChannelCallStateEvent;
import org.bigbluebutton.freeswitch.voice.events.ChannelHangupCompleteEvent;
import org.bigbluebutton.freeswitch.voice.events.ConferenceEventListener;
import org.bigbluebutton.freeswitch.voice.events.VideoFloorChangedEvent;
import org.bigbluebutton.freeswitch.voice.events.VideoPausedEvent;
import org.bigbluebutton.freeswitch.voice.events.VideoResumedEvent;
import org.bigbluebutton.freeswitch.voice.events.VoiceStartRecordingEvent;
import org.bigbluebutton.freeswitch.voice.events.VoiceUserJoinedEvent;
import org.bigbluebutton.freeswitch.voice.events.VoiceUserLeftEvent;
import org.bigbluebutton.freeswitch.voice.events.VoiceUserMutedEvent;
import org.bigbluebutton.freeswitch.voice.events.VoiceUserTalkingEvent;
import org.bigbluebutton.freeswitch.voice.freeswitch.response.ConferenceMember;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.jboss.netty.channel.ExceptionEvent;

public class ESLEventListener implements IEslEventListener {

    private static final String START_TALKING_EVENT = "start-talking";
    private static final String STOP_TALKING_EVENT = "stop-talking";
    private static final String START_RECORDING_EVENT = "start-recording";
    private static final String STOP_RECORDING_EVENT = "stop-recording";
    private static final String VIDEO_PAUSED_EVENT = "video-paused";
    private static final String VIDEO_RESUMED_EVENT = "video-resumed";
    private static final String VIDEO_FLOOR_CHANGE_EVENT = "video-floor-change";
    
    private final ConferenceEventListener conferenceEventListener;
    
    private static Set<String> confsThatVideoIsActive = new HashSet<String>();
    private Map<String, DialReferenceValuePair> outboundDialReferences = new ConcurrentHashMap<String, DialReferenceValuePair>();

    public ESLEventListener(ConferenceEventListener conferenceEventListener) {
    	this.conferenceEventListener = conferenceEventListener;
    }

    @Override
    public void conferenceEventPlayFile(String uniqueId, String confName, int confSize, EslEvent event) {
        //Ignored, Noop
    }

    private static final Pattern DIAL_ORIGINATION_UUID_PATTERN = Pattern.compile(".* dial .*origination_uuid='([^']*)'.*");
    private static final Pattern DIAL_RESPONSE_PATTERN = Pattern.compile("^\\[Call Requested: result: \\[(.*)\\].*\\]$");
    private static final String[] DIAL_IGNORED_RESPONSES = new String[]{ "SUCCESS" };

    @Override
    public void backgroundJobResultReceived(EslEvent event) {
        System.out.println( "Background job result received [" + event + "]");
        String uri = this.getCallerIdFromEvent(event);

        String arg = event.getEventHeaders().get("Job-Command-Arg");
        if (arg != null) {
            Matcher matcher = DIAL_ORIGINATION_UUID_PATTERN.matcher(arg);
            if (matcher.matches()) {
                String uuid = matcher.group(1).trim();
                String responseString = event.getEventBodyLines().toString().trim();

                System.out.println("Background job result for uuid [" + uuid + "], response: [" + responseString +"]");

                matcher = DIAL_RESPONSE_PATTERN.matcher(responseString);
                if (matcher.matches()) {
                    String error = matcher.group(1).trim();

                    if (Arrays.asList(DIAL_IGNORED_RESPONSES).contains(error)) {
                        System.out.println("Ignoring error code [" + error + "]");
                        return;
                    }

                    DialReferenceValuePair ref = removeDialReference(uri);
                    if (ref == null) {
                        return;
                    }

                    ChannelHangupCompleteEvent hce = new ChannelHangupCompleteEvent(uuid,
                            "HANGUP", error, ref.getRoom(), ref.getParticipant());
                    conferenceEventListener.handleConferenceEvent(hce);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ExceptionEvent e) {
//        setChanged();
//        notifyObservers(e);
    }

    private static final Pattern GLOBALCALL_NAME_PATTERN = Pattern.compile("(GLOBAL_CALL)_(.*)$");
    private static final Pattern CALLERNAME_PATTERN = Pattern.compile("(.*)-bbbID-(.*)$");
    
    @Override
    public void conferenceEventJoin(String uniqueId, String confName, int confSize, EslEvent event) {
    	
        Integer memberId = this.getMemberIdFromEvent(event);
        Map<String, String> headers = event.getEventHeaders();
        String callerId = this.getCallerIdFromEvent(event);
        String callerIdName = this.getValidCallerIdNameFromConferenceEvent(event);

        boolean muted = headers.get("Speak").equals("true") ? false : true; //Was inverted which was causing a State issue
        boolean speaking = headers.get("Talking").equals("true") ? true : false;
        boolean hasVideo = headers.get("Video").equals("true") ? true : false;
        boolean hasFloor = headers.get("Floor").equals("true") ? true : false;

        String voiceUserId = callerIdName;
        
        System.out.println("User joined voice conference, user=[" + callerIdName + "], conf=[" + confName + "]");
        
        Matcher gcpMatcher = GLOBALCALL_NAME_PATTERN.matcher(callerIdName);
        if (gcpMatcher.matches()) {
            System.out.println("GLOBAL CALL CONNECTED [" + callerIdName + "]");

            printConfsThatHaveActiveVideo();
            //if the conference has a active video before the global is connected, it means that a sip phone is already sending video
            if(isThereVideoActive(confName)) {
                System.out.println("Sending VideoResumedEvent because there is(are) sip phone(s) sending video (confName = " + confName + ")");
                VideoResumedEvent vResumed = new VideoResumedEvent(confName);
                conferenceEventListener.handleConferenceEvent(vResumed);
            }

        	return;
        }
        		
		    Matcher matcher = CALLERNAME_PATTERN.matcher(callerIdName);
		    if (matcher.matches()) {			
			    voiceUserId = matcher.group(1).trim();
			    callerIdName = matcher.group(2).trim();
		    } 
        
        VoiceUserJoinedEvent pj = new VoiceUserJoinedEvent(voiceUserId, memberId.toString(), confName, callerId, callerIdName, muted, speaking, hasVideo, hasFloor);
        conferenceEventListener.handleConferenceEvent(pj);
    }

    @Override
    public void conferenceEventLeave(String uniqueId, String confName, int confSize, EslEvent event) {   	
        Integer memberId = this.getMemberIdFromEvent(event);
        if (memberId == null) {
            return;
        }

        String callerIdName = this.getValidCallerIdNameFromConferenceEvent(event);
        System.out.println("User left voice conference, user=[" + callerIdName + "], conf=[" + confName + "], memberId=[" + memberId.toString() + "]");
        Matcher gcpMatcher = GLOBALCALL_NAME_PATTERN.matcher(callerIdName);
        if (gcpMatcher.matches()) {
            System.out.println("GLOBAL CALL DISCONNECTED [" + callerIdName + "]");
            return;
        }


        VoiceUserLeftEvent pl = new VoiceUserLeftEvent(memberId.toString(), confName);
        conferenceEventListener.handleConferenceEvent(pl);
    }

    @Override
    public void conferenceEventMute(String uniqueId, String confName, int confSize, EslEvent event) {
        Integer memberId = this.getMemberIdFromEvent(event);
        System.out.println("******************** Received Conference Muted Event from FreeSWITCH user[" + memberId.toString() + "]");
        System.out.println("User muted voice conference, user=[" + memberId.toString() + "], conf=[" + confName + "]");
        VoiceUserMutedEvent pm = new VoiceUserMutedEvent(memberId.toString(), confName, true);
        conferenceEventListener.handleConferenceEvent(pm);
    }

    @Override
    public void conferenceEventUnMute(String uniqueId, String confName, int confSize, EslEvent event) {
        Integer memberId = this.getMemberIdFromEvent(event);
        System.out.println("******************** Received ConferenceUnmuted Event from FreeSWITCH user[" + memberId.toString() + "]");
        System.out.println("User unmuted voice conference, user=[" + memberId.toString() + "], conf=[" + confName + "]");
        VoiceUserMutedEvent pm = new VoiceUserMutedEvent(memberId.toString(), confName, false);
        conferenceEventListener.handleConferenceEvent(pm);
    }

    @Override
    public void conferenceEventAction(String uniqueId, String confName, int confSize, String action, EslEvent event) {
        Integer memberId = this.getMemberIdFromEvent(event);
        VoiceUserTalkingEvent pt;
        
        System.out.println("******************** Receive conference Action [" + action + "]");
        
        if (action == null) {
            return;
        }

        if (action.equals(START_TALKING_EVENT)) {
            pt = new VoiceUserTalkingEvent(memberId.toString(), confName, true);
            conferenceEventListener.handleConferenceEvent(pt);        	
        } else if (action.equals(STOP_TALKING_EVENT)) {
            pt = new VoiceUserTalkingEvent(memberId.toString(), confName, false);
            conferenceEventListener.handleConferenceEvent(pt);        	
        } else {
        	System.out.println("Unknown conference Action [" + action + "]");
        }
    }

    @Override
    public void conferenceEventTransfer(String uniqueId, String confName, int confSize, EslEvent event) {
        //Ignored, Noop
    }

    @Override
    public void conferenceEventThreadRun(String uniqueId, String confName, int confSize, EslEvent event) {
    	
    }
    
    //@Override
    public void conferenceEventRecord(String uniqueId, String confName, int confSize, EslEvent event) {
    	String action = event.getEventHeaders().get("Action");
    	
        if(action == null) {          
            return;
        }
        
    	System.out.println("Handling conferenceEventRecord " + action);
    	
    	if (action.equals(START_RECORDING_EVENT)) {
            VoiceStartRecordingEvent sre = new VoiceStartRecordingEvent(confName, true);
            sre.setRecordingFilename(getRecordFilenameFromEvent(event));
            sre.setTimestamp(genTimestamp().toString());
            
            System.out.println("Voice conference recording started. file=[" + getRecordFilenameFromEvent(event) + "], conf=[" + confName + "]");
            
            conferenceEventListener.handleConferenceEvent(sre);    		
    	} else if (action.equals(STOP_RECORDING_EVENT)) {
        	VoiceStartRecordingEvent srev = new VoiceStartRecordingEvent(confName, false);
            srev.setRecordingFilename(getRecordFilenameFromEvent(event));
            srev.setTimestamp(genTimestamp().toString());
            
            System.out.println("Voice conference recording stopped. file=[" + getRecordFilenameFromEvent(event) + "], conf=[" + confName + "]");           
            conferenceEventListener.handleConferenceEvent(srev);    		
    	} else {
    		System.out.println("Processing UNKNOWN conference Action " + action + "]");
    	}
    }

    private Long genTimestamp() {
    	return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
    
	@Override
	public void eventReceived(EslEvent event) {
		System.out.println("ESL Event Listener received event=[" + event.getEventName() + "]");
//        if (event.getEventName().equals(FreeswitchHeartbeatMonitor.EVENT_HEARTBEAT)) {
////           setChanged();
//           notifyObservers(event);
//           return; 
//        }
          if(event.getEventName().equals("CUSTOM")) {
            String action = event.getEventHeaders().get("Action");
            String confName = event.getEventHeaders().get("Conference-Name");
            if (action != null && confName != null) {
                switch (action) {
                    case VIDEO_PAUSED_EVENT:
                        System.out.println("Received " + action + " from Freeswitch");
                        VideoPausedEvent vPaused = new VideoPausedEvent(confName);
                        conferenceEventListener.handleConferenceEvent(vPaused);
                        if(isThereVideoActive(confName)) {
                            confsThatVideoIsActive.remove(confName);
                            System.out.println("Received video paused => " + confName + " doesn't have active video anymore.");
                        }
                        break;

                    case VIDEO_RESUMED_EVENT:
                        System.out.println("Received " + action + " from Freeswitch from conference " +confName);
                        VideoResumedEvent vResumed = new VideoResumedEvent(confName);
                        conferenceEventListener.handleConferenceEvent(vResumed);
                        break;

                    case VIDEO_FLOOR_CHANGE_EVENT:
                        System.out.println("Received " + action + " from Freeswitch");
                        String holderMemberId = getNewFloorHolderMemberIdFromEvent(event);

                        if(!holderMemberId.isEmpty()) {
                            System.out.println(confName + " video floor passed to the holderMemberId = " + holderMemberId);
                            if(!isThereVideoActive(confName))
                                confsThatVideoIsActive.add(confName);
                        }
                        else if(isThereVideoActive(confName)) {
                                confsThatVideoIsActive.remove(confName);
                                System.out.println("Received an empty id as video floor => " + confName + " doesn't have active video anymore.");
                             }

                        VideoFloorChangedEvent vFloor= new VideoFloorChangedEvent(confName, holderMemberId);
                        conferenceEventListener.handleConferenceEvent(vFloor);
                        break;

                    default:
                        System.out.println("Unknown conference Action [" + action + "]");
                }
            }
        }
        else if(event.getEventName().equals("CHANNEL_CALLSTATE")) {
            String uniqueId = this.getUniqueIdFromEvent(event);
            String callState = this.getChannelCallStateFromEvent(event);
            String originalCallState = this.getOrigChannelCallStateFromEvent(event);
            String origCallerIdName = this.getOrigCallerIdNameFromEvent(event);
            String channelName = this.getCallerChannelNameFromEvent(event);
            String uri = this.getCallerIdFromEvent(event);


            System.out.println("Received [" +  event.getEventName() + "] for uuid [" + uniqueId + "], URI [" + uri + "], CallState [" + callState + "]");

            DialReferenceValuePair ref = getDialReferenceValue(uri);
            if (ref == null) {
                System.out.println("There was no dial reference, aborting...");
                return;
            }

            String room = ref.getRoom();
            String participant = ref.getParticipant();

            System.out.println("There was a dial reference for uuid [" + uniqueId + "], URI [" + uri + "], room [" + room + "], participant [" + participant + "]");

            ChannelCallStateEvent cse = new ChannelCallStateEvent(uniqueId, callState,
                                                    room, participant);

            conferenceEventListener.handleConferenceEvent(cse);
        }
        else if(event.getEventName().equals("CHANNEL_HANGUP_COMPLETE")) {
            String uniqueId = getUniqueIdFromEvent(event);
            String callState = getChannelCallStateFromEvent(event);
            String hangupCause = getHangupCauseFromEvent(event);
            String origCallerIdName = getOrigCallerIdNameFromEvent(event);
            String channelName = getCallerChannelNameFromEvent(event);
            String uri = this.getCallerIdFromEvent(event);

            System.out.println("Received [" +  event.getEventName() + "] for uuid [" + uniqueId + "], URI [" + uri + "], CallState [" + callState + "],  HangupCause [" + hangupCause + "]");
            DialReferenceValuePair ref = removeDialReference(uri);
            if (ref == null) {
                System.out.println("There was no dial reference, aborting...");
                return;
            }

            String room = ref.getRoom();
            String participant = ref.getParticipant();

            ChannelHangupCompleteEvent hce = new ChannelHangupCompleteEvent(uniqueId, callState,
                                                    hangupCause, room, participant);

            conferenceEventListener.handleConferenceEvent(hce);
        }
    }

    public void addDialReference(String uri, DialReferenceValuePair value) {
        System.out.println("Adding dial reference: [" + uri+ "] -> [" + value.getRoom() + "], [" + value.getParticipant() + "]");
        if (!outboundDialReferences.containsKey(uri)) {
            outboundDialReferences.put(uri, value);
        }
    }

    private DialReferenceValuePair removeDialReference(String uri) {
        System.out.println("Removing dial reference: [" + uri + "]");
        DialReferenceValuePair r = outboundDialReferences.remove(uri);
        if (r == null) {
            System.out.println("Returning null because the uri has already been removed");
        }
        System.out.println("Current dial references size: [" + outboundDialReferences.size() + "]");
        return r;
    }

    private DialReferenceValuePair getDialReferenceValue(String uri) {
        return outboundDialReferences.get(uri);
    }

    private String getChannelCallStateFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Channel-Call-State");
    }

    private String getHangupCauseFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Hangup-Cause");
    }

    private String getCallerChannelNameFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Caller-Channel-Name");
    }

    private String getOrigChannelCallStateFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Original-Channel-Call-State");
    }

    private String getUniqueIdFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Unique-ID");
    }

    private String getOrigCallerIdNameFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Caller-Orig-Caller-ID-Name");
    }

    private Integer getMemberIdFromEvent(EslEvent e) {
        try {
            return new Integer(e.getEventHeaders().get("Member-ID"));
        } catch (NumberFormatException excp) {
            return null;
        }
    }

    private String getCallerIdFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Caller-Caller-ID-Number");
    }

    private String getCallerIdNameFromEvent(EslEvent e) {
        return e.getEventHeaders().get("Caller-Caller-ID-Name");
    }

    private String getValidCallerIdNameFromConferenceEvent(EslEvent e) {
        /*
         * For some equipments, if callerIdName is 'unknown', we get the caller name
         * from the sip user agent
         */
        String callerIdName = this.getCallerIdNameFromEvent(e);
        String callerIPAddress = this.getCallerNetworkAddress(e);
        return ConferenceMember.getValidCallerIdName(callerIdName,callerIPAddress);
    }

    private String getCallerNetworkAddress(EslEvent e){
        return e.getEventHeaders().get("Caller-Network-Addr");
    }

    
    private String getRecordFilenameFromEvent(EslEvent e) {
    	return e.getEventHeaders().get("Path");
    }

    private String getNewFloorHolderMemberIdFromEvent(EslEvent e) {
        String newHolder = e.getEventHeaders().get("New-ID");
        if(newHolder == null || newHolder.equalsIgnoreCase("none")) {
            newHolder = "";
        }
        return newHolder;
    }


    private boolean isThereVideoActive(String confName) {
        return confsThatVideoIsActive.contains(confName);
    }

    private void printConfsThatHaveActiveVideo() {
        String message = "Rooms that have active video at this precise moment: ";
        message = message + Arrays.toString(confsThatVideoIsActive.toArray(new String[0]));

        System.out.println(message);
    }
}

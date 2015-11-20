package org.bigbluebutton.webconference.voice;

public interface IVoiceConferenceService {
  void voiceUserJoined(String userId, String webUserId, String conference, 
			String callerIdNum, String callerIdName,
			Boolean muted, Boolean speaking, Boolean hasVideo, Boolean hasFloor);
  void voiceUserLeft(String meetingId, String userId);
  void voiceUserLocked(String meetingId, String userId, Boolean locked);
  void voiceUserMuted(String meetingId, String userId, Boolean muted);
  void voiceUserTalking(String meetingId, String userId, Boolean talking);
  void voiceStartedRecording(String conference, String recordingFile, 
		  String timestamp, Boolean recording);
  void videoPaused(String meetingId);
  void videoResumed(String meetingId);
  void activeTalkerChanged(String meetingId, String floorHolderVoiceUserId);
  void channelCallState(String conference, String uniqueId, String callState,
    String userId);
  void channelHangup(String conference, String uniqueId, String callState,
    String hangupCause, String userId);
}

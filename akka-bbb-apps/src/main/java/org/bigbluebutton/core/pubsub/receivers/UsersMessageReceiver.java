
package org.bigbluebutton.core.pubsub.receivers;

import org.bigbluebutton.common.messages.ActiveTalkerChangedInVoiceConfMessage;
import org.bigbluebutton.common.messages.AssignPresenterRequestMessage;
import org.bigbluebutton.common.messages.BroadcastLayoutRequestMessage;
import org.bigbluebutton.common.messages.ChannelCallStateInVoiceConfMessage;
import org.bigbluebutton.common.messages.CancelDialRequestInVoiceConfMessage;
import org.bigbluebutton.common.messages.ChannelHangupInVoiceConfMessage;
import org.bigbluebutton.common.messages.EjectUserFromMeetingRequestMessage;
import org.bigbluebutton.common.messages.EjectUserFromVoiceRequestMessage;
import org.bigbluebutton.common.messages.GetCurrentLayoutRequestMessage;
import org.bigbluebutton.common.messages.GetRecordingStatusRequestMessage;
import org.bigbluebutton.common.messages.GetUsersRequestMessage;
import org.bigbluebutton.common.messages.InitAudioSettingsMessage;
import org.bigbluebutton.common.messages.InitPermissionsSettingMessage;
import org.bigbluebutton.common.messages.IsMeetingMutedRequestMessage;
import org.bigbluebutton.common.messages.LockLayoutRequestMessage;
import org.bigbluebutton.common.messages.LockMuteUserRequestMessage;
import org.bigbluebutton.common.messages.MessagingConstants;
import org.bigbluebutton.common.messages.MuteAllExceptPresenterRequestMessage;
import org.bigbluebutton.common.messages.MuteAllRequestMessage;
import org.bigbluebutton.common.messages.MuteUserRequestMessage;
import org.bigbluebutton.common.messages.OutboundDialRequestInVoiceConfMessage;
import org.bigbluebutton.common.messages.SendDtmfRequestInVoiceConfMessage;
import org.bigbluebutton.common.messages.SetRecordingStatusRequestMessage;
import org.bigbluebutton.common.messages.SetUserStatusRequestMessage;
import org.bigbluebutton.common.messages.StartKurentoRtpReplyMessage;
import org.bigbluebutton.common.messages.StartTranscoderReplyMessage;
import org.bigbluebutton.common.messages.StartProbingReplyMessage;
import org.bigbluebutton.common.messages.StopKurentoRtpReplyMessage;
import org.bigbluebutton.common.messages.UpdateTranscoderReplyMessage;
import org.bigbluebutton.common.messages.StopTranscoderReplyMessage;
import org.bigbluebutton.common.messages.TranscoderStatusUpdateMessage;
import org.bigbluebutton.common.messages.UserJoinedVoiceConfMessage;
import org.bigbluebutton.common.messages.UserLeavingMessage;
import org.bigbluebutton.common.messages.UserLeftVoiceConfMessage;
import org.bigbluebutton.common.messages.UserLockedInVoiceConfMessage;
import org.bigbluebutton.common.messages.UserMutedInVoiceConfMessage;
import org.bigbluebutton.common.messages.UserEmojiStatusMessage;
import org.bigbluebutton.common.messages.UserShareWebcamRequestMessage;
import org.bigbluebutton.common.messages.UserTalkingInVoiceConfMessage;
import org.bigbluebutton.common.messages.UserUnshareWebcamRequestMessage;
import org.bigbluebutton.common.messages.VoiceConfRecordingStartedMessage;
import org.bigbluebutton.core.api.IBigBlueButtonInGW;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class UsersMessageReceiver implements MessageHandler{

	private IBigBlueButtonInGW bbbInGW;
	
	public UsersMessageReceiver(IBigBlueButtonInGW bbbInGW) {
		this.bbbInGW = bbbInGW;
	}

	@Override
	public void handleMessage(String pattern, String channel, String message) {
		if (channel.equalsIgnoreCase(MessagingConstants.TO_USERS_CHANNEL)) {
//			System.out.println("Users message: " + channel + " " + message);
			JsonParser parser = new JsonParser();
			JsonObject obj = (JsonObject) parser.parse(message);
			if (obj.has("header") && obj.has("payload")) {
				JsonObject header = (JsonObject) obj.get("header");

				if (header.has("name")) {
					String messageName = header.get("name").getAsString();
					switch (messageName) {
					  case UserLeavingMessage.USER_LEAVING:
						  processUserLeavingMessage(message);
						  break;
					  case AssignPresenterRequestMessage.ASSIGN_PRESENTER_REQUEST:
						  processAssignPresenterRequestMessage(message);
						  break;
					  case UserEmojiStatusMessage.USER_EMOJI_STATUS:
						  processUserEmojiStatusMessage(message);
						  break;
					  case EjectUserFromMeetingRequestMessage.EJECT_USER_FROM_MEETING_REQUEST:
						  processEjectUserFromMeetingRequestMessage(message);
						  break;
					  case UserShareWebcamRequestMessage.USER_SHARE_WEBCAM_REQUEST:
						  processUserShareWebcamRequestMessage(message);
						  break;
					  case UserUnshareWebcamRequestMessage.USER_UNSHARE_WEBCAM_REQUEST:
						  processUserUnshareWebcamRequestMessage(message);
						  break;
					  case SetUserStatusRequestMessage.SET_USER_STATUS_REQUEST:
						  processSetUserStatusRequestMessage(message);
						  break;
					  case SetRecordingStatusRequestMessage.SET_RECORDING_STATUS_REQUEST:
						  processSetRecordingStatusRequestMessage(message);
						  break;
					  case GetRecordingStatusRequestMessage.GET_RECORDING_STATUS_REQUEST:
						  processGetRecordingStatusRequestMessage(message);
						  break;
					  case GetUsersRequestMessage.GET_USERS_REQUEST:
						  processGetUsersRequestMessage(message);
						  break;
					  case InitPermissionsSettingMessage.INIT_PERMISSIONS_SETTING:
						  processInitPermissionsSettingMessage(message);
						  break;
					  case InitAudioSettingsMessage.INIT_AUDIO_SETTING:
						  processInitAudioSettingsMessage(message);
						  break;
					  case BroadcastLayoutRequestMessage.BROADCAST_LAYOUT_REQUEST:
						  processBroadcastLayoutRequestMessage(message);
						  break;
					  case LockLayoutRequestMessage.LOCK_LAYOUT_REQUEST:
						  processLockLayoutRequestMessage(message);
						  break;
					  case GetCurrentLayoutRequestMessage.GET_CURRENT_LAYOUT_REQUEST:
						  processGetCurrentLayoutRequestMessage(message);
						  break;
					  case MuteAllExceptPresenterRequestMessage.MUTE_ALL_EXCEPT_PRESENTER_REQUEST:
						  processMuteAllExceptPresenterRequestMessage(message);
						  break;
					  case MuteAllRequestMessage.MUTE_ALL_REQUEST:
						  processMuteAllRequestMessage(message);
						  break;
					  case IsMeetingMutedRequestMessage.IS_MEETING_MUTED_REQUEST:
						  processIsMeetingMutedRequestMessage(message);
						  break;
					  case MuteUserRequestMessage.MUTE_USER_REQUEST:
						  processMuteUserRequestMessage(message);
						  break;
					  case LockMuteUserRequestMessage.LOCK_MUTE_USER_REQUEST:
						  processLockMuteUserRequestMessage(message);
						  break;
					  case EjectUserFromVoiceRequestMessage.EJECT_USER_FROM_VOICE_REQUEST:
						  processEjectUserFromVoiceRequestMessage(message);
						  break;
					  case OutboundDialRequestInVoiceConfMessage.OUTBOUND_DIAL_REQUEST_IN_VOICE_CONF:
						  processOutboundDialRequestInVoiceConfMessage(message);
						  break;
					  case CancelDialRequestInVoiceConfMessage.CANCEL_DIAL_REQUEST_IN_VOICE_CONF:
						  processCancelDialRequestInVoiceConfMessage(message);
						  break;
					  case SendDtmfRequestInVoiceConfMessage.SEND_DTMF_REQUEST_IN_VOICE_CONF:
						  processSendDtmfRequestInVoiceConfMessage(message);
						  break;
					}
				}
			}
		} else if (channel.equalsIgnoreCase(MessagingConstants.FROM_VOICE_CONF_SYSTEM_CHAN)) {
			//System.out.println("Voice message: " + channel + " " + message);
			JsonParser parser = new JsonParser();
			JsonObject obj = (JsonObject) parser.parse(message);
			if (obj.has("header") && obj.has("payload")) {
				JsonObject header = (JsonObject) obj.get("header");

				if (header.has("name")) {
					String messageName = header.get("name").getAsString();
					switch (messageName) {
					  case UserJoinedVoiceConfMessage.USER_JOINED_VOICE_CONF:
						  processUserJoinedVoiceConfMessage(message);
						  break;	
					  case UserLeftVoiceConfMessage.USER_LEFT_VOICE_CONF:
						  processUserLeftVoiceConfMessage(message);
						  break;
					  case UserLockedInVoiceConfMessage.USER_LOCKED_IN_VOICE_CONF:
						  processUserLockedInVoiceConfMessage(message);
						  break;
					  case UserMutedInVoiceConfMessage.USER_MUTED_IN_VOICE_CONF:
						  processUserMutedInVoiceConfMessage(message);
						  break;
					  case UserTalkingInVoiceConfMessage.USER_TALKING_IN_VOICE_CONF:
						  processUserTalkingInVoiceConfMessage(message);
						  break;
					  case VoiceConfRecordingStartedMessage.VOICE_CONF_RECORDING_STARTED:
						  processVoiceConfRecordingStartedMessage(message);
						  break;
					  case ActiveTalkerChangedInVoiceConfMessage.ACTIVE_TALKER_CHANGED_IN_VOICE_CONF:
						  processActiveTalkerChangedInVoiceConfMessage(message);
						  break;
					  case ChannelCallStateInVoiceConfMessage.CHANNEL_CALL_STATE_IN_VOICE_CONF:
						  processChannelCallStateInVoiceConfMessage(message);
						  break;
					  case ChannelHangupInVoiceConfMessage.CHANNEL_HANGUP_IN_VOICE_CONF:
						  processChannelHangupInVoiceConfMessage(message);
						  break;
					}
				}
			}
		} else if (channel.equalsIgnoreCase(MessagingConstants.FROM_BBB_TRANSCODE_SYSTEM_CHAN)) {
			JsonParser parser = new JsonParser();
			JsonObject obj = (JsonObject) parser.parse(message);
			if (obj.has("header") && obj.has("payload")) {
				JsonObject header = (JsonObject) obj.get("header");

				if (header.has("name")) {
					String messageName = header.get("name").getAsString();
					switch (messageName) {
						case StartTranscoderReplyMessage.START_TRANSCODER_REPLY:
							processStartTranscoderReplyMessage(message);
							break;
						case UpdateTranscoderReplyMessage.UPDATE_TRANSCODER_REPLY:
							processUpdateTranscoderReplyMessage(message);
							break;
						case StopTranscoderReplyMessage.STOP_TRANSCODER_REPLY:
							processStopTranscoderReplyMessage(message);
							break;
						case TranscoderStatusUpdateMessage.TRANSCODER_STATUS_UPDATE:
							processTranscoderStatusUpdateMessage(message);
							break;
						case StartProbingReplyMessage.START_PROBING_REPLY:
							processStartProbingReplyMessage(message);
							break;
					}
				}
			}
		}
	}
	
	private void processUserJoinedVoiceConfMessage(String json) {
		UserJoinedVoiceConfMessage msg = UserJoinedVoiceConfMessage.fromJson(json);
		if (msg != null) {
			bbbInGW.voiceUserJoined(msg.voiceConfId, msg.voiceUserId, msg.userId, msg.callerIdName, msg.callerIdNum, msg.muted, msg.talking, msg.hasVideo, msg.hasFloor);
		}
	}

	private void processUserLeftVoiceConfMessage(String json) {
		UserLeftVoiceConfMessage msg = UserLeftVoiceConfMessage.fromJson(json);
		if (msg != null) {
			bbbInGW.voiceUserLeft(msg.voiceConfId, msg.voiceUserId);
		}
	}

	private void processUserLockedInVoiceConfMessage(String json) {
		UserLockedInVoiceConfMessage msg = UserLockedInVoiceConfMessage.fromJson(json);
		if (msg != null) {
			bbbInGW.voiceUserLocked(msg.voiceConfId, msg.voiceUserId, msg.locked);
		}
	}
	
	private void processUserMutedInVoiceConfMessage(String json) {
		UserMutedInVoiceConfMessage msg = UserMutedInVoiceConfMessage.fromJson(json);
		if (msg != null) {
			bbbInGW.voiceUserMuted(msg.voiceConfId, msg.voiceUserId, msg.muted);
		}
	}

	private void processUserTalkingInVoiceConfMessage(String json) {
		UserTalkingInVoiceConfMessage msg = UserTalkingInVoiceConfMessage.fromJson(json);
		if (msg != null) {
			bbbInGW.voiceUserTalking(msg.voiceConfId, msg.voiceUserId, msg.talking);
		}
	}
	
	private void processVoiceConfRecordingStartedMessage(String json) {
		VoiceConfRecordingStartedMessage msg = VoiceConfRecordingStartedMessage.fromJson(json);
		if (msg != null) {
			bbbInGW.voiceRecording(msg.voiceConfId, msg.recordStream, msg.timestamp, msg.recording);
		}
	}
	
	private void processUserLeavingMessage(String message) {
		  UserLeavingMessage ulm = UserLeavingMessage.fromJson(message);
		  if (ulm != null) {
			  bbbInGW.userLeft(ulm.meetingId, ulm.userId, ulm.meetingId);
		  }		
	}
	
	private void processAssignPresenterRequestMessage(String message) {
		AssignPresenterRequestMessage apm = AssignPresenterRequestMessage.fromJson(message);
		if (apm != null) {
			bbbInGW.assignPresenter(apm.meetingId, apm.newPresenterId, apm.newPresenterName, apm.assignedBy);
		}
	}
	
	private void processUserEmojiStatusMessage(String message) {
		UserEmojiStatusMessage uesm = UserEmojiStatusMessage.fromJson(message);
		if (uesm != null) {
			bbbInGW.userEmojiStatus(uesm.meetingId, uesm.userId, uesm.emojiStatus);
		}
	}
	
	private void processEjectUserFromMeetingRequestMessage(String message) {
		EjectUserFromMeetingRequestMessage eufm = EjectUserFromMeetingRequestMessage.fromJson(message);
		if (eufm != null) {
			bbbInGW.ejectUserFromMeeting(eufm.meetingId, eufm.userId, eufm.ejectedBy);
		}
	}
	
	private void processUserShareWebcamRequestMessage(String message) {
		UserShareWebcamRequestMessage msg = UserShareWebcamRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.shareWebcam(msg.meetingId, msg.userId, msg.stream);
		}
	}
	
	private void processUserUnshareWebcamRequestMessage(String message) {
		UserUnshareWebcamRequestMessage msg = UserUnshareWebcamRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.unshareWebcam(msg.meetingId, msg.userId, msg.stream);
		}
	}
	
	private void processSetUserStatusRequestMessage(String message) {
		SetUserStatusRequestMessage msg = SetUserStatusRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.setUserStatus(msg.meetingId, msg.userId, msg.status, msg.value);
		}
	}
	
	private void processSetRecordingStatusRequestMessage(String message) {
		SetRecordingStatusRequestMessage msg = SetRecordingStatusRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.setRecordingStatus(msg.meetingId, msg.userId, msg.recording);
		}
	}
	
	private void processGetRecordingStatusRequestMessage(String message) {
		GetRecordingStatusRequestMessage msg = GetRecordingStatusRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.getRecordingStatus(msg.meetingId, msg.userId);
		}
	}
	
	private void processGetUsersRequestMessage(String message) {
		GetUsersRequestMessage msg = GetUsersRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.getUsers(msg.meetingId, msg.requesterId);
		}
	}
	
	private void processInitPermissionsSettingMessage(String message) {
		InitPermissionsSettingMessage msg = InitPermissionsSettingMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.initLockSettings(msg.meetingId, msg.permissions);
		}
	}
	
	private void processInitAudioSettingsMessage(String message) {
		InitAudioSettingsMessage msg = InitAudioSettingsMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.initAudioSettings(msg.meetingId, msg.userId, msg.muted);
		}
	}
	
	private void processBroadcastLayoutRequestMessage(String message) {
		BroadcastLayoutRequestMessage msg = BroadcastLayoutRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.broadcastLayout(msg.meetingId, msg.userId, msg.layout);
		}
	}
	
	private void processLockLayoutRequestMessage(String message) {
		LockLayoutRequestMessage msg = LockLayoutRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.lockLayout(msg.meetingId, msg.userId, msg.lock, msg.viewersOnly, msg.layout);
		}
	}
	
	private void processGetCurrentLayoutRequestMessage(String message) {
		GetCurrentLayoutRequestMessage msg = GetCurrentLayoutRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.getCurrentLayout(msg.meetingId, msg.userId);
		}
	}
	
	private void processMuteAllExceptPresenterRequestMessage(String message) {
		MuteAllExceptPresenterRequestMessage msg = MuteAllExceptPresenterRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.muteAllExceptPresenter(msg.meetingId, msg.requesterId, msg.mute);
		}
	}
	
	private void processMuteAllRequestMessage(String message) {
		MuteAllRequestMessage msg = MuteAllRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.muteAllUsers(msg.meetingId, msg.requesterId, msg.mute);
		}
	}
	
	private void processIsMeetingMutedRequestMessage(String message) {
		IsMeetingMutedRequestMessage msg = IsMeetingMutedRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.isMeetingMuted(msg.meetingId, msg.requesterId);
		}		
	}
	
	private void processMuteUserRequestMessage(String message) {
		MuteUserRequestMessage msg = MuteUserRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.muteUser(msg.meetingId, msg.requesterId, msg.userId, msg.mute);
		}		
	}
	
	private void processLockMuteUserRequestMessage(String message) {
		LockMuteUserRequestMessage msg = LockMuteUserRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.lockMuteUser(msg.meetingId, msg.requesterId, msg.userId, msg.lock);
		}		
	}
	
	private void processEjectUserFromVoiceRequestMessage(String message) {
		EjectUserFromVoiceRequestMessage msg = EjectUserFromVoiceRequestMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.ejectUserFromVoice(msg.meetingId, msg.userId, msg.requesterId);
		}		
	}

	private void processOutboundDialRequestInVoiceConfMessage(String message) {
		OutboundDialRequestInVoiceConfMessage msg = OutboundDialRequestInVoiceConfMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.voiceOutboundDialRequest(msg.meetingId, msg.userId, msg.options, msg.params);
		}
	}

	private void processCancelDialRequestInVoiceConfMessage(String message) {
		CancelDialRequestInVoiceConfMessage msg = CancelDialRequestInVoiceConfMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.voiceCancelDialRequest(msg.meetingId, msg.userId, msg.uniqueId);
		}
	}

	private void processSendDtmfRequestInVoiceConfMessage(String message) {
		SendDtmfRequestInVoiceConfMessage msg = SendDtmfRequestInVoiceConfMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.voiceSendDtmfRequest(msg.meetingId, msg.userId, msg.uniqueId, msg.dtmfDigit);
		}
	}

	private void processActiveTalkerChangedInVoiceConfMessage(String message) {
		ActiveTalkerChangedInVoiceConfMessage msg = ActiveTalkerChangedInVoiceConfMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.activeTalkerChanged(msg.voiceConfId, msg.voiceUserId);
		}
	}

	private void processChannelCallStateInVoiceConfMessage(String message) {
		ChannelCallStateInVoiceConfMessage msg = ChannelCallStateInVoiceConfMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.voiceDialing(msg.voiceConfId, msg.userId, msg.uniqueId, msg.callState);
		}
	}

	private void processChannelHangupInVoiceConfMessage(String message) {
		ChannelHangupInVoiceConfMessage msg = ChannelHangupInVoiceConfMessage.fromJson(message);
		if (msg != null) {
			bbbInGW.voiceHangingUp(msg.voiceConfId, msg.userId, msg.uniqueId, msg.callState, msg.hangupCause);
		}
	}

	private void processStartTranscoderReplyMessage(String message) {
		StartTranscoderReplyMessage msg = StartTranscoderReplyMessage.fromJson(message);
		if(msg !=null){
			bbbInGW.startTranscoderReply(msg.meetingId, msg.transcoderId, msg.params);
		}
	}

	private void processUpdateTranscoderReplyMessage(String message) {
		UpdateTranscoderReplyMessage msg = UpdateTranscoderReplyMessage.fromJson(message);
		if(msg !=null){
			bbbInGW.updateTranscoderReply(msg.meetingId, msg.transcoderId, msg.params);
		}
	}

	private void processStopTranscoderReplyMessage(String message) {
		StopTranscoderReplyMessage msg = StopTranscoderReplyMessage.fromJson(message);
		if(msg !=null){
			bbbInGW.stopTranscoderReply(msg.meetingId, msg.transcoderId);
		}
	}

	private void processTranscoderStatusUpdateMessage(String message) {
		TranscoderStatusUpdateMessage msg = TranscoderStatusUpdateMessage.fromJson(message);
		if(msg !=null){
			bbbInGW.transcoderStatusUpdate(msg.meetingId, msg.transcoderId, msg.params);
		}
	}

	private void processStartProbingReplyMessage(String message) {
		StartProbingReplyMessage msg = StartProbingReplyMessage.fromJson(message);
		if (msg != null){
			bbbInGW.startProbingReply(msg.meetingId, msg.transcoderId, msg.params);
		}
	}
}

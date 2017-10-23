package org.bigbluebutton.core

import org.bigbluebutton.core.bus._
import org.bigbluebutton.core.api._
import scala.collection.JavaConversions._
import java.util.ArrayList
import scala.collection.mutable.ArrayBuffer
import org.bigbluebutton.core.apps.Page
import org.bigbluebutton.core.apps.Presentation
import akka.actor.ActorSystem
import org.bigbluebutton.core.apps.AnnotationVO
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure
import org.bigbluebutton.core.service.recorder.RecorderApplication
import org.bigbluebutton.common.messages.IBigBlueButtonMessage
import org.bigbluebutton.common.messages.StartCustomPollRequestMessage
import org.bigbluebutton.common.messages.PubSubPingMessage
import org.bigbluebutton.messages._
import org.bigbluebutton.messages.payload._
import akka.event.Logging
import spray.json.JsonParser

class BigBlueButtonInGW(
    val system: ActorSystem,
    eventBus: IncomingEventBus,
    outGW: OutMessageGateway,
    val red5DeskShareIP: String,
    val red5DeskShareApp: String) extends IBigBlueButtonInGW {

  val log = Logging(system, getClass)
  val bbbActor = system.actorOf(BigBlueButtonActor.props(system, eventBus, outGW), "bigbluebutton-actor")
  eventBus.subscribe(bbbActor, "meeting-manager")

  def handleBigBlueButtonMessage(message: IBigBlueButtonMessage) {
    message match {
      case msg: StartCustomPollRequestMessage => {
        eventBus.publish(
          BigBlueButtonEvent(
            msg.payload.meetingId,
            new StartCustomPollRequest(
              msg.payload.meetingId,
              msg.payload.requesterId,
              msg.payload.pollId,
              msg.payload.pollType,
              msg.payload.answers)))
      }
      case msg: PubSubPingMessage => {
        eventBus.publish(
          BigBlueButtonEvent(
            "meeting-manager",
            new PubSubPing(msg.payload.system, msg.payload.timestamp)))
      }

      case msg: CreateMeetingRequest => {
        val mProps = new MeetingProperties(
          msg.payload.id,
          msg.payload.externalId,
          msg.payload.parentId,
          msg.payload.name,
          msg.payload.record,
          msg.payload.voiceConfId,
          msg.payload.voiceConfId + "-DESKSHARE", // WebRTC Desktop conference id
          msg.payload.durationInMinutes,
          msg.payload.autoStartRecording,
          msg.payload.allowStartStopRecording,
          msg.payload.webcamsOnlyForModerator,
          msg.payload.moderatorPassword,
          msg.payload.viewerPassword,
          msg.payload.createTime,
          msg.payload.createDate,
          red5DeskShareIP, red5DeskShareApp,
          msg.payload.isBreakout,
          msg.payload.sequence,
          msg.payload.metadata,
          "unknown_kurento_token")

        eventBus.publish(BigBlueButtonEvent("meeting-manager", new CreateMeeting(msg.payload.id, mProps)))
      }
    }
  }

  def handleJsonMessage(json: String) {
    JsonMessageDecoder.decode(json) match {
      case Some(validMsg) => forwardMessage(validMsg)
      case None => log.error("Unhandled json message: {}", json)
    }
  }

  def forwardMessage(msg: InMessage) = {
    msg match {
      case m: BreakoutRoomsListMessage => eventBus.publish(BigBlueButtonEvent(m.meetingId, m))
      case m: CreateBreakoutRooms => eventBus.publish(BigBlueButtonEvent(m.meetingId, m))
      case m: RequestBreakoutJoinURLInMessage => eventBus.publish(BigBlueButtonEvent(m.meetingId, m))
      case m: TransferUserToMeetingRequest => eventBus.publish(BigBlueButtonEvent(m.meetingId, m))
      case m: EndAllBreakoutRooms => eventBus.publish(BigBlueButtonEvent(m.meetingId, m))
      case _ => log.error("Unhandled message: {}", msg)
    }
  }

  def destroyMeeting(meetingID: String) {
    forwardMessage(new EndAllBreakoutRooms(meetingID))
    eventBus.publish(
      BigBlueButtonEvent(
        "meeting-manager",
        new DestroyMeeting(
          meetingID)))
  }

  def getAllMeetings(meetingID: String) {
    eventBus.publish(BigBlueButtonEvent("meeting-manager", new GetAllMeetingsRequest("meetingId")))
  }

  def isAliveAudit(aliveId: String) {
    eventBus.publish(BigBlueButtonEvent("meeting-manager", new KeepAliveMessage(aliveId)))
  }

  def lockSettings(meetingID: String, locked: java.lang.Boolean,
    lockSettings: java.util.Map[String, java.lang.Boolean]) {
  }

  def statusMeetingAudit(meetingID: String) {

  }

  def endMeeting(meetingId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new EndMeeting(meetingId)))
  }

  def endAllMeetings() {

  }

  def activityResponse(meetingId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new ActivityResponse(meetingId)))
  }

  def startMediaSource(meetingId: String, mediaSourceId: String, mediaSourceUri: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StartMediaSource(meetingId, mediaSourceId, mediaSourceUri)))
  }

  def stopMediaSource(meetingId: String, mediaSourceId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StopMediaSource(meetingId, mediaSourceId)))
  }

  def setMeetingDesksharePresent(meetingId: String, desksharePresent: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new SetMeetingDesksharePresent(meetingId, desksharePresent)))
  }

  def getDeskshareStatusRequest(meetingId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new GetDeskshareStatusRequest(meetingId)))
  }

  /**
   * ***********************************************************
   * Message Interface for Users
   * ***********************************************************
   */
  def validateAuthToken(meetingId: String, userId: String, token: String, correlationId: String, sessionId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new ValidateAuthToken(meetingId, userId, token, correlationId, sessionId)))
  }

  def registerUser(meetingID: String, userID: String, name: String, role: String, extUserID: String, authToken: String, avatarURL: String, guest: java.lang.Boolean): Unit = {
    val userRole = if (role == "MODERATOR") Role.MODERATOR else Role.VIEWER
    eventBus.publish(BigBlueButtonEvent(meetingID, new RegisterUser(meetingID, userID, name, userRole, extUserID, authToken, avatarURL, guest)))
  }

  def sendLockSettings(meetingID: String, userId: String, settings: java.util.Map[String, java.lang.Boolean]) {
    // Convert java.util.Map to scala.collection.immutable.Map
    // settings.mapValues -> convaert java Map to scala mutable Map
    // v => v.booleanValue() -> convert java Boolean to Scala Boolean
    // toMap -> converts from scala mutable map to scala immutable map
    val s = settings.mapValues(v => v.booleanValue() /* convert java Boolean to Scala Boolean */ ).toMap
    val disableCam = s.getOrElse("disableCam", false)
    val disableMic = s.getOrElse("disableMic", false)
    val disablePrivChat = s.getOrElse("disablePrivateChat", false)
    val disablePubChat = s.getOrElse("disablePublicChat", false)
    val lockedLayout = s.getOrElse("lockedLayout", false)
    var lockOnJoin = s.getOrElse("lockOnJoin", false)
    var lockOnJoinConfigurable = s.getOrElse("lockOnJoinConfigurable", false)

    val permissions = new Permissions(disableCam = disableCam,
      disableMic = disableMic,
      disablePrivChat = disablePrivChat,
      disablePubChat = disablePubChat,
      lockedLayout = lockedLayout,
      lockOnJoin = lockOnJoin,
      lockOnJoinConfigurable = lockOnJoinConfigurable)

    eventBus.publish(BigBlueButtonEvent(meetingID, new SetLockSettings(meetingID, userId, permissions)))
  }

  def initLockSettings(meetingID: String, settings: java.util.Map[String, java.lang.Boolean]) {
    // Convert java.util.Map to scala.collection.immutable.Map
    // settings.mapValues -> convert java Map to scala mutable Map
    // v => v.booleanValue() -> convert java Boolean to Scala Boolean
    // toMap -> converts from scala mutable map to scala immutable map
    val s = settings.mapValues(v => v.booleanValue() /* convert java Boolean to Scala Boolean */ ).toMap
    val disableCam = s.getOrElse("disableCam", false)
    val disableMic = s.getOrElse("disableMic", false)
    val disablePrivChat = s.getOrElse("disablePrivateChat", false)
    val disablePubChat = s.getOrElse("disablePublicChat", false)
    val lockedLayout = s.getOrElse("lockedLayout", false)
    val lockOnJoin = s.getOrElse("lockOnJoin", false)
    val lockOnJoinConfigurable = s.getOrElse("lockOnJoinConfigurable", false)
    val permissions = new Permissions(disableCam = disableCam,
      disableMic = disableMic,
      disablePrivChat = disablePrivChat,
      disablePubChat = disablePubChat,
      lockedLayout = lockedLayout,
      lockOnJoin = lockOnJoin,
      lockOnJoinConfigurable = lockOnJoinConfigurable)

    eventBus.publish(BigBlueButtonEvent(meetingID, new InitLockSettings(meetingID, permissions)))
  }

  def initAudioSettings(meetingID: String, requesterID: String, muted: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new InitAudioSettings(meetingID, requesterID, muted.booleanValue())))
  }

  def getLockSettings(meetingId: String, userId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new GetLockSettings(meetingId, userId)))
  }

  def lockUser(meetingId: String, requesterID: String, lock: Boolean, userId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new LockUserRequest(meetingId, requesterID, userId, lock)))
  }

  def setRecordingStatus(meetingId: String, userId: String, recording: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new SetRecordingStatus(meetingId, userId, recording.booleanValue())))
  }

  def getRecordingStatus(meetingId: String, userId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new GetRecordingStatus(meetingId, userId)))
  }

  // Users
  def userEmojiStatus(meetingId: String, userId: String, emojiStatus: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new UserEmojiStatus(meetingId, userId, emojiStatus)))
  }

  def ejectUserFromMeeting(meetingId: String, userId: String, ejectedBy: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new EjectUserFromMeeting(meetingId, userId, ejectedBy)))
  }

  def logoutEndMeeting(meetingId: String, userId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new LogoutEndMeeting(meetingId, userId)))
  }

  def shareWebcam(meetingId: String, userId: String, stream: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new UserShareWebcam(meetingId, userId, stream)))
  }

  def unshareWebcam(meetingId: String, userId: String, stream: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new UserUnshareWebcam(meetingId, userId, stream)))
  }

  def setUserStatus(meetingID: String, userID: String, status: String, value: Object) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new ChangeUserStatus(meetingID, userID, status, value)))
  }

  def setUserRole(meetingID: String, userID: String, role: String) {
    val userRole = if (role == "MODERATOR") Role.MODERATOR else Role.VIEWER
    eventBus.publish(BigBlueButtonEvent(meetingID, new ChangeUserRole(meetingID, userID, userRole)))
  }

  def getUsers(meetingID: String, requesterID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new GetUsers(meetingID, requesterID)))
  }

  def userLeft(meetingID: String, userID: String, sessionId: String): Unit = {
    eventBus.publish(BigBlueButtonEvent(meetingID, new UserLeaving(meetingID, userID, sessionId)))
  }

  def userJoin(meetingID: String, userID: String, authToken: String): Unit = {
    eventBus.publish(BigBlueButtonEvent(meetingID, new UserJoining(meetingID, userID, authToken)))
  }

  def checkIfAllowedToShareDesktop(meetingID: String, userID: String): Unit = {
    eventBus.publish(BigBlueButtonEvent(meetingID, AllowUserToShareDesktop(meetingID: String,
      userID: String)))
  }

  def assignPresenter(meetingID: String, newPresenterID: String, newPresenterName: String, assignedBy: String): Unit = {
    eventBus.publish(BigBlueButtonEvent(meetingID, new AssignPresenter(meetingID, newPresenterID, newPresenterName, assignedBy)))
  }

  def getCurrentPresenter(meetingID: String, requesterID: String): Unit = {
    // do nothing
  }

  def userConnectedToGlobalAudio(voiceConf: String, userid: String, name: String) {
    // we are required to pass the meeting_id as first parameter (just to satisfy trait)
    // but it's not used anywhere. That's why we pass voiceConf twice instead
    eventBus.publish(BigBlueButtonEvent(voiceConf, new UserConnectedToGlobalAudio(voiceConf, voiceConf, userid, name)))
  }

  def userDisconnectedFromGlobalAudio(voiceConf: String, userid: String, name: String) {
    // we are required to pass the meeting_id as first parameter (just to satisfy trait)
    // but it's not used anywhere. That's why we pass voiceConf twice instead
    eventBus.publish(BigBlueButtonEvent(voiceConf, new UserDisconnectedFromGlobalAudio(voiceConf, voiceConf, userid, name)))
  }

  /**
   * ***********************************************************************
   * Message Interface for Guest
   * *******************************************************************
   */

  def getGuestPolicy(meetingId: String, requesterId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new GetGuestPolicy(meetingId, requesterId)))
  }

  def setGuestPolicy(meetingId: String, guestPolicy: String, requesterId: String) {
    val policy = guestPolicy.toUpperCase() match {
      case "ALWAYS_ACCEPT" => GuestPolicy.ALWAYS_ACCEPT
      case "ALWAYS_DENY" => GuestPolicy.ALWAYS_DENY
      case "ASK_MODERATOR" => GuestPolicy.ASK_MODERATOR
      //default
      case undef => GuestPolicy.ASK_MODERATOR
    }
    eventBus.publish(BigBlueButtonEvent(meetingId, new SetGuestPolicy(meetingId, policy, requesterId)))
  }

  def responseToGuest(meetingId: String, userId: String, response: java.lang.Boolean, requesterId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new RespondToGuest(meetingId, userId, response, requesterId)))
  }

  /**
   * ************************************************************************************
   * Message Interface for Presentation
   * ************************************************************************************
   */

  def clear(meetingID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new ClearPresentation(meetingID)))
  }

  def sendConversionUpdate(messageKey: String, meetingId: String, code: String, presentationId: String, presName: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new PresentationConversionUpdate(meetingId, messageKey, code, presentationId, presName)))
  }

  def sendPageCountError(messageKey: String, meetingId: String, code: String, presentationId: String, numberOfPages: Int, maxNumberPages: Int, presName: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new PresentationPageCountError(meetingId, messageKey, code, presentationId, numberOfPages, maxNumberPages, presName)))
  }

  def sendSlideGenerated(messageKey: String, meetingId: String, code: String, presentationId: String, numberOfPages: Int, pagesCompleted: Int, presName: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new PresentationSlideGenerated(meetingId, messageKey, code, presentationId, numberOfPages, pagesCompleted, presName)))
  }

  def generatePresentationPages(presId: String, numPages: Int, presBaseUrl: String): scala.collection.immutable.HashMap[String, Page] = {
    var pages = new scala.collection.immutable.HashMap[String, Page]
    val baseUrl =
      for (i <- 1 to numPages) {
        val id = presId + "/" + i
        val num = i;
        val current = if (i == 1) true else false
        val thumbnail = presBaseUrl + "/thumbnail/" + i
        val swfUri = presBaseUrl + "/slide/" + i

        val txtUri = presBaseUrl + "/textfiles/" + i
        val svgUri = presBaseUrl + "/svg/" + i

        val p = new Page(id = id, num = num, thumbUri = thumbnail, swfUri = swfUri,
          txtUri = txtUri, svgUri = svgUri,
          current = current)
        pages += (p.id -> p)
      }

    pages
  }

  def sendConversionCompleted(messageKey: String, meetingId: String, code: String, presentationId: String, numPages: Int, presName: String, presBaseUrl: String, downloadable: Boolean) {

    val pages = generatePresentationPages(presentationId, numPages, presBaseUrl)
    val presentation = new Presentation(id = presentationId, name = presName, pages = pages, downloadable = downloadable)
    eventBus.publish(BigBlueButtonEvent(meetingId, new PresentationConversionCompleted(meetingId, messageKey, code, presentation)))

  }

  def removePresentation(meetingID: String, presentationID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new RemovePresentation(meetingID, presentationID)))
  }

  def getPresentationInfo(meetingID: String, requesterID: String, replyTo: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new GetPresentationInfo(meetingID, requesterID, replyTo)))
  }

  def sendCursorUpdate(meetingID: String, xPercent: Double, yPercent: Double) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new SendCursorUpdate(meetingID, xPercent, yPercent)))
  }

  def resizeAndMoveSlide(meetingID: String, xOffset: Double, yOffset: Double, widthRatio: Double, heightRatio: Double) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new ResizeAndMoveSlide(meetingID, xOffset, yOffset, widthRatio, heightRatio)))
  }

  def gotoSlide(meetingID: String, pageId: String) {
    //	  println("**** Forwarding GotoSlide for meeting[" + meetingID + "] ****")
    eventBus.publish(BigBlueButtonEvent(meetingID, new GotoSlide(meetingID, pageId)))
  }

  def sharePresentation(meetingID: String, presentationID: String, share: Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new SharePresentation(meetingID, presentationID, share)))
  }

  def getSlideInfo(meetingID: String, requesterID: String, replyTo: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new GetSlideInfo(meetingID, requesterID, replyTo)))
  }

  /**
   * ***********************************************************************
   * Message Interface for Layout
   * *******************************************************************
   */

  def getCurrentLayout(meetingID: String, requesterID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new GetCurrentLayoutRequest(meetingID, requesterID)))
  }

  def broadcastLayout(meetingID: String, requesterID: String, layout: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new BroadcastLayoutRequest(meetingID, requesterID, layout)))
  }

  def lockLayout(meetingId: String, setById: String, lock: Boolean, viewersOnly: Boolean, layout: String) {
    if (layout != null) {
      eventBus.publish(BigBlueButtonEvent(meetingId, new LockLayoutRequest(meetingId, setById, lock, viewersOnly, Some(layout))))
    } else {
      eventBus.publish(BigBlueButtonEvent(meetingId, new LockLayoutRequest(meetingId, setById, lock, viewersOnly, None)))
    }

  }

  /**
   * *******************************************************************
   * Message Interface for Chat
   * *****************************************************************
   */

  def getChatHistory(meetingID: String, requesterID: String, replyTo: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new GetChatHistoryRequest(meetingID, requesterID, replyTo)))
  }

  def sendPublicMessage(meetingID: String, requesterID: String, message: java.util.Map[String, String]) {
    // Convert java Map to Scala Map, then convert Mutable map to immutable map
    eventBus.publish(BigBlueButtonEvent(meetingID, new SendPublicMessageRequest(meetingID, requesterID, mapAsScalaMap(message).toMap)))
  }

  def sendPrivateMessage(meetingID: String, requesterID: String, message: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new SendPrivateMessageRequest(meetingID, requesterID, mapAsScalaMap(message).toMap)))
  }

  def clearPublicChatHistory(meetingID: String, requesterID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new ClearPublicChatHistoryRequest(meetingID, requesterID)))
  }

  /**
   * *******************************************************************
   * Message Interface for Whiteboard
   * *****************************************************************
   */
  private def buildAnnotation(annotation: scala.collection.mutable.Map[String, Object]): Option[AnnotationVO] = {
    var shape: Option[AnnotationVO] = None

    val id = annotation.getOrElse("id", null).asInstanceOf[String]
    val shapeType = annotation.getOrElse("type", null).asInstanceOf[String]
    val status = annotation.getOrElse("status", null).asInstanceOf[String]
    val wbId = annotation.getOrElse("whiteboardId", null).asInstanceOf[String]
    //    println("** GOT ANNOTATION status[" + status + "] shape=[" + shapeType + "]");

    if (id != null && shapeType != null && status != null && wbId != null) {
      shape = Some(new AnnotationVO(id, status, shapeType, annotation.toMap, wbId))
    }

    shape
  }

  def sendWhiteboardAnnotation(meetingID: String, requesterID: String, annotation: java.util.Map[String, Object]) {
    val ann: scala.collection.mutable.Map[String, Object] = mapAsScalaMap(annotation)

    buildAnnotation(ann) match {
      case Some(shape) => {
        eventBus.publish(BigBlueButtonEvent(meetingID, new SendWhiteboardAnnotationRequest(meetingID, requesterID, shape)))
      }
      case None => // do nothing
    }
  }

  def requestWhiteboardAnnotationHistory(meetingID: String, requesterID: String, whiteboardId: String, replyTo: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new GetWhiteboardShapesRequest(meetingID, requesterID, whiteboardId, replyTo)))
  }

  def clearWhiteboard(meetingID: String, requesterID: String, whiteboardId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new ClearWhiteboardRequest(meetingID, requesterID, whiteboardId)))
  }

  def undoWhiteboard(meetingID: String, requesterID: String, whiteboardId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new UndoWhiteboardRequest(meetingID, requesterID, whiteboardId)))
  }

  def enableWhiteboard(meetingID: String, requesterID: String, enable: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new EnableWhiteboardRequest(meetingID, requesterID, enable)))
  }

  def isWhiteboardEnabled(meetingID: String, requesterID: String, replyTo: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new IsWhiteboardEnabledRequest(meetingID, requesterID, replyTo)))
  }

  /**
   * *******************************************************************
   * Message Interface for Voice
   * *****************************************************************
   */

  def muteAllExceptPresenter(meetingID: String, requesterID: String, mute: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new MuteAllExceptPresenterRequest(meetingID, requesterID, mute)))
  }

  def muteAllUsers(meetingID: String, requesterID: String, mute: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new MuteMeetingRequest(meetingID, requesterID, mute)))
  }

  def isMeetingMuted(meetingID: String, requesterID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new IsMeetingMutedRequest(meetingID, requesterID)))
  }

  def muteUser(meetingID: String, requesterID: String, userID: String, mute: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new MuteUserRequest(meetingID, requesterID, userID, mute)))
  }

  def lockMuteUser(meetingID: String, requesterID: String, userID: String, lock: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new LockUserRequest(meetingID, requesterID, userID, lock)))
  }

  def ejectUserFromVoice(meetingId: String, userId: String, ejectedBy: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new EjectUserFromVoiceRequest(meetingId, userId, ejectedBy)))
  }

  def voiceUserJoined(voiceConfId: String, voiceUserId: String, userId: String, callerIdName: String,
    callerIdNum: String, muted: java.lang.Boolean, avatarURL: String, talking: java.lang.Boolean,
    hasVideo: java.lang.Boolean, hasFloor: java.lang.Boolean) {

    eventBus.publish(BigBlueButtonEvent(voiceConfId, new UserJoinedVoiceConfMessage(voiceConfId, voiceUserId, userId, userId, callerIdName,
      callerIdNum, muted, talking, avatarURL, false /*hardcode listenOnly to false as the message for listenOnly is ConnectedToGlobalAudio*/ , hasVideo, hasFloor)))
  }

  def voiceUserLeft(voiceConfId: String, voiceUserId: String) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new UserLeftVoiceConfMessage(voiceConfId, voiceUserId)))
  }

  def voiceUserLocked(voiceConfId: String, voiceUserId: String, locked: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new UserLockedInVoiceConfMessage(voiceConfId, voiceUserId, locked)))
  }

  def voiceUserMuted(voiceConfId: String, voiceUserId: String, muted: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new UserMutedInVoiceConfMessage(voiceConfId, voiceUserId, muted)))
  }

  def voiceUserTalking(voiceConfId: String, voiceUserId: String, talking: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new UserTalkingInVoiceConfMessage(voiceConfId, voiceUserId, talking)))
  }

  def voiceRecording(voiceConfId: String, recordingFile: String, timestamp: String, recording: java.lang.Boolean) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new VoiceConfRecordingStartedMessage(voiceConfId, recordingFile, recording, timestamp)))
  }

  /**
   * *******************************************************************
   * Message Interface for DeskShare
   * *****************************************************************
   */
  def deskShareStarted(confId: String, callerId: String, callerIdName: String) {
    println("____BigBlueButtonInGW::deskShareStarted " + confId + callerId + "    " +
      callerIdName)
    eventBus.publish(BigBlueButtonEvent(confId, new DeskShareStartedRequest(confId, callerId,
      callerIdName)))
  }

  def deskShareStopped(meetingId: String, callerId: String, callerIdName: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new DeskShareStoppedRequest(meetingId, callerId, callerIdName)))
  }

  def deskShareRTMPBroadcastStarted(meetingId: String, streamname: String, videoWidth: Int, videoHeight: Int, timestamp: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new DeskShareRTMPBroadcastStartedRequest(meetingId, streamname, videoWidth, videoHeight, timestamp)))
  }

  def deskShareRTMPBroadcastStopped(meetingId: String, streamname: String, videoWidth: Int, videoHeight: Int, timestamp: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new DeskShareRTMPBroadcastStoppedRequest(meetingId, streamname, videoWidth, videoHeight, timestamp)))
  }

  def deskShareGetInfoRequest(meetingId: String, requesterId: String, replyTo: String): Unit = {
    eventBus.publish(BigBlueButtonEvent(meetingId, new DeskShareGetDeskShareInfoRequest(meetingId, requesterId, replyTo)))
  }

  def voiceOutboundDialRequest(meetingId: String, userId: String, options: java.util.Map[String, String], params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new VoiceOutboundDialRequest(meetingId, userId, mapAsScalaMap(options).toMap, mapAsScalaMap(params).toMap)))
  }

  def voiceCancelDialRequest(meetingId: String, userId: String, uuid: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new VoiceCancelDialRequest(meetingId, userId, uuid)))
  }

  def voiceSendDtmfRequest(meetingId: String, userId: String, uuid: String, dtmfDigit: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new VoiceSendDtmfRequest(meetingId, userId, uuid, dtmfDigit)))
  }

  def voiceDialing(voiceConfId: String, userId: String, uuid: String, callState: String) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new VoiceDialing(voiceConfId, userId, uuid, callState)))
  }

  def voiceHangingUp(voiceConfId: String, userId: String, uuid: String, callState: String, hangupCause: String) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new VoiceHangingUp(voiceConfId, userId, uuid, callState, hangupCause)))
  }

  def activeTalkerChanged(voiceConfId: String, voiceUserId: String) {
    eventBus.publish(BigBlueButtonEvent(voiceConfId, new ActiveTalkerChanged(voiceConfId, voiceUserId)))
  }

  // Polling
  def votePoll(meetingId: String, userId: String, pollId: String, questionId: Integer, answerId: Integer) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new RespondToPollRequest(meetingId, userId, pollId, questionId, answerId)))
  }

  def startPoll(meetingId: String, requesterId: String, pollId: String, pollType: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StartPollRequest(meetingId, requesterId, pollId, pollType)))
  }

  def stopPoll(meetingId: String, userId: String, pollId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StopPollRequest(meetingId, userId)))
  }

  def showPollResult(meetingId: String, requesterId: String, pollId: String, show: java.lang.Boolean) {
    if (show) {
      eventBus.publish(BigBlueButtonEvent(meetingId, new ShowPollResultRequest(meetingId, requesterId, pollId)))
    } else {
      eventBus.publish(BigBlueButtonEvent(meetingId, new HidePollResultRequest(meetingId, requesterId, pollId)))
    }
  }

  /**
   * *******************************************************************
   * Message Interface for Caption
   * *****************************************************************
   */

  def sendCaptionHistory(meetingID: String, requesterID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new SendCaptionHistoryRequest(meetingID, requesterID)))
  }

  def updateCaptionOwner(meetingID: String, locale: String, localeCode: String, ownerID: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new UpdateCaptionOwnerRequest(meetingID, locale, localeCode, ownerID)))
  }

  def editCaptionHistory(meetingID: String, userID: String, startIndex: Integer, endIndex: Integer, locale: String, localeCode: String, text: String) {
    eventBus.publish(BigBlueButtonEvent(meetingID, new EditCaptionHistoryRequest(meetingID, userID, startIndex, endIndex, locale, localeCode, text)))
  }

  /**
   * *******************************************************************
   * Message Interface for Shared Notes
   * *****************************************************************
   */

  def patchDocument(meetingId: String, userId: String, noteId: String, patch: String, operation: String) {
    val sharedNotesOperation = operation.toUpperCase() match {
      case "PATCH" => SharedNotesOperation.PATCH
      case "UNDO" => SharedNotesOperation.UNDO
      case "REDO" => SharedNotesOperation.REDO
      case _ => SharedNotesOperation.UNDEFINED
    }
    eventBus.publish(BigBlueButtonEvent(meetingId, new PatchDocumentRequest(meetingId, userId, noteId, patch, sharedNotesOperation)))
  }

  def getCurrentDocument(meetingId: String, userId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new GetCurrentDocumentRequest(meetingId, userId)))
  }

  def createAdditionalNotes(meetingId: String, userId: String, noteName: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new CreateAdditionalNotesRequest(meetingId, userId, noteName)))
  }

  def destroyAdditionalNotes(meetingId: String, userId: String, noteId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new DestroyAdditionalNotesRequest(meetingId, userId, noteId)))
  }

  def requestAdditionalNotesSet(meetingId: String, userId: String, additionalNotesSetSize: Int) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new RequestAdditionalNotesSetRequest(meetingId, userId, additionalNotesSetSize)))
  }

  def sharedNotesSyncNoteRequest(meetingId: String, userId: String, noteId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new SharedNotesSyncNoteRequest(meetingId, userId, noteId)))
  }

  /**
   * *******************************************************************
   * Message Interface for transcode
   * *****************************************************************
   */

  def startTranscoderReply(meetingId: String, transcoderId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StartTranscoderReply(meetingId, transcoderId, mapAsScalaMap(params).toMap)))
  }

  def updateTranscoderReply(meetingId: String, transcoderId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new UpdateTranscoderReply(meetingId, transcoderId, mapAsScalaMap(params).toMap)))
  }

  def stopTranscoderReply(meetingId: String, transcoderId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StopTranscoderReply(meetingId, transcoderId)))
  }

  def transcoderStatusUpdate(meetingId: String, transcoderId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new TranscoderStatusUpdate(meetingId, transcoderId, mapAsScalaMap(params).toMap)))
  }

  def startProbingReply(meetingId: String, transcoderId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StartProbingReply(meetingId, transcoderId, mapAsScalaMap(params).toMap)))
  }

  /**
   * *******************************************************************
   * Message Interface for kurento
   * *****************************************************************
   */

  def allMediaSourcesStopped(meetingId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new AllMediaSourcesStopped(meetingId)))
  }

  def startKurentoRtpReply(meetingId: String, kurentoEndpointId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StartKurentoRtpReply(meetingId, kurentoEndpointId, mapAsScalaMap(params).toMap)))
  }

  def stopKurentoRtpReply(meetingId: String, kurentoEndpointId: String) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StopKurentoRtpReply(meetingId, kurentoEndpointId)))
  }

  def updateKurentoRtp(meetingId: String, kurentoEndpointId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new UpdateKurentoRtp(meetingId, kurentoEndpointId, mapAsScalaMap(params).toMap)))
  }

  def updateKurentoToken(token: String) {
    eventBus.publish(BigBlueButtonEvent("meeting-manager", new UpdateKurentoToken(token)))
  }

  def startKurentoSendRtpReply(meetingId: String, params: java.util.Map[String, String]) {
    eventBus.publish(BigBlueButtonEvent(meetingId, new StartKurentoSendRtpReply(meetingId, mapAsScalaMap(params).toMap)))
  }
}

package org.bigbluebutton.core

import akka.actor._
import akka.actor.ActorLogging
import akka.actor.SupervisorStrategy.Resume
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import java.io.{ PrintWriter, StringWriter }
import scala.concurrent.duration._
import org.bigbluebutton.core.bus._
import org.bigbluebutton.core.api._
import org.bigbluebutton.SystemConfiguration

import java.util.concurrent.TimeUnit

object BigBlueButtonActor extends SystemConfiguration {
  def props(system: ActorSystem,
    eventBus: IncomingEventBus,
    outGW: OutMessageGateway): Props =
    Props(classOf[BigBlueButtonActor], system, eventBus, outGW)
}

class BigBlueButtonActor(val system: ActorSystem,
    eventBus: IncomingEventBus, outGW: OutMessageGateway) extends Actor with ActorLogging {

  implicit def executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  private var meetings = new collection.immutable.HashMap[String, RunningMeeting]

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case e: Exception => {
      val sw: StringWriter = new StringWriter()
      sw.write("An exception has been thrown on BigBlueButtonActor, exception message [" + e.getMessage() + "] (full stacktrace below)\n")
      e.printStackTrace(new PrintWriter(sw))
      log.error(sw.toString())
      Resume
    }
  }

  private var kurentoToken = "unknown_kurento_token"

  def receive = {
    case msg: CreateMeeting => handleCreateMeeting(msg)
    case msg: DestroyMeeting => handleDestroyMeeting(msg)
    case msg: KeepAliveMessage => handleKeepAliveMessage(msg)
    case msg: PubSubPing => handlePubSubPingMessage(msg)
    case msg: ValidateAuthToken => handleValidateAuthToken(msg)
    case msg: GetAllMeetingsRequest => handleGetAllMeetingsRequest(msg)
    case msg: UserJoinedVoiceConfMessage => handleUserJoinedVoiceConfMessage(msg)
    case msg: UserLeftVoiceConfMessage => handleUserLeftVoiceConfMessage(msg)
    case msg: UserLockedInVoiceConfMessage => handleUserLockedInVoiceConfMessage(msg)
    case msg: UserMutedInVoiceConfMessage => handleUserMutedInVoiceConfMessage(msg)
    case msg: UserTalkingInVoiceConfMessage => handleUserTalkingInVoiceConfMessage(msg)
    case msg: VoiceConfRecordingStartedMessage => handleVoiceConfRecordingStartedMessage(msg)
    case msg: VoiceDialing => handleVoiceDialing(msg)
    case msg: VoiceHangingUp => handleVoiceHangingUp(msg)
    case msg: ActiveTalkerChanged => handleActiveTalkerChanged(msg)
    case msg: UpdateKurentoToken => handleUpdateKurentoToken(msg)
    case _ => // do nothing
  }

  private def findMeetingWithVoiceConfId(voiceConfId: String): Option[RunningMeeting] = {
    meetings.values.find(m => { m.mProps.voiceBridge == voiceConfId })
  }

  private def handleUserJoinedVoiceConfMessage(msg: UserJoinedVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m => m.actorRef ! msg }
  }

  private def handleUserLeftVoiceConfMessage(msg: UserLeftVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleUserLockedInVoiceConfMessage(msg: UserLockedInVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleUserMutedInVoiceConfMessage(msg: UserMutedInVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleVoiceConfRecordingStartedMessage(msg: VoiceConfRecordingStartedMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }

  }

  private def handleUserTalkingInVoiceConfMessage(msg: UserTalkingInVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleActiveTalkerChanged(msg: ActiveTalkerChanged) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleUpdateKurentoToken(msg: UpdateKurentoToken) {
    kurentoToken = msg.token
    System.out.println("Kurento token updated successfully, new token: " + msg.token)
    meetings.values foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleVoiceDialing(msg: VoiceDialing) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleVoiceHangingUp(msg: VoiceHangingUp) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleValidateAuthToken(msg: ValidateAuthToken) {
    meetings.get(msg.meetingID) foreach { m =>
      m.actorRef ! msg

      //      val future = m.actorRef.ask(msg)(5 seconds)
      //      future onComplete {
      //        case Success(result) => {
      //          log.info("Validate auth token response. meetingId=" + msg.meetingID + " userId=" + msg.userId + " token=" + msg.token)
      //          /**
      //           * Received a reply from MeetingActor which means hasn't hung!
      //           * Sometimes, the actor seems to hang and doesn't anymore accept messages. This is a simple
      //           * audit to check whether the actor is still alive. (ralam feb 25, 2015)
      //           */
      //        }
      //        case Failure(failure) => {
      //          log.warning("Validate auth token timeout. meetingId=" + msg.meetingID + " userId=" + msg.userId + " token=" + msg.token)
      //          outGW.send(new ValidateAuthTokenTimedOut(msg.meetingID, msg.userId, msg.token, false, msg.correlationId))
      //        }
      //      }
    }
  }

  private def handleKeepAliveMessage(msg: KeepAliveMessage): Unit = {
    outGW.send(new KeepAliveMessageReply(msg.aliveID))
  }

  private def handlePubSubPingMessage(msg: PubSubPing): Unit = {
    outGW.send(new PubSubPong(msg.system, msg.timestamp))
  }

  private def handleDestroyMeeting(msg: DestroyMeeting) {
    log.info("Received DestroyMeeting message for meetingId={}", msg.meetingID)
    meetings.get(msg.meetingID) match {
      case None => log.info("Could not find meetingId={}", msg.meetingID)
      case Some(m) => {
        meetings -= msg.meetingID
        log.info("Kick everyone out on meetingId={}", msg.meetingID)
        if (m.mProps.isBreakout) {
          log.info("Informing parent meeting {} that a breakout room has been ended {}", m.mProps.parentMeetingID, m.mProps.meetingID)
          eventBus.publish(BigBlueButtonEvent(m.mProps.parentMeetingID,
            BreakoutRoomEnded(m.mProps.parentMeetingID, m.mProps.meetingID)))
        }
        // Eject all users using the client.
        outGW.send(new EndAndKickAll(msg.meetingID, m.mProps.recorded))
        // Eject all users from the voice conference
        outGW.send(new EjectAllVoiceUsers(msg.meetingID, m.mProps.recorded, m.mProps.voiceBridge))

        // Delay sending DisconnectAllUsers because of RTMPT connection being dropped before UserEject message arrives to the client  
        context.system.scheduler.scheduleOnce(Duration.create(2500, TimeUnit.MILLISECONDS)) {
          // Disconnect all clients
          outGW.send(new DisconnectAllUsers(msg.meetingID))
          outGW.send(new StopMeetingTranscoders(msg.meetingID))
          log.info("Destroyed meetingId={}", msg.meetingID)
          outGW.send(new MeetingDestroyed(msg.meetingID))

          /** Unsubscribe to meeting and voice events. **/
          eventBus.unsubscribe(m.actorRef, m.mProps.meetingID)
          eventBus.unsubscribe(m.actorRef, m.mProps.voiceBridge)

          // Stop the meeting actor.
          context.stop(m.actorRef)
        }
      }
    }
  }

  private def handleCreateMeeting(msg: CreateMeeting): Unit = {
    meetings.get(msg.meetingID) match {
      case None => {
        log.info("Create meeting request. meetingId={}", msg.mProps.meetingID)

        var m = RunningMeeting(msg.mProps, outGW, eventBus)

        /** Subscribe to meeting and voice events. **/
        eventBus.subscribe(m.actorRef, m.mProps.meetingID)
        eventBus.subscribe(m.actorRef, m.mProps.voiceBridge)
        eventBus.subscribe(m.actorRef, m.mProps.deskshareBridge)

        meetings += m.mProps.meetingID -> m
        outGW.send(new MeetingCreated(m.mProps.meetingID, m.mProps.externalMeetingID, m.mProps.parentMeetingID,
          m.mProps.recorded, m.mProps.meetingName, m.mProps.voiceBridge, msg.mProps.duration, msg.mProps.moderatorPass,
          msg.mProps.viewerPass, msg.mProps.createTime, msg.mProps.createDate, msg.mProps.isBreakout))

        m.actorRef ! new InitializeMeeting(m.mProps.meetingID, m.mProps.recorded, kurentoToken)
      }
      case Some(m) => {
        log.info("Meeting already created. meetingID={}", msg.mProps.meetingID)
        // do nothing
      }
    }
  }

  private def handleGetAllMeetingsRequest(msg: GetAllMeetingsRequest) {
    val len = meetings.keys.size
    var currPosition = len - 1
    var resultArray: Array[MeetingInfo] = new Array[MeetingInfo](len)

    meetings.values.foreach(m => {
      val id = m.mProps.meetingID
      val duration = m.mProps.duration
      val name = m.mProps.meetingName
      val recorded = m.mProps.recorded
      val voiceBridge = m.mProps.voiceBridge

      val info = new MeetingInfo(id, name, recorded, voiceBridge, duration)
      resultArray(currPosition) = info
      currPosition = currPosition - 1

      val html5clientRequesterID = "nodeJSapp"

      //send the users
      eventBus.publish(BigBlueButtonEvent(id, new GetUsers(id, html5clientRequesterID)))

      //send the presentation
      eventBus.publish(BigBlueButtonEvent(id, new GetPresentationInfo(id, html5clientRequesterID, html5clientRequesterID)))

      //send chat history
      eventBus.publish(BigBlueButtonEvent(id, new GetChatHistoryRequest(id, html5clientRequesterID, html5clientRequesterID)))

      //send lock settings
      eventBus.publish(BigBlueButtonEvent(id, new GetLockSettings(id, html5clientRequesterID)))

      //send desktop sharing info
      eventBus.publish(BigBlueButtonEvent(id, new DeskShareGetDeskShareInfoRequest(id, html5clientRequesterID, html5clientRequesterID)))

      // send captions
      eventBus.publish(BigBlueButtonEvent(id, new SendCaptionHistoryRequest(id, html5clientRequesterID)))
    })

    outGW.send(new GetAllMeetingsReply(resultArray))
  }

}


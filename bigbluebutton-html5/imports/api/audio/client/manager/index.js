import BaseAudioBridge from '../bridge/base';
import VertoBridge from '../bridge/verto';
import SIPBridge from '../bridge/sip';

class CallStates {
  static get init() {
    return "initialized state";
  }
  static get echo() {
    return "do echo test state";
  }
  static get callIntoEcho() {
    return "calling into echo test state";
  }
  static get inEchoTest() {
    return "in echo test state";
  }
  static get joinVoiceConference() {
    return "join voice conference state";
  }
  static get callIntoConference() {
    return "calling into conference state";
  }
  static get inConference() {
    return "in conference state";
  }
  static get transferToConference() {
    return "joining from echo into conference state";
  }
  static get echoTestFailed() {
    return "echo test failed state";
  }
  static get callToListenOnly() {
    return "call to listen only state";
  }
  static get connectToListenOnly() {
    return "connecting to listen only state";
  }
  static get inListenOnly() {
    return "in listen only state";
  }
  static get reconnecting() {
    return "reconecting";
  }
};

// manages audio calls and audio bridges
class AudioManager {
  constructor() {
  }

  init(userData) {
    // this check ensures changing locales will not rerun init
    if (this.currentState != undefined) {
      return;
    }
    const MEDIA_CONFIG = Meteor.settings.public.media;
    const audioBridge = MEDIA_CONFIG.useSIPAudio
      ? new SIPBridge(userData)
      : new VertoBridge(userData);

    if (!(audioBridge instanceof BaseAudioBridge)) {
      throw 'Audio Bridge not compatible';
    }

    this.bridge = audioBridge;
    this.isListenOnly = false;
    this.microphoneLockEnforced = userData.microphoneLockEnforced;
    this.callStates = CallStates;
    this.currentState = this.callStates.init;

    callbackToAudioBridge = function (audio) {
      switch (audio.status) {
        case 'failed':
          let audioFailed = new CustomEvent('bbb.webrtc.failed', {
            status: 'Failed', });
          window.dispatchEvent(audioFailed);
          break;
        case 'mediafail':
          let mediaFailed = new CustomEvent('bbb.webrtc.mediaFailed', {
            status: 'MediaFailed', });
          window.dispatchEvent(mediaFailed);
          break;
        case 'mediasuccess':
        case 'started':
          let connected = new CustomEvent('bbb.webrtc.connected', {
            status: 'started', });
          window.dispatchEvent(connected);
          break;
      }
    };
  }

  getCurrentState() {
    return this.currentState;
  }

  exitAudio() {
    this.bridge.exitAudio(this.isListenOnly);
    this.currentState = this.callStates.init;
  }

  joinAudio(listenOnly) {
    if (listenOnly || this.microphoneLockEnforced) {
      this.isListenOnly = true;
      this.bridge.joinListenOnly(callbackToAudioBridge);
      // TODO: remove line below after echo test implemented
      this.currentState = this.callStates.inListenOnly;
    } else {
      this.isListenOnly = false;
      this.bridge.joinMicrophone(callbackToAudioBridge);
      // TODO: remove line below after echo test implemented
      this.currentState = this.callStates.inConference;
    }
  }

  transferToConference() {
    // TODO: transfer from initialized state
    // TODO: transfer from echo test to conference
    // this.bridge.transferToConference();
  }

  webRTCCallStarted(inEchoTest) {
    if (this.isListenOnly) {
      this.currentState = this.callStates.inListenOnly;
    }
    this.currentState = this.callStates.inConference;
  }

  webRTCCallFailed(inEchoTest, errorcode, cause) {
    if (this.currentState !== this.CallStates.reconecting) {
      this.currentState = this.CallStates.reconecting;
    }
  }

  getMicId() {
    // Placeholder, will get the microphone ID for switching input device
    // this.bridge.getMicId();
  }

  setMicId() {
    // Placeholder, will set the microphone ID for switching input device
    // this.bridge.setMicId();
  }

  getSpeakerId() {
    // Placeholder, will get the speaker ID for switching output device
    // this.bridge.getSpeakerId();
  }

  setSpeakerId() {
    // Placeholder, will set the speaker ID for switching output device
    // this.bridge.setSpeakerId();
  }

  getActiveMic() {
    // Placeholder, will detect active input hardware
    // this.bridge.getActiveMic();
  }

}

const AudioManagerSingleton = new AudioManager();
export default AudioManagerSingleton;

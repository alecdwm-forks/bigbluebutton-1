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
package org.bigbluebutton.webconference.voice;
import java.util.Map;

public interface ConferenceServiceProvider {
	public void populateRoom(String room);
	public void mute(String room, String participant, Boolean mute);	
	public void eject(String room, String participant);
	public void ejectAll(String room);
	public void record(String room, String meetingid);
	public void broadcast(String room, String meetingid);
	public void dial(String room, String participant, Map<String, String> options, Map<String, String> params);
	public void cancelDial(String room, String uuid);
	public void sendDtmf(String room, String uuid, String dtmfDigit);
}

/*
 * Copyright @ 2017 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.meet.test;


import org.jitsi.meet.test.base.JitsiMeetUrl;
import org.jitsi.meet.test.pageobjects.web.AVModerationMenu;
import org.jitsi.meet.test.pageobjects.web.ParticipantsPane;
import org.jitsi.meet.test.pageobjects.web.UnmuteModalDialogHelper;
import org.jitsi.meet.test.util.MeetUIUtils;
import org.jitsi.meet.test.util.TestUtils;
import org.jitsi.meet.test.web.WebParticipant;
import org.jitsi.meet.test.web.WebTestBase;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests the A-V moderation functionality.
 *
 * @author Calin Chitu
 */
public class AudioVideoModerationTest extends WebTestBase
{
    /**
     * The participant.
     */
    private WebParticipant participant1;
    private WebParticipant participant2;
    private WebParticipant participant3;

    @Override
    public void setupClass()
    {
        super.setupClass();

        ensureThreeParticipants();
        participant1 = getParticipant1();
        participant2 = getParticipant2();
        participant3 = getParticipant3();
    }

    /**
     * Checks moderation by enabling and disabling it
     */
    @Test
    public void testCheckModerationEnableDisable()
    {
        ParticipantsPane participantsPane = participant1.getParticipantsPane();
        AVModerationMenu avModerationMenu = participant1.getAVModerationMenu();

        TestUtils.waitMillis(2000);

        assertTrue(participant1.isModerator(), "Participant 1 must be moderator");
        assertFalse(participant2.isModerator(), "Participant 2 must not be moderator");
        assertFalse(participant3.isModerator(), "Participant 3 must not be moderator");

        participantsPane.open();

        participantsPane.clickContextMenuButton();

        avModerationMenu.clickStartModeration();

        checkAudioVideoParticipantMute(participant3);

        avModerationMenu.clickStopModeration();

        checkAudioVideoParticipantUnmute(participant3);

        participantsPane.close();
    }

    /**
     * Opens the context menu from the participants pane
     * and enables moderation
     */
    @Test
    public void testEnableModerationForParticipant()
    {
        ParticipantsPane participantsPane = participant1.getParticipantsPane();
        AVModerationMenu avModerationMenu = participant1.getAVModerationMenu();
        WebDriver driver1 = participant1.getDriver();

        TestUtils.waitMillis(2000);

        assertTrue(participant1.isModerator(), "Participant 1 must be moderator");
        assertFalse(participant2.isModerator(), "Participant 2 must not be moderator");
        assertFalse(participant3.isModerator(), "Participant 3 must not be moderator");

        participantsPane.open();

        participantsPane.clickContextMenuButton();

        avModerationMenu.clickStartModeration();

        startModerationForParticipant(participant3);

        raiseHandToSpeak(participant3);

        askParticipantToUnmute(participant3);

        checkAudioVideoParticipantUnmute(participant3);

        participantsPane.close();

        raiseHandToSpeak(participant2);

        TestUtils.waitForElementDisplayToBe(
                driver1,
                By.id("notification-participant-list"),
                3,
                true);

        UnmuteModalDialogHelper.clickUnmuteButton(driver1);

        TestUtils.waitMillis(2000);

        assertTrue(
                participant2.getNotifications().hasAskToUnmuteNotification(),
                "The participant should see a notification that the moderator requests to unmute.");

        checkAudioVideoParticipantUnmute(participant2);

        participantsPane.open();

        participantsPane.clickContextMenuButton();

        avModerationMenu.clickStopModeration();

        TestUtils.waitMillis(2000);

        assertTrue(
                participant2.getNotifications().hasModerationStopNotification(),
                "The participant should see a notification that moderation stopped.");
    }


    /**
     * Initial moderator reloads and next participant becomes the new moderator
     */
    public void testHangUpAndChangeModerator()
    {
        ParticipantsPane participantsPane1 = participant1.getParticipantsPane();
        AVModerationMenu avModerationMenu1 = participant1.getAVModerationMenu();

        participant2.hangUp();
        participant3.hangUp();

        joinSecondParticipant();
        joinThirdParticipant();

        participant2.muteAudio(true);

        participant3.muteAudio(true);

        participantsPane1.open();

        participantsPane1.clickContextMenuButton();

        avModerationMenu1.clickStartModeration();

        participant2.getToolbar().clickRaiseHandButton();

        participant3.getToolbar().clickRaiseHandButton();

        participantsPane1.askToUnmute(participant1);

        participant1.hangUp();

        joinFirstParticipant();
//
//        MeetUIUtils.toggleAudioAndCheck(participant3, participant2, false, false);
//
//        MeetUIUtils.unmuteVideoAndCheck(participant3, participant2);
//
//        participantsPane.clickContextMenuButton();
//
//        avModerationMenu.clickStopModeration();
//
//        TestUtils.waitMillis(2000);
//
//        assertTrue(
//                participant3.getNotifications().hasModerationStopNotification(),
//                "The participant should see a notification that moderation stopped.");
    }

    /**
     * Participant raises hand to speak during moderation
     * @param participant the participant that wants to speak
     */
    private void raiseHandToSpeak(WebParticipant participant)
    {
        participant.getToolbar().clickAudioMuteButton();

        TestUtils.waitMillis(2000);

        assertTrue(
                participant.getNotifications().hasAudioModerationNotification(),
                "The participant should see a notification that has to raise his hand.");

        participant.getToolbar().clickRaiseHandButton();

        TestUtils.waitMillis(2000);

        assertTrue(
                participant1.getNotifications().hasRaisedHandNotification(),
                "The moderator should see a notification that a participant wants to unmute.");
    }

    /**
     * Moderator asks participant to unmute
     * @param participant the participant who is requested to unmute
     */
    private void askParticipantToUnmute(WebParticipant participant)
    {
        ParticipantsPane participantsPane = participant1.getParticipantsPane();

        participantsPane.askToUnmute(participant1);

        TestUtils.waitMillis(2000);

        assertTrue(
                participant.getNotifications().hasAskToUnmuteNotification(),
                "The participant should see a notification that the moderator requests to unmute.");
    }

    /**
     * Checks audio/video mute state for participant
     * @param participant the participant that is checked
     */
    private void checkAudioVideoParticipantMute(WebParticipant participant)
    {
        TestUtils.waitMillis(2000);

        MeetUIUtils.toggleAudioAndCheck(participant, participant1, true, false);

        TestUtils.waitMillis(2000);

        MeetUIUtils.muteVideoAndCheck(participant, participant1);
    }

    /**
     * Checks audio/video unmute state for participant
     * @param participant the participant that is checked
     */
    private void checkAudioVideoParticipantUnmute(WebParticipant participant)
    {
        TestUtils.waitMillis(2000);

        MeetUIUtils.toggleAudioAndCheck(participant, participant1, false, false);

        TestUtils.waitMillis(2000);

        MeetUIUtils.unmuteVideoAndCheck(participant, participant1);
    }

    /**
     * Starts moderation for participant
     * @param participant the moderated participant
     */
    private void startModerationForParticipant(WebParticipant participant)
    {
        TestUtils.waitMillis(2000);

        assertTrue(
                participant.getNotifications().hasModerationStartNotification(),
                "The participant should see a notification that moderation started.");
    }

    /**
     * Checks audio/video state for participant
     * @param participant the participant that is checked
     */
    private void checkAudioVideoParticipant(WebParticipant participant)
    {
        TestUtils.waitMillis(2000);

        checkAudioVideoParticipantUnmute(participant);

        TestUtils.waitMillis(2000);

        checkAudioVideoParticipantMute(participant);
    }
}

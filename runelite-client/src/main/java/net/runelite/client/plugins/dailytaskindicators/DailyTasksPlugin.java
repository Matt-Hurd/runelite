/*
 * Copyright (c) 2018, Infinitay <https://github.com/Infinitay>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.runelite.client.plugins.dailytaskindicators;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColor;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Daily Task Indicator",
	enabledByDefault = false
)
@Slf4j
public class DailyTasksPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DailyTasksConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	private boolean hasSentHerbMsg, hasSentStavesMsg, hasSentEssenceMsg;

	@Provides
	DailyTasksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DailyTasksConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		hasSentHerbMsg = hasSentStavesMsg = hasSentEssenceMsg = false;
		cacheColors();
	}

	@Override
	protected void shutDown() throws Exception
	{
		hasSentHerbMsg = hasSentStavesMsg = hasSentEssenceMsg = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("dailytaskindicators"))
		{
			if (event.getKey().equals("showHerbBoxes"))
			{
				hasSentHerbMsg = false;
			}
			else if (event.getKey().equals("showStaves"))
			{
				hasSentStavesMsg = false;
			}
			else if (event.getKey().equals("showEssence"))
			{
				hasSentEssenceMsg = false;
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState().equals(GameState.LOGGED_IN))
		{
			if (config.showHerbBoxes() && !hasSentHerbMsg && checkCanCollectHerbBox())
			{
				sendChatMessage("You have herb boxes waiting to be collected at NMZ.");
				hasSentHerbMsg = true;
			}
			if (config.showStaves() && !hasSentStavesMsg && checkCanCollectStaves())
			{
				sendChatMessage("You have staves waiting to be collected from Zaff.");
				hasSentStavesMsg = true;
			}
			if (config.showEssence() && !hasSentEssenceMsg && checkCanCollectEssence())
			{
				sendChatMessage("You have pure essence waiting to be collected from Wizard Cromperty.");
				hasSentEssenceMsg = true;
			}
		}
	}

	private boolean checkCanCollectHerbBox()
	{
		int value = client.getSetting(Varbits.DAILY_HERB_BOX);
		return value < 15; // < 15 can claim
	}

	private boolean checkCanCollectStaves()
	{
		int value = client.getSetting(Varbits.DAILY_STAVES);
		return value == 0; // 1 = can't claim
	}

	private boolean checkCanCollectEssence()
	{
		int value = client.getSetting(Varbits.DAILY_ESSENCE);
		return value == 0; // 1 = can't claim
	}

	private void cacheColors()
	{
		chatMessageManager.cacheColor(new ChatColor(ChatColorType.HIGHLIGHT, Color.RED, false), ChatMessageType.GAME).refreshAll();
	}

	private void sendChatMessage(String chatMessage)
	{
		final String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(chatMessage)
			.build();

		chatMessageManager.queue(
			QueuedMessage.builder()
				.type(ChatMessageType.GAME)
				.runeLiteFormattedMessage(message)
				.build());
	}
}

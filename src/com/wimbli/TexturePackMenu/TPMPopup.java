package com.wimbli.TexturePackMenu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.player.SpoutPlayer;

// SCREEN SIZE NOTE: scaled screen size seems to always be precisely 427x240... not sure why 427 width, but there you have it
public class TPMPopup extends GenericPopup {
	private static final char DEFAULT_INDICATOR = '*';
	private static final char CURRENT_INDICATOR = '@';
	private TexturePackMenu plugin;
	private SpoutPlayer sPlayer;
	private GenericButton bNext;
	private GenericButton bPrev;
	private List<GenericButton> bChoice = new ArrayList<GenericButton>(10);
	private int page = 0;
	private int maxPage = 0;

	public static void create(final TexturePackMenu plugin, Player player) {
		if (!player.hasPermission("texturepackmenu.texture")) {
			player.sendMessage("You do not have the necessary permission to choose a texture pack.");
			return;
		}

		TPMPopup newPopup = new TPMPopup(plugin, player);
		newPopup.initiate();
	}

	public TPMPopup(final TexturePackMenu mainPlugin, Player player) {
		if (player == null) {
			return;
		}

		sPlayer = SpoutManager.getPlayer(player);
		if (sPlayer == null) {
			return;
		}

		this.plugin = mainPlugin;

		if (Config.texturePackCount() == 0) {
			player.sendMessage("Sorry, but no texture packs are currently configured.");
			return;
		}

		if (!sPlayer.isSpoutCraftEnabled()) {
			player.sendMessage("This only works with the Spoutcraft client. See here:");
			player.sendMessage("              " + ChatColor.BLUE + "http://get.spout.org");
			return;
		}

		maxPage = (int) Math.ceil((double) Config.texPackNames().size() / 10.0) - 1;
	}

	public void initiate() {
		this.initLabels();
		this.initOtherButtons();
		this.initChoiceButtons();
		this.refreshButtons();

		sPlayer.getMainScreen().attachPopupScreen(this); // Show the player the popup
	}

	public void exit() {
		sPlayer.getMainScreen().closePopup();
	}

	private void makeChoice(final int buttonIndex) {
		Config.setPackDelayed(sPlayer, (page * 10) + buttonIndex);
		exit();
	}

	private void refreshButtons() {
		bPrev.setEnabled(page > 0);
		bNext.setEnabled(page < maxPage);
		bPrev.setDirty(true);
		bNext.setDirty(true);

		int loop, offset = page * 10;

		for (loop = 0; loop < 10; loop++) {
			int index = offset + loop;
			if (index > Config.texPackNames().size() - 1) {
				break;
			}

			String text = Config.texPackNames().get(index);
			GenericButton btn = bChoice.get(loop);

			btn.setTextColor(new Color(255, 255, 255, 0));

			if (index == 0) { // default pack
				text = DEFAULT_INDICATOR + " " + text;
				btn.setTextColor(new Color(127, 255, 255, 0));
			}
			if (Config.getPack(sPlayer.getName()).equals(Config.texPackNames().get(index))) { // current pack
				text = CURRENT_INDICATOR + " " + text;
				btn.setTextColor(new Color(191, 255, 191, 0));
			}

			btn.setText(text);
			btn.setVisible(true);
			btn.setDirty(true);
		}
		while (loop < 10) {
			bChoice.get(loop).setVisible(false);
			bChoice.get(loop).setDirty(true);
			loop++;
		}
	}

	private void nextPage() {
		if (page < maxPage) {
			page += 1;
		}
		refreshButtons();
	}

	private void lastPage() {
		if (page > 0) {
			page -= 1;
		}
		refreshButtons();
	}

	private void initLabels() {
		GenericLabel label = new GenericLabel("Choose a texture pack below:");
		label.setWidth(1).setHeight(1); // prevent Spout's questionable "no default size" warning; how a variable
										// width/height text widget benefits from a width and height being set is beyond
										// me
		label.setTextColor(new Color(63, 255, 63, 0));
		label.setScale(2.0f);
		label.setX(64).setY(20);
		this.attachWidget(plugin, label);

		label = new GenericLabel(DEFAULT_INDICATOR + " - Default Pack");
		label.setWidth(1).setHeight(1); // prevent Spout's questionable "no default size" warning
		label.setX(207).setY(191);
		label.setTextColor(new Color(127, 255, 255, 0));
		this.attachWidget(plugin, label);

		label = new GenericLabel(CURRENT_INDICATOR + " - Current Pack");
		label.setWidth(1).setHeight(1); // prevent Spout's questionable "no default size" warning
		label.setX(204).setY(201);
		label.setTextColor(new Color(191, 255, 191, 0));
		this.attachWidget(plugin, label);
	}

	private void initOtherButtons() {
		GenericButton cancel = new GenericButton("Cancel") {
			@Override
			public void onButtonClick(ButtonClickEvent event) {
				exit();
			}
		};
		cancel.setWidth(95).setHeight(20);
		cancel.setX(311).setY(190);
		cancel.setTextColor(new Color(255, 191, 191, 0));
		this.attachWidget(plugin, cancel);

		bPrev = new GenericButton("< Prev Page") {
			@Override
			public void onButtonClick(ButtonClickEvent event) {
				lastPage();
			}
		};
		bPrev.setWidth(80).setHeight(20);
		bPrev.setX(21).setY(190);
		if (maxPage == 0)
			bPrev.setVisible(false);
		this.attachWidget(plugin, bPrev);

		bNext = new GenericButton("Next Page >") {
			@Override
			public void onButtonClick(ButtonClickEvent event) {
				nextPage();
			}
		};
		bNext.setWidth(80).setHeight(20);
		bNext.setX(105).setY(190);
		if (maxPage == 0)
			bNext.setVisible(false);
		this.attachWidget(plugin, bNext);
	}

	private void initChoiceButtons() {
		bChoice = new ArrayList<GenericButton>(10);

		boolean rowToggle = true;
		GenericButton current;
		final int bWidth = 190, bHeight = 20, offsetLeft1 = 21, offsetLeft2 = 216;
		int offsetTop = 50;

		for (int i = 0; i < 10; i++) {
			final int idx = i;

			current = new GenericButton(Integer.toString(i + 1)) {
				int index = idx;

				@Override
				public void onButtonClick(ButtonClickEvent event) {
					makeChoice(index);
				}
			};

			current.setWidth(bWidth).setHeight(bHeight);
			current.setX(rowToggle ? offsetLeft1 : offsetLeft2).setY(offsetTop);

			bChoice.add(current);
			this.attachWidget(plugin, current);

			rowToggle ^= true;
			if (rowToggle) {
				offsetTop += 25;
			}
		}
	}
}

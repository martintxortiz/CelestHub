package net.kryunek.hub.managers.scoreboard;


import lombok.Getter;
import net.kryunek.hub.managers.scoreboard.events.ScoreboardBoardCreatedEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class ScoreboardBoard {

	private final Scoreboard assemble;

	private final List<ScoreboardBoardEntry> entries = new ArrayList<>();
	private final List<String> identifiers = new ArrayList<>();

	private final UUID uuid;

	/**
	 * Scoreboard Board.
	 *
	 * @param player that the board belongs to.
	 * @param assemble instance.
	 */
	public ScoreboardBoard(Player player, Scoreboard assemble) {
		this.uuid = player.getUniqueId();
		this.assemble = assemble;
		this.setup(player);
	}

	/**
	 * Get's a player's bukkit scoreboard.
	 *
	 * @return either existing scoreboard or new scoreboard.
	 */
	public org.bukkit.scoreboard.Scoreboard getScoreboard() {
		Player player = Bukkit.getPlayer(getUuid());
		if (player == null) {
			return Bukkit.getScoreboardManager().getNewScoreboard();
		}
		if (getAssemble().isHook() || player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
			return player.getScoreboard();
		} else {
			return Bukkit.getScoreboardManager().getNewScoreboard();
		}
	}

	/**
	 * Get's the player's scoreboard objective.
	 *
	 * @return either existing objecting or new objective.
	 */
	public Objective getObjective() {
		org.bukkit.scoreboard.Scoreboard scoreboard = getScoreboard();
		if (scoreboard.getObjective("Scoreboard") == null) {
			Objective objective = scoreboard.registerNewObjective("Scoreboard", "dummy");
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			objective.setDisplayName(getAssemble().getAdapter().getTitle(Bukkit.getPlayer(getUuid())));
			return objective;
		} else {
			return scoreboard.getObjective("Scoreboard");
		}
	}

	/**
	 * Setup the board.
	 *
	 * @param player who's board to setup.
	 */
	private void setup(Player player) {
		org.bukkit.scoreboard.Scoreboard scoreboard = getScoreboard();
		player.setScoreboard(scoreboard);
		getObjective();

		// Call Events if enabled.
		if (assemble.isCallEvents()) {
			ScoreboardBoardCreatedEvent createdEvent = new ScoreboardBoardCreatedEvent(this);
			Bukkit.getPluginManager().callEvent(createdEvent);
		}
	}

	/**
	 * Get the board entry at a specific position.
	 *
	 * @param pos to find entry.
	 * @return entry if it isn't out of range.
	 */
	public ScoreboardBoardEntry getEntryAtPosition(int pos) {
		return pos >= this.entries.size() ? null : this.entries.get(pos);
	}

	/**
	 * Get the unique identifier for position in scoreboard.
	 *
	 * @param position for identifier.
	 * @return unique identifier.
	 */
	public String getUniqueIdentifier(int position) {
		String identifier = getRandomChatColor(position) + ChatColor.WHITE;

		while (this.identifiers.contains(identifier)) {
			identifier = identifier + getRandomChatColor(position) + ChatColor.WHITE;
		}

		// This is rare, but just in case, make the method recursive
		if (identifier.length() > 16) {
			return this.getUniqueIdentifier(position);
		}

		// Add our identifier to the list so there are no duplicates
		this.identifiers.add(identifier);

		return identifier;
	}

	/**
	 * Gets a ChatColor based off the position in the collection.
	 *
	 * @param position of entry.
	 * @return ChatColor adjacent to position.
	 */
	private String getRandomChatColor(int position) {
		return assemble.getChatColorCache()[position].toString();
	}

}

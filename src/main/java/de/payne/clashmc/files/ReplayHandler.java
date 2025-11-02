package de.payne.clashmc.files;

import org.bukkit.plugin.Plugin;

public class ReplayHandler extends FileManager {

    public ReplayHandler(Plugin plugin) {
        super(plugin, FILENAME.REPLAY);
    }

    /**
     * Beispiel: Replay-Daten als YAML speichern
     * Du kannst komplexere Strukturen anlegen (z.B. Spielstände, Events)
     */
    public void saveReplayData(String replayName, Object data) {
        getConfig().set("replays." + replayName.toLowerCase(), data);
        save();
    }

    public Object loadReplayData(String replayName) {
        return getConfig().get("replays." + replayName.toLowerCase());
    }

    public boolean replayExists(String replayName) {
        return getConfig().contains("replays." + replayName.toLowerCase());
    }

    public void deleteReplay(String replayName) {
        getConfig().set("replays." + replayName.toLowerCase(), null);
        save();
    }
}

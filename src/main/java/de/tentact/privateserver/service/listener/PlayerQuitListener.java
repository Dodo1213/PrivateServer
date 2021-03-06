package de.tentact.privateserver.service.listener;
/*  Created in the IntelliJ IDEA.
    Copyright(c) 2020
    Created by 0utplay | Aldin Sijamhodzic
    Datum: 06.08.2020
    Uhrzeit: 11:27
*/

import de.tentact.languageapi.LanguageAPI;
import de.tentact.privateserver.PrivateServer;
import de.tentact.privateserver.provider.config.NPCServerItemProperty;
import de.tentact.privateserver.provider.i18n.I18N;
import de.tentact.privateserver.service.CurrentPrivateServiceUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final CurrentPrivateServiceUtil currentPrivateServiceUtil;
    private final LanguageAPI languageAPI = LanguageAPI.getInstance();
    private final PrivateServer privateServer;

    public PlayerQuitListener(PrivateServer privateServer) {
        this.privateServer = privateServer;
        this.currentPrivateServiceUtil = privateServer.getCurrentPrivateServiceUtil();
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        NPCServerItemProperty serverItemProperty = this.currentPrivateServiceUtil.getCurrentServerItemProperty();
        if (!this.privateServer.getCurrentPrivateServiceUtil().isPrivateServer()) {
            return;
        }
        if (serverItemProperty == null) {
            return;
        }
        if (!serverItemProperty.isAutoStopOnOwnerLeave()) {
            return;
        }
        if (!player.getUniqueId().equals(this.currentPrivateServiceUtil.getOwner())) {
            return;
        }
        languageAPI.getPlayerExecutor().broadcastMessage(I18N.OWNER_LEFT_STOPPING_SERVER.replace("%OWNER%", player.getName()));

        Bukkit.getScheduler().runTaskLater(privateServer, () -> {
            languageAPI.getPlayerExecutor().kickAll(I18N.OWNER_LEFT_KICK_MESSAGE.replace("%OWNER%", player.getName()));
            Bukkit.shutdown();
        }, 20 * 15L);

    }
}

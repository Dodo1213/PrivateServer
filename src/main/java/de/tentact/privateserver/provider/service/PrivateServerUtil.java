package de.tentact.privateserver.provider.service;
/*  Created in the IntelliJ IDEA.
    Copyright(c) 2020
    Created by 0utplay | Aldin Sijamhodzic
    Datum: 05.08.2020
    Uhrzeit: 12:51
*/

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.tentact.languageapi.LanguageAPI;
import de.tentact.languageapi.player.LanguagePlayer;
import de.tentact.privateserver.PrivateServer;
import de.tentact.privateserver.i18n.I18N;
import de.tentact.privateserver.provider.config.NPCServerItemProperty;
import de.tentact.privateserver.provider.config.PrivateServerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class PrivateServerUtil {

    private final PrivateServer privateServer;
    private final PrivateServerConfig privateServerConfig;
    private final CloudNetDriver cloudNetDriver = CloudNetDriver.getInstance();
    private final IPlayerManager iPlayerManager;

    public PrivateServerUtil(PrivateServer privateServer) {
        this.privateServer = privateServer;
        this.privateServerConfig = this.privateServer.getConfiguration().getPrivateServerConfig();
        this.iPlayerManager = this.cloudNetDriver.getServicesRegistry().getFirstService(IPlayerManager.class);
    }

    public boolean startPrivateServer(UUID serverOwner, ServiceTemplate serviceTemplate, NPCServerItemProperty serverItemProperty) {
        if (this.privateServer.getConfiguration().getPrivateServerConfig().getPrivateServerTask() == null) {
            return false;
        }
        ServiceTask serviceTask = this.privateServerConfig.getPrivateServerTask();

        if (serviceTask == null) {
            return false;
        }
        JsonDocument document = new JsonDocument("serverowner", serverOwner)
                .append("templatePrefix", serviceTemplate.getPrefix())
                .append("templateName", serviceTemplate.getName())
                .append("autoStopOnOwnerLeave", serverItemProperty.isAutoStopOnOwnerLeave())
                .append("autoStopOnEmpty", serverItemProperty.isAutoStopIfEmpty());

        ServiceInfoSnapshot serviceInfoSnapshot = ServiceConfiguration
                .builder(serviceTask)
                .addTemplates(serviceTemplate)
                .properties(document)
                .build()
                .createNewService();
        if (serviceInfoSnapshot == null) {
            return false;
        }
        serviceInfoSnapshot.provider().startAsync();
        return true;
    }

    public void createPrivateServer(Player player, String template) {
        LanguagePlayer languagePlayer = LanguageAPI.getInstance().getPlayerExecutor().getLanguagePlayer(player.getUniqueId());
        if (languagePlayer == null) {
            return;
        }
        if (this.hasPrivateServer(player.getUniqueId())) {
            languagePlayer.sendMessage(I18N.PLAYER_ALREADY_HAS_PSERVER);
            return;
        }

        if (!template.matches("([A-Za-z0-9]+\\/[A-Za-z0-9]+)")) {
            languagePlayer.sendMessage(I18N.WRONG_TEMPLATE_FORMAT);
            return;
        }

        String templatePrefix = template.split("/")[0];
        String templateName = template.split("/")[1];
        NPCServerItemProperty serverItem = this.privateServerConfig.getStartItems()
                .stream()
                .filter(npcServerItemProperty -> npcServerItemProperty.getTemplateToStart().equalsIgnoreCase(template)).findFirst().orElse(null);

        if (serverItem == null) {
            languagePlayer.sendMessage(I18N.TEMPLATE_NOT_FOUND
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            return;
        }

        if (!player.hasPermission(serverItem.getStartPermission())) {
            languagePlayer.sendMessage(I18N.NO_TEMPLATE_START_PERMISSION
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            return;
        }

        ServiceTemplate serviceTemplate = this.cloudNetDriver.getLocalTemplateStorageTemplates()
                .stream()
                .filter(sTemplate -> sTemplate.getPrefix().equalsIgnoreCase(templatePrefix) && sTemplate.getName().equalsIgnoreCase(templateName)).findFirst().orElse(null);

        if (serviceTemplate == null) {
            languagePlayer.sendMessage(I18N.TEMPLATE_NOT_FOUND
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            return;
        }

        boolean started = this.privateServer.getPrivateServerUtil().startPrivateServer(languagePlayer.getUniqueId(), serviceTemplate, serverItem);
        if (started) {
            languagePlayer.sendMessage(I18N.STARTING_PSERVER
                    .replace("%TEMPLATE%", template)
                    .replace("%TEMPLATE_NAME%", templateName)
                    .replace("%TEMPLATE_PREFIX%", templatePrefix));
            Bukkit.getScheduler().runTaskLater(this.privateServer, () -> languagePlayer.sendMessage(I18N.TELEPORT_AFTER_START), 20*2);
            return;
        }
        languagePlayer.sendMessage(I18N.STARTING_PSERVER_ERROR);
    }

    public boolean hasPrivateServer(UUID serverOwner) {
        for (ServiceInfoSnapshot cloudService : this.cloudNetDriver.getCloudServiceProvider().getCloudServices(this.privateServerConfig.getPrivateServerTaskName())) {
            if (cloudService.getProperties().contains("serverowner") && cloudService.getProperties().get("serverowner", UUID.class).equals(serverOwner)) {
                return true;
            }
        }
        return false;
    }

    public void sendOwner(UUID serverOwner) {
        if (!this.hasPrivateServer(serverOwner)) {
            return;
        }
        this.getServiceInfoSnapshot(serverOwner).ifPresent(serviceInfoSnapshot ->
                this.iPlayerManager.getPlayerExecutor(serverOwner).connect(serviceInfoSnapshot.getName()));
    }

    public Optional<ServiceInfoSnapshot> getServiceInfoSnapshot(UUID serverOwner) {
        for (ServiceInfoSnapshot cloudService : this.cloudNetDriver.getCloudServiceProvider().getCloudServices(this.privateServerConfig.getPrivateServerTaskName())) {
            if (cloudService.getProperties().contains("serverowner") && cloudService.getProperties().get("serverowner", UUID.class).equals(serverOwner)) {
                return Optional.of(cloudService);
            }
        }
        return Optional.empty();
    }

    public <T> T getProperty(ServiceInfoSnapshot serviceInfoSnapshot, String name, Class<T> clazz) {
        return serviceInfoSnapshot.getProperties().get(name, clazz);
    }

    public String getPropertyAsString(ServiceInfoSnapshot serviceInfoSnapshot, String property) {
        return this.getProperty(serviceInfoSnapshot, property, String.class);
    }

}

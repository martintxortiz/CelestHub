package net.kryunek.hub.managers.module.impl;

import net.kryunek.hub.support.config.ConfigFiles;

import net.kryunek.hub.Celest;
import net.kryunek.hub.listeners.*;
import net.kryunek.hub.listeners.hotbar.GadgetsItemListener;
import net.kryunek.hub.listeners.hotbar.HideShowItemListener;
import net.kryunek.hub.listeners.hotbar.HotbarCommandListener;
import net.kryunek.hub.listeners.hotbar.ServerItemListener;
import net.kryunek.hub.listeners.hotbar.SettingsItemListener;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.module.Module;
import net.kryunek.hub.support.message.Messages;
import net.kryunek.hub.utils.menu.ButtonListener;

public class ListenerModule extends Module {
    @Override
    public void onEnable(Celest hub) {
        new ButtonListener(hub);
        new JoinLeaveListener(hub);
        new ChatListener(hub);
        new EditorListener(hub);
        new ProfileListener(hub,
                ModuleService.getManagerModule().getProfileManager(),
                Messages.from(ModuleService.getFileModule().getFile(ConfigFiles.MESSAGES), hub.getLogger()));
        new ServerItemListener(hub);
        new HotbarCommandListener(hub);
        new GadgetsItemListener(hub);
        new SettingsItemListener(hub);
        new HideShowItemListener(hub);
        new WorldListeners(hub);
        new PvpArenaListener(hub);
        new PvpArenaEditorListener(hub);
        new BuildModeListener(hub);
        new OutfitListener(hub);
        new QueueListener(hub);
        new LotteryListener(hub);
        new TimerListener(hub);
        new TrailParticleListener(hub);
        new DoubleJumpListener(hub);
        new ServerSelectorEditorListener(hub);
        new GadgetEditorListener(hub);
        new RankEditorListener(hub);



    }
    
    @Override
    public int getPriority() {
        return 4;
    }
}

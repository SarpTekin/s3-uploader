package com.sarptekin.awsuploader.ui;

import java.awt.GraphicsEnvironment;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UiLauncher {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (Boolean.getBoolean("headless") || GraphicsEnvironment.isHeadless()) {
            return; // allow disabling UI via -Dheadless=true
        }
        UploaderUI.launch();
    }
}



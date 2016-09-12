package com.kms.katalon.composer.webui.recorder.core;

import java.io.File;
import java.io.IOException;

import org.eclipse.e4.core.services.log.Logger;
import org.openqa.selenium.firefox.FirefoxProfile;

import com.kms.katalon.core.webui.driver.WebUIDriverType;
import com.kms.katalon.core.webui.util.WebDriverPropertyUtil;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.objectspy.core.HTMLElementCaptureServer;
import com.kms.katalon.objectspy.core.InspectSession;

@SuppressWarnings("restriction")
public class RecordSession extends InspectSession {
	public static final String RECORDER_ADDON_NAME = "Recorder";
	
    private static final String CHROME_EXTENSION_RELATIVE_PATH = File.separator + "Chrome" + File.separator
			+ RECORDER_ADDON_NAME;
	private static final String FIREFOX_ADDON_RELATIVE_PATH = File.separator + "Firefox" + File.separator
			+ "recorder.xpi";
	
	private static final String RECORDER_APPLICATION_DATA_FOLDER = System.getProperty("user.home") + File.separator + "AppData" + File.separator
			+ "Local" + File.separator + "KMS" + File.separator + "qAutomate" + File.separator + RECORDER_ADDON_NAME;
	
    private static final String RECORDER_FIREFOX_SERVER_PORT_PREFERENCE_KEY = "extensions.@recorder.katalonServerPort";

    private static final String RECORDER_FIREFOX_ON_OFF_PREFERENCE_KEY = "extensions.@recorder.katalonOnOffStatus";

	public RecordSession(HTMLElementCaptureServer server, WebUIDriverType webUiDriverType, ProjectEntity currentProject, Logger logger) throws Exception {
		super(server, webUiDriverType, currentProject, logger);
	}
	
	protected String getChromeExtensionPath() {
		return CHROME_EXTENSION_RELATIVE_PATH;
	}

	protected String getFirefoxExtensionPath() {
		return FIREFOX_ADDON_RELATIVE_PATH;
	}

	@Override
	protected String getAddOnName() {
		return RECORDER_ADDON_NAME;
	}
	
	@Override
	protected String getIEApplicationDataFolder() {
		return RECORDER_APPLICATION_DATA_FOLDER;
	}
	
	protected FirefoxProfile createFireFoxProfile() throws IOException {
        FirefoxProfile firefoxProfile = WebDriverPropertyUtil.createDefaultFirefoxProfile();
        firefoxProfile.setPreference(RECORDER_FIREFOX_SERVER_PORT_PREFERENCE_KEY, String.valueOf(server.getServerPort()));
        firefoxProfile.setPreference(RECORDER_FIREFOX_ON_OFF_PREFERENCE_KEY, true);
        File file = getFirefoxAddonFile();
        if (file != null) {
            firefoxProfile.addExtension(file);
        }
        return firefoxProfile;
    }
}

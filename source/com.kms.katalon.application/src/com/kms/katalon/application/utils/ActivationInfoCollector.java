package com.kms.katalon.application.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;

import com.google.gson.JsonObject;
import com.kms.katalon.application.KatalonApplication;
import com.kms.katalon.application.KatalonApplicationActivator;
import com.kms.katalon.application.constants.ApplicationMessageConstants;
import com.kms.katalon.application.constants.ApplicationStringConstants;
import com.kms.katalon.constants.UsagePropertyConstant;
import com.kms.katalon.feature.FeatureServiceConsumer;
import com.kms.katalon.feature.IFeatureService;
import com.kms.katalon.license.LicenseService;
import com.kms.katalon.license.models.Feature;
import com.kms.katalon.license.models.License;
import com.kms.katalon.core.util.internal.JsonUtil;
import com.kms.katalon.feature.FeatureServiceConsumer;
import com.kms.katalon.feature.IFeatureService;
import com.kms.katalon.logging.LogUtil;
import com.kms.katalon.util.CryptoUtil;

public class ActivationInfoCollector {

    public static final String DEFAULT_HOST_NAME = "can.not.get.host.name";

    private static boolean activated = false;

    protected ActivationInfoCollector() {
    }

    public static void setActivated(boolean activated) {
        ActivationInfoCollector.activated = activated;
    }

    public static boolean isActivated() {
        return activated;
    }

    public static boolean checkAndMarkActivated() {
        activated = checkActivated();
        if (activated) {
            activateTestOpsFeatures();
        }
        return activated;
    }

    public static boolean checkActivated() {
        try {
            String offlineActivationFlag = ApplicationInfo.getAppProperty(
                    ApplicationStringConstants.ARG_OFFLINE_ACTIVATION);
            if (offlineActivationFlag != null) {
                String activationCode = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_ACTIVATION_CODE);
                if (activationCode == null) {
                    return false;
                } else {
                    License license = LicenseService.getInstance().parseJws(activationCode);
                    boolean isValidLicense = isValidLicense(license);
                    if (isValidLicense) {
                        enableFeatures(license);
                    }
                    return isValidLicense;
                }
            } else {
                return isActivatedByAccount();
            }
        } catch (Exception ex) {
            LogUtil.logError(ex);
            return false;
        }
    }

    private static boolean isActivatedByAccount() {
        String username = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_EMAIL);
        String encryptedPassword = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_PASSWORD);
        String activationCode = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_ACTIVATION_CODE);

        StringBuilder errorMessage = new StringBuilder();
        try {
            if (StringUtils.isBlank(username)) {
                return false;
            }

            String password = CryptoUtil.decode(CryptoUtil.getDefault(encryptedPassword));
            return ActivationInfoCollector.activate(username, password, errorMessage);
        } catch (Exception ex) {
            LogUtil.logError(ex);
        }
        
        return false;
    }

    private static void activateTestOpsFeatures() {
        if (KatalonApplicationActivator.getFeatureActivator() != null) {
            try {
                String email = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_EMAIL);
                String encryptedPassword = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_PASSWORD);
                String password = CryptoUtil.decode(CryptoUtil.getDefault(encryptedPassword));
                Organization org = new Organization();
                String jsonObject = ApplicationInfo.getAppProperty(ApplicationStringConstants.KA_ORGANIZATION);
                if (StringUtils.isNotBlank(jsonObject)) {
                    try {
                        org = JsonUtil.fromJson(jsonObject, Organization.class);
                    } catch (IllegalArgumentException e) {
                        LogUtil.logError(e);
                    }
                }
                Long orgId = org.getId();

                String serverUrl = ApplicationInfo.getTestOpsServer();
                String ksVersion = VersionUtil.getCurrentVersion().getVersion();
                activateFeatures(serverUrl, email, password, orgId, ksVersion);
            } catch (GeneralSecurityException | IOException e) {
                LogUtil.logError(e);
            }
        }
    }

    public static void activateFeatures(String serverUrl, String email, String password, long orgId, String ksVersion) {
        Set<String> featureKeys = KatalonApplicationActivator.getFeatureActivator().getFeatures(serverUrl, email,
                password, Long.valueOf(orgId), ksVersion);
        IFeatureService instance = FeatureServiceConsumer.getServiceInstance();
        for (String featureKey : featureKeys) {
            instance.enable(featureKey);
        }
    }

    private static int getHostNameHashValue() throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName();
        String ipAddress = InetAddress.getLocalHost().getHostAddress();

        if (hostName.equals(ipAddress)) {
            hostName = DEFAULT_HOST_NAME;
        }

        return Objects.hash(hostName);
    }

    private static String collectActivationInfo(String userName, String pass) {
        JsonObject traits = traitsWithAppInfo();
        traits.addProperty("password", pass);

        JsonObject activationObject = new JsonObject();
        activationObject.addProperty("userId", userName);
        activationObject.add("traits", traits);

        return activationObject.toString();
    }

    public static JsonObject traitsWithAppInfo() {
        JsonObject traits = new JsonObject();
        String katVersion = ApplicationInfo.versionNo() + " build " + ApplicationInfo.buildNo();
        String osType = Platform.getOSArch().contains("64") ? "64" : "32";
        String host = "";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
            host = "unknown";
        }

        traits.addProperty("host_name", host);
        traits.addProperty("os", Platform.getOS());
        traits.addProperty("os_type", osType);
        traits.addProperty("kat_version", katVersion);
        traits.addProperty("kat_type", System.getProperty("sun.arch.data.model"));
        traits.addProperty(UsagePropertyConstant.PROPERTY_SESSION_ID, KatalonApplication.SESSION_ID);
        traits.addProperty(UsagePropertyConstant.PROPERTY_USER_KEY, KatalonApplication.USER_KEY);
        return traits;
    }

    public static boolean activate(String userName, String pass, StringBuilder errorMessage) {
        boolean activatedResult = false;
        try {
            String userInfo = collectActivationInfo(userName, pass);
            String result = ServerAPICommunicationUtil.post("/segment/identify", userInfo);
            if (result.equals(ApplicationMessageConstants.SEND_SUCCESS_RESPONSE)) {
                activatedResult = true;
            } else if (errorMessage != null) {
                errorMessage.append(ApplicationMessageConstants.ACTIVATE_INFO_INVALID);
            }

        } catch (IOException ex) {
            LogUtil.logError(ex, ApplicationMessageConstants.ACTIVATION_COLLECT_FAIL_MESSAGE);
            if (errorMessage != null) {
                errorMessage.delete(0, errorMessage.length());
                errorMessage.append(ApplicationMessageConstants.NETWORK_ERROR);
            }
        } catch (Exception e) {
            LogUtil.logError(e);
        }

        return activatedResult;
    }
    
    public static boolean activateOffline(String activationCode, StringBuilder errorMessage) {
        try {
            License license = LicenseService.getInstance().parseJws(activationCode);
            if (isValidLicense(license)) {
                markActivatedForOfflineMode(activationCode);
                enableFeatures(license);
                return true;
            } else if (errorMessage != null) {
                errorMessage.append(ApplicationMessageConstants.ACTIVATION_CODE_INVALID);
            }
        } catch (Exception ex) {
            LogUtil.logError(ex);
            if (errorMessage != null) {
                errorMessage.append(ApplicationMessageConstants.ACTIVATION_CODE_INVALID);
            }
        }

        return false;
    }
    
    private static boolean isValidLicense(License license) {
        return hasValidMachineId(license) && !isExpired(license);
    }
    
    private static boolean hasValidMachineId(License license) {
        try {
            String machineId = MachineUtil.getMachineId();
            return license.getMachineId().equals(machineId);
        } catch (Exception e) {
            LogUtil.logError(e);
            return false;
        }
    }
    
    private static boolean isExpired(License license) {
        Date currentDate = new Date();
        return currentDate.after(license.getExpirationDate());
    }
    
    private static void enableFeatures(License license) {
        List<Feature> features = license.getFeatures();
        IFeatureService featureService = FeatureServiceConsumer.getServiceInstance();
        for (Feature feature : features) {
            featureService.enable(feature.getKey());
        }
    }


    public static void markActivated(String userName, String password) throws Exception {
        setActivatedVal();
        ApplicationInfo.removeAppProperty(ApplicationStringConstants.REQUEST_CODE_PROP_NAME);
        ApplicationInfo.setAppProperty(ApplicationStringConstants.ARG_EMAIL, userName, true);
        String encryptedPassword = CryptoUtil.encode(CryptoUtil.getDefault(password));
        ApplicationInfo.setAppProperty(ApplicationStringConstants.ARG_PASSWORD, encryptedPassword, true);
    }

    private static void markActivatedForOfflineMode(String activationCode) throws Exception {
        setActivatedValForOfflineMode();
        ApplicationInfo.setAppProperty(ApplicationStringConstants.ARG_ACTIVATION_CODE, activationCode, true);
    }
    
    private static void setActivatedValForOfflineMode() throws Exception {
        setActivatedVal();
        ApplicationInfo.setAppProperty(ApplicationStringConstants.ARG_OFFLINE_ACTIVATION, "true", true);
    }

    private static void setActivatedVal() throws Exception {
        String activatedVal = Integer.toString(getHostNameHashValue());
        String curVersion = new StringBuilder(ApplicationInfo.versionNo().replaceAll("\\.", "")).reverse().toString();
        ApplicationInfo.setAppProperty(ApplicationStringConstants.ACTIVATED_PROP_NAME, curVersion + "_" + activatedVal,
                true);
    }

    public static void markActivatedViaUpgradation(String versionNumber) {
        ApplicationInfo.setAppProperty(ApplicationStringConstants.UPDATED_VERSION_PROP_NAME,
                getVersionNo(versionNumber), true);
    }

    private static String getVersionNo(String versionNumber) {
        if (versionNumber == null) {
            return versionNumber;
        }
        String[] numbers = versionNumber.split("\\.");
        while (numbers.length < 3) {
            numbers = ArrayUtils.add(numbers, "0");
        }
        return StringUtils.join(ArrayUtils.subarray(numbers, 0, 3), ".");
    }
}

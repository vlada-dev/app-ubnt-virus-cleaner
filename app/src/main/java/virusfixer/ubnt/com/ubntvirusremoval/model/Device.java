package virusfixer.ubnt.com.ubntvirusremoval.model;

import android.util.Log;

import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import virusfixer.ubnt.com.ubntvirusremoval.exception.ConnectionFailedException;
import virusfixer.ubnt.com.ubntvirusremoval.exception.LoginException;
import virusfixer.ubnt.com.ubntvirusremoval.networking.SSH;

/**
 * Created by Vlad on 14.5.16.
 */
public class Device {

    public static final int WAITING = 1;
    public static final int CONNECTING = 2;
    public static final int CLEANING = 3;
    public static final int UPGRADING_FIRMWARE = 4;
    public static final int VIRUS_DETECTED = 5;
    public static final int VIRUS_NOT_DETECTED = 6;
    public static final int VIRUS_REMOVED = 7;
    public static final int UPGRADED = 8;
    public static final int LOGIN_FAILED = 9;
    public static final int VIRUS_REMOVED_UPGRADED = 10;
    public static final int VIRUS_NOT_DETECTED_UPGRADING = 11;
    public static final int REBOOT = 12;
    private static final int CONNECT_FAILED = 13;
    String ip;
    String username;
    String password;
    private boolean canRetry = true;
    private String firmwareVersion;
    private int status;
    private SSH sshClient;
    private int mPort;
    private boolean mIsInfected;


    public static final int FIRMWARE_VERSION_SEGMENT_COUNT = 3;

    public Device(String ip, String ipUsername, String ipPassword, int port) {
        this.ip = ip;
        this.username = ipUsername;
        this.password = ipPassword;
        this.mPort = port;
        this.firmwareVersion = "";
        this.status = WAITING;

    }

    public String getIp() {
        return ip;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getStatusText() {
        switch(status) {
            case WAITING:
                return "Waiting for check";
            case CONNECTING:
                return "Processing";
            case CLEANING:
                return "Cleaning virus";
            case UPGRADING_FIRMWARE:
                return "Upgrading firmware";
            case VIRUS_DETECTED:
                return "Virus detected";
            case VIRUS_NOT_DETECTED:
                return "Clean, done";
            case VIRUS_NOT_DETECTED_UPGRADING:
                return "Clean, upgrading firmware";
            case VIRUS_REMOVED:
                return "Virus removed, rebooting";
            case VIRUS_REMOVED_UPGRADED:
                return "Virus removed,fw upgraded, rebooting";
            case UPGRADED:
                return "Firmware upgraded, rebooting";
            case LOGIN_FAILED:
                return "Login failed";
            case CONNECT_FAILED:
                return "Unreachable";
        }
        return "Unknown";
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean canRetry() {
        return canRetry;
    }

    private boolean ping(String ip, int count) {
        Process p1 = null;
        try {
            p1 = Runtime.getRuntime().exec("ping -c " +String.valueOf(count) + " " + ip);
            int returnVal = p1.waitFor();
            if (returnVal>0) {
                return false;
            }
        } catch (IOException | InterruptedException e) {
            return true;
        }
        return true;

    }

    public void check(ArrayList<Login> mLoginList, boolean removeVirus, boolean upgradeFirmware) throws ConnectionFailedException, JSchException, LoginException {
        status = CONNECTING;
        mIsInfected = false;
        Log.v("UBNT", "Connecting " + ip);

        if (!ping(ip, 1) && !ping(ip,3)) {
            status = CONNECT_FAILED;
            return;
        }



        this.sshClient = new SSH(this.username, this.password, this.ip, this.mPort);
        if (username != null && password != null) {
            try {
                firmwareVersion = sshClient.getFirmwareVersion();
            } catch (LoginException e) {
                //reset username and password
                username = null;
                password = null;
            } catch (ConnectionFailedException | JSchException e) {
                status = CONNECT_FAILED;
            }
        }
        if (username == null || password == null) {
            for(Login l: mLoginList) {
                sshClient.setLogin(l.getUsername(), l.getPassword());
                try {
                    firmwareVersion = sshClient.getFirmwareVersion();
                } catch (LoginException e) {
                    continue;
                } catch (ConnectionFailedException | JSchException e) {
                    status = CONNECT_FAILED;
                }
                username = l.getUsername();
                password = l.getPassword();
                break;

            }
        }

        if (username == null || password == null) {
            canRetry = false;
            status = LOGIN_FAILED;
            throw new LoginException();
        }

        //check infection status
        if (sshClient.isVirusInfected()) {
            status = VIRUS_DETECTED;
            mIsInfected = true;

            if (removeVirus) {

                if (upgradeFirmware) {
                    boolean isUpgradable = isUpgradable();

                    if (isUpgradable) {
                        String fwLink = getFirmwareDownloadLink();
                        sshClient.removeVirus(fwLink);
                        if (fwLink != null) {
                            status = VIRUS_REMOVED_UPGRADED;
                        } else {
                            status = VIRUS_REMOVED;
                        }
                    } else {
                        sshClient.removeVirus(null);
                        status = VIRUS_REMOVED;
                    }
                } else {
                    status = CLEANING;
                    sshClient.removeVirus(null);
                    status = VIRUS_REMOVED;
                }
                //wait for reboot
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } else {


            if (upgradeFirmware && isUpgradable()) {
                status = VIRUS_NOT_DETECTED_UPGRADING;
                sshClient.upgradeFirmware(getFirmwareDownloadLink());
                //wait for reboot
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                status = UPGRADED;
            } else {
                status = VIRUS_NOT_DETECTED;
            }
        }
        sshClient.disconnect();
        sshClient = null;
    }



    private String getFirmwareDownloadLink() {
        if (firmwareVersion != null && firmwareVersion.length()>5) {
            if (firmwareVersion.startsWith("XM")) {
                return "http://dl.ubnt.com/firmwares/XN-fw/v5.6.5/XM.v5.6.5.29033.160515.2119.bin";
            } else if (firmwareVersion.startsWith("TI")) {
                return "http://dl.ubnt.com/firmwares/XN-fw/v5.6.5/TI.v5.6.5.29033.160515.2058.bin";
            } else if (firmwareVersion.startsWith("XW")) {
                return "http://dl.ubnt.com/firmwares/XW-fw/v5.6.5/XW.v5.6.5.29033.160515.2108.bin";
            } else if (firmwareVersion.startsWith("AirGW.")) {
                return "http://dl.ubnt.com/firmwares/airgateway/v1.1.6/AirGW.v1.1.6.28062.150731.1510.bin";
            } else if (firmwareVersion.startsWith("AirGWP.")) {
                return "http://dl.ubnt.com/firmwares/airgateway/v1.1.6/AirGWP.v1.1.6.28062.150731.1520.bin";
            } else if (firmwareVersion.startsWith("WA.")) {
                return "http://dl.ubnt.com/firmwares/XC-fw/v7.2/WA.v7.2.30339.160210.1421.bin";
            } else if (firmwareVersion.startsWith("XC.")) {
                return "http://dl.ubnt.com/firmwares/XC-fw/v7.2/XC.v7.2.30339.160210.1420.bin";
            } else if (firmwareVersion.startsWith("SW.")) {
                return "http://dl.ubnt.com/firmwares/TOUGHSwitch/v1.3.2/SW.v1.3.2.27935.150716.1133.bin";
            } else if (firmwareVersion.startsWith("AF5X.")) {
                return "http://dl.ubnt.com/firmwares/airfiber5X/v3.2.1/AF5X.v3.2.1.bin";
            } else if (firmwareVersion.startsWith("AF24.")) {
                return "http://dl.ubnt.com/firmwares/airfiber/v3.2/AF24.v3.2.bin";
            } else if (firmwareVersion.startsWith("AF5.")) {
                return "http://dl.ubnt.com/firmwares/airfiber5/v3.2/AF5.v3.2.bin";
            } else if (firmwareVersion.startsWith("AF3X.")) {
                return "http://dl.ubnt.com/firmwares/airfiber3X/v3.2/AF3X.v3.2.bin";
            } else if (firmwareVersion.startsWith("AF2X.")) {
                return "http://dl.ubnt.com/firmwares/airfiber2X/v3.2/AF2X.v3.2.bin";
            }
        }
        return null;
    }

    private boolean isUpgradable() {
        if (firmwareVersion != null && firmwareVersion.length()>5) {
            if (firmwareVersion.startsWith("XM") || firmwareVersion.startsWith("TI") || firmwareVersion.startsWith("XW")) {
                return !hasMinVersion("5.6.5");

            } else if (firmwareVersion.startsWith("AirGW.") || firmwareVersion.startsWith("AirGWP.")) {
                return !hasMinVersion("1.1.6");
            }  else if (firmwareVersion.startsWith("WA.") || firmwareVersion.startsWith("XC.")) {
                return !hasMinVersion("7.2.30339");
            }  else if (firmwareVersion.startsWith("SW.")) {
                return !hasMinVersion("1.3.2");
            } else if (firmwareVersion.startsWith("AF5X.")) {
                return !hasMinVersion("3.2.1");
            } else if (firmwareVersion.startsWith("AF24.") || firmwareVersion.startsWith("AF5.") || firmwareVersion.startsWith("AF3X.") || firmwareVersion.startsWith("AF2X.")) {
                return !hasMinVersion("3.2");
            }
        }
        return false;
    }

    private boolean hasMinVersion(String minVersion) {

        String[] deviceVersionSegments = firmwareVersion.split("\\.",2);
        if (deviceVersionSegments.length < 2) {
            return true;
        }
        String fwVersion = deviceVersionSegments[1].substring(1);
        return compareFirmwareVersion(fwVersion, minVersion);



    }

    private static Boolean compareFirmwareVersion(String firmA, String firmB)
    {

        String[] explodedCurrent = firmA.split("\\.");
        String[] explodedLatest = firmB.split("\\.");

        int i;
        for(i = 0; i < explodedLatest.length && i < explodedCurrent.length && i < FIRMWARE_VERSION_SEGMENT_COUNT; i++)
        {
            int latestPart, currentPart;
            try
            {
                latestPart = Integer.parseInt(explodedLatest[i]);
                currentPart = Integer.parseInt(explodedCurrent[i]);
            }
            catch(NumberFormatException e)
            {
                return i != 0;
            }

            if(latestPart > currentPart)
            {
                return false;
            }
            else if(latestPart < currentPart)
            {
                return true;
            }
        }
        if(i == FIRMWARE_VERSION_SEGMENT_COUNT)
        {
            //first N segments are equal
            return true;
        }
        else
        {
            return explodedLatest.length <= explodedCurrent.length;
        }
    }

    public boolean hasWarningStatus() {
        return status == VIRUS_DETECTED || status == LOGIN_FAILED || status == CONNECT_FAILED;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getStatus() {
        return status;
    }

    public void releaseSsh() {
        if (this.sshClient != null) {
            try {
                sshClient.disconnect();
            } catch (Exception ignored) {

            }
            sshClient = null;
        }
    }

    public boolean isInfected() {
        return mIsInfected;
    }
}

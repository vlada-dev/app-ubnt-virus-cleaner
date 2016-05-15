package virusfixer.ubnt.com.ubntvirusremoval;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import virusfixer.ubnt.com.ubntvirusremoval.adapter.DeviceAdapter;
import virusfixer.ubnt.com.ubntvirusremoval.model.Device;
import virusfixer.ubnt.com.ubntvirusremoval.model.Login;
import virusfixer.ubnt.com.ubntvirusremoval.networking.Checker;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_IP_LIST_SIZE = 65536;
    private static final int DEFAULT_SSH_PORT = 22;
    ArrayList<Device> mDeviceList;
    int mTotalIp;
    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private ArrayList<Login> mLoginList;
    private DeviceAdapter mAdapter;
    private String mLoginListText;
    private String mIpListText;
    private Checker mChecker;
    private boolean mUpgradeFirmware;
    private boolean mRemoveVirus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void start(View v) {

        mIpListText = ((EditText) findViewById(R.id.input_ip)).getText().toString();
        mLoginListText = ((EditText)findViewById(R.id.input_password)).getText().toString();
        mRemoveVirus = ((CheckBox)findViewById(R.id.check_remove_virus)).isChecked();
        mUpgradeFirmware = ((CheckBox)findViewById(R.id.check_upgrade_firmware)).isChecked();
        mDeviceList = new ArrayList<>();
        mLoginList = new ArrayList<>();
        mTotalIp = 0;
        if (!parseIpList(mIpListText)) {
            return;
        }
        if (!parseLoginList(mLoginListText)) {
            return;
        }
        startCheck();


    }

    private void startCheck() {

        setContentView(R.layout.scanning_screen);
        ListView lv = (ListView) findViewById(R.id.list);
        this.mAdapter = new DeviceAdapter(this, mDeviceList);
        lv.setAdapter(mAdapter);
        mChecker = new Checker(this, mLoginList, mRemoveVirus, mUpgradeFirmware);
        mChecker.start(mDeviceList);




    }

    public static boolean validateIp(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

    private boolean parseLoginList(String loginListText) {
        if (loginListText.length() == 0) {
            return true;
        }
        String[] loginList = loginListText.split("\n");
        if (loginList.length == 0) {
            return true;
        }

        for(String loginLine: loginList) {

            if (loginLine.length() == 0) {
                continue;
            }
            if (loginLine.contains(",")) {
                //line may contain login credentials
                String[] t = loginLine.split(",",2);
                if (t.length == 2) {
                    mLoginList.add(new Login(t[0],t[1]));
                }
            }





        }
        return true;


    }

    private int pack(byte[] bytes) {
        int val = 0;
        for (int i = 0; i < bytes.length; i++) {
            val <<= 8;
            val |= bytes[i] & 0xff;
        }
        return val;
    }

    private byte[] unpack(int bytes) {
        return new byte[] {
                (byte)((bytes >>> 24) & 0xff),
                (byte)((bytes >>> 16) & 0xff),
                (byte)((bytes >>>  8) & 0xff),
                (byte)((bytes       ) & 0xff)
        };
    }

    private int inetAton(String ipString) throws UnknownHostException {
        byte[] ip = InetAddress.getByName(ipString).getAddress();
        return pack(ip);
    }

    private String inetNtoa(int ipLong) throws UnknownHostException {
        return InetAddress.getByAddress(unpack(ipLong)).getHostAddress();
    }

    private int getFirstIp(int ipLong, int netmask) {

        int netsize = (int) Math.pow(2,32-netmask);
        int left = ipLong % netsize;
        return ipLong - left + 1;
    }

    private int getLastIp(int ipLong, int netmask) {
        int netsize = (int) Math.pow(2,32-netmask);
        int left = ipLong % netsize;
        int rangeStart = ipLong - left;
        return rangeStart + netsize - 2;
    }

    private void showToast(String text) {
        Toast.makeText(this,text, Toast.LENGTH_LONG).show();
    }

    private boolean parseIpList(String ipListText) {
        if (ipListText.length() == 0) {
            showToast("No IP address specified");
            return false;
        }
        String[] ipList = ipListText.split("\n");
        if (ipList.length == 0) {
            showToast("No IP address specified");
            return false;
        }
        String ip, ipUsername, ipPassword;
        int port;
        for(String ipLine: ipList) {

            if (ipLine.length() == 0) {
                continue;
            }
            if (ipLine.contains(",")) {
                //line may contain login credentials
                String[] t = ipLine.split(",",3);
                if (t.length == 3) {
                    ip = t[0];
                    ipUsername = t[1];
                    ipPassword = t[2];
                } else {
                    ip = ipLine.replace(",","");
                    ipUsername = null;
                    ipPassword = null;
                }
            } else {
                ip = ipLine;
                ipUsername = null;
                ipPassword = null;
            }

            if (ip.contains(":")) {
                String[] sp = ip.split(":",2);
                ip = sp[0];
                try {
                    port = Integer.valueOf(sp[1]);
                } catch (NumberFormatException e) {
                    port = DEFAULT_SSH_PORT;
                }
            } else {
                port = DEFAULT_SSH_PORT;
            }

            if (ip.contains("/")) {
                //CIDR subnet format
                String[] s = ip.split("/");
                if (s.length!=2 || !validateIp(s[0])) {
                    showToast("IP subnet " + ip + " is not in correct CIDR format network/mask!");
                    return false;
                }
                int netmask;
                try {
                    netmask = Integer.parseInt(s[1]);
                } catch (Exception e) {
                    showToast("IP netmask for " + ip + " is not integer!");
                    return false;
                }
                if (netmask<16) {
                    showToast("IP netmask for " + ip + " cannot be lower than 16! Max allowed limit of IP address to test is 65536.");
                    return false;
                }
                if (netmask>32) {
                    showToast("IP netmask for " + ip + " cannot be higher than 32!");
                    return false;
                }
                int ipLong;
                try {
                    ipLong = inetAton(s[0]);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    showToast("IP subnet " + ip + " is not in correct CIDR format network/mask!");
                    return false;
                }
                int firstIp = getFirstIp(ipLong, netmask);
                int lastIp = getLastIp(ipLong, netmask);

                if (lastIp-firstIp+1+mTotalIp>MAX_IP_LIST_SIZE) {
                    showToast("IP subnet " + ip + " would reach maximum number of hosts to check (65536)!");
                    return false;
                }

                for (int i=firstIp;i<=lastIp;i++) {
                    try {
                        mDeviceList.add(new Device(inetNtoa(i), ipUsername, ipPassword, port));
                        mTotalIp++;
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (!validateIp(ip)) {
                    showToast("IP " + ip + " is not in correct format!");
                    return false;
                }
                mDeviceList.add(new Device(ip, ipUsername, ipPassword, port));
                mTotalIp++;

            }



        }
        return true;
    }

    public void stop(View view) {
        if (mChecker != null) {
            mChecker.stop();
        }
        findViewById(R.id.stopButton).setVisibility(View.GONE);
        findViewById(R.id.closeButton).setVisibility(View.VISIBLE);

    }

    public void close(View view) {
        setContentView(R.layout.activity_main);
        ((EditText) findViewById(R.id.input_ip)).setText(getIpListText());
        ((EditText) findViewById(R.id.input_password)).setText(mLoginListText);

    }

    public void setProgressBar(int percentage) {

        ProgressBar pb = ((ProgressBar)findViewById(R.id.progressBar));
        if (pb != null) {
            pb.setProgress(percentage);
        }

    }


    private String getIpListText() {
        StringBuilder sb = new StringBuilder();
        for(Device d: mDeviceList) {
           sb.append(d.getIp());
            if (d.getUsername() != null && d.getPassword()!=null) {
                sb.append(",");
                sb.append(d.getUsername());
                sb.append(",");
                sb.append(d.getPassword());
            }
            sb.append("\n");

        }
        return sb.toString();
    }

    public void finished() {
        showToast("Finished");

        try {
            findViewById(R.id.stopButton).setVisibility(View.GONE);
            findViewById(R.id.closeButton).setVisibility(View.VISIBLE);
        } catch (NullPointerException e) {

        }
    }

    public void notifyUpdate() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // actionbar actions

        int id = item.getItemId();

        if (id == android.R.id.home) {
            //Helper.toast(this, "Home");

        } else if (id == R.id.show_help) {
            // add new devices
            Intent intent = new Intent(MainActivity.this, HelpScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);

        }

        return super.onOptionsItemSelected(item);
    }
}

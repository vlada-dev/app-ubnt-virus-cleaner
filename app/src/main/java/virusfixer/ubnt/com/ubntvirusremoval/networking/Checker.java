package virusfixer.ubnt.com.ubntvirusremoval.networking;

import com.jcraft.jsch.JSchException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import virusfixer.ubnt.com.ubntvirusremoval.MainActivity;
import virusfixer.ubnt.com.ubntvirusremoval.exception.ConnectionFailedException;
import virusfixer.ubnt.com.ubntvirusremoval.exception.LoginException;
import virusfixer.ubnt.com.ubntvirusremoval.model.Device;
import virusfixer.ubnt.com.ubntvirusremoval.model.Login;

/**
 * Created by Vlad on 14.5.16.
 */
public class Checker {

    public static final int MAX_THREADS = 10;
    private final MainActivity mActivity;
    private final ArrayList<Login> mLoginList;
    private final boolean mUpgradeFirmware;
    private final boolean mRemoveVirus;
    private boolean isFinished = false;
    private int mTotalDevices = 0;

    Queue<Device> mDevices=new LinkedList<>();
    Thread[] taskList = new Thread[MAX_THREADS];
    private int mDevicesFinished;

    public Checker(MainActivity activity, ArrayList<Login> loginList, boolean removeVirus, boolean upgradeFirmware) {
        this.mActivity = activity;
        this.mLoginList = loginList;
        this.mUpgradeFirmware = upgradeFirmware;
        this.mRemoveVirus = removeVirus;
    }

    public void start(ArrayList<Device> list) {
        mDevices.clear();
        mDevices.addAll(list);
        mTotalDevices = list.size();
        mDevicesFinished = 0;
        mActivity.setProgressBar(0);

            for (int i = 0; i < MAX_THREADS; i++) {
                if (mDevices.size()>0) {
                    startNewTask(i, mDevices.poll());
                }
            }

    }

    public void notifyFinishedOk(int pos,Device d) {

        int status = d.getStatus();
        if (status == Device.VIRUS_REMOVED || status == Device.UPGRADED || status == Device.VIRUS_REMOVED_UPGRADED) {
            //rebooting
            mDevices.add(d);
        } else {
            mDevicesFinished++;
        }


        notifyFinished(pos);
        mActivity.notifyUpdate();

    }

    public void notifyFinishedFailed(int pos, Device d, boolean doRetry) {
        if (doRetry) {
            //repeat
            mDevices.add(d);
        } else {
            mDevicesFinished++;
        }
        notifyFinished(pos);
        mActivity.notifyUpdate();

    }

    public void notifyFinished(int position) {
        mActivity.setProgressBar(Math.round((mDevicesFinished/(mTotalDevices*1.0f))*100));
        int activeThreads = 0;
        taskList[position] = null;
        for(int i=0;i<MAX_THREADS;i++) {
            Thread task = taskList[i];
            if (task == null) {
                if (mDevices.size()>0) {
                    startNewTask(i, mDevices.poll());
                    activeThreads++;
                }
            } else {
                activeThreads++;
            }
        }
        if (activeThreads == 0 && !isFinished) {
            //done
            isFinished = true;
            mActivity.finished();

        }
    }

    private void startNewTask(int i, Device d) {
        if (isFinished) {
            taskList[i]= null;
            return;
        }
        taskList[i] = runCheckThread(i, d);

    }

    public void stop() {
        isFinished = true;
        mDevices.clear();
    }

    private Thread runCheckThread(final int pos,final Device dev) {

        Thread t = new Thread() {
            public void run() {

                try
                {
                    dev.check(mLoginList, mRemoveVirus, mUpgradeFirmware);

                }
                catch(LoginException | ConnectionFailedException | JSchException e)
                {
                    e.printStackTrace();

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyFinishedFailed(pos, dev, dev.canRetry());

                        }
                    });
                }

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            notifyFinishedOk(pos,dev);

                    }
                });
            }
        };
        t.start();
        return t;
    }


}

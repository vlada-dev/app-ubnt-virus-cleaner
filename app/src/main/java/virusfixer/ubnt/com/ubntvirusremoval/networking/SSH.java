package virusfixer.ubnt.com.ubntvirusremoval.networking;

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import virusfixer.ubnt.com.ubntvirusremoval.exception.ConnectionFailedException;
import virusfixer.ubnt.com.ubntvirusremoval.exception.LoginException;


public class SSH
{

	private final static String TAG = "SSH";
	private String username;
	private String password;
	private String hostname;
	private int port;
	private Session session;




	private static void closeQuietly(Closeable closeable)
	{
		if(closeable == null)
		{
			return;
		}
		try
		{
			closeable.close();
		}
		catch(IOException ignored)
		{
			ignored.printStackTrace();
		}
	}


	public SSH(String username, String password, String hostname, int port)
	{
		this.username = username;
		this.password = password;
		this.hostname = hostname;
		this.port = port;
	}


	public void connect() throws JSchException
	{
		JSch jsch = new JSch();

		if(this.session != null && !this.session.isConnected())
		{
			this.session.disconnect();
		}

		session = jsch.getSession(username, hostname, port);

		session.setPassword(password);

		// Avoid asking for key confirmation
		Properties prop = new Properties();
		prop.put("StrictHostKeyChecking", "no");
		session.setConfig(prop);
		Log.v(TAG, "Connecting SSH");
		session.connect(40000);
		Log.v(TAG, "Connected SSH");
		session.setTimeout(20000);
	}





	public String executeRemoteCommand(String command) throws JSchException
	{
		// SSH Channel
		ChannelExec channelssh = (ChannelExec) session.openChannel("exec");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		channelssh.setOutputStream(baos);
		// Execute command
		channelssh.setCommand(command);
		channelssh.connect();
		try
		{
			awaitChannelClosure(channelssh);
		}
		catch(InterruptedException e)
		{
			// no one cares
		}
		String data = baos.toString();
		closeQuietly(baos);
		channelssh.disconnect();

		return data;
	}




	public String execute(String command) throws LoginException,
			ConnectionFailedException, JSchException
	{

		if(this.session == null || !this.session.isConnected())
		{
			Log.v(TAG, "Reconnecting " + this.hostname);
			this.connect();
		}
		return executeRemoteCommand(command);

	}


	public String executeWithRetry(String command, int maxRetries) throws LoginException,
			ConnectionFailedException, JSchException
	{

		try
		{
			return execute(command);
		}
		catch(JSchException e)
		{
			if(e.getMessage().equals("Auth fail"))
			{
				throw new LoginException();
			}
			if(maxRetries > 0)
			{
				try
				{
					Thread.sleep(500);
				}
				catch(InterruptedException e1)
				{
					e1.printStackTrace();
				}
				return executeWithRetry(command, maxRetries - 1);
			}
			else
			{
				if(e.getMessage().equals("Auth fail"))
				{
					throw new LoginException();
				}
				else
				{
					throw new ConnectionFailedException();
				}
			}
		}
		catch(ConnectionFailedException e)
		{
			if(maxRetries > 0)
			{
				try
				{
					Thread.sleep(500);
				}
				catch(InterruptedException e1)
				{
					e1.printStackTrace();
				}
				return executeWithRetry(command, maxRetries - 1);
			}
			else
			{
				throw e;
			}
		}

	}





	public String getFirmwareVersion() throws JSchException, LoginException, ConnectionFailedException
	{

		String version = executeWithRetry("cat /etc/version", 3);
		if(version != null)
		{
			return version.trim();
		}
		return null;

	}


	public void setLogin(String username, String password)
	{
		this.username = username;
		this.password = password;
		this.session = null;
	}







	private void awaitChannelClosure(Channel channelssh)
			throws InterruptedException
	{
		while(channelssh.isConnected())
		{
			Thread.sleep(100);
		}
	}

    public boolean isVirusInfected() throws LoginException, JSchException, ConnectionFailedException {
        String dirList = executeWithRetry("ls /etc/persistent", 3);
        if(dirList != null)
        {
            if (dirList.contains("mf.tar")) {
                return true;
            }
        }
        String pList = executeWithRetry("ps -w", 3);
        if(pList != null)
        {
            if (pList.contains(".mf/infect") || pList.contains(".mf/search") || pList.contains(".mf/mother") || pList.contains(".mf/curl")) {
                return true;
            }

        }
        return false;
    }

    public void removeVirus(String firmwareUpgradeLink) throws LoginException, JSchException, ConnectionFailedException {
        String res;
        if (firmwareUpgradeLink != null) {

            res = executeWithRetry("kill -9 $(pgrep mother);kill -9 $(pgrep search); kill -9 $(pgrep sleep);rm /tmp/*.bin;rm /etc/persistent/mf.tar;rm /etc/persistent/rc.poststart;rm -R /etc/persistent/.mf;cfgmtd -p /etc/persistent/ -w;wget -O/tmp/fwupdate.bin " + firmwareUpgradeLink +
                    " && /usr/bin/ubntbox fwupdate.real -m /tmp/fwupdate.bin 2>&1", 3);
        } else {
            res = executeWithRetry("kill -9 $(pgrep mother);kill -9 $(pgrep search); kill -9 $(pgrep sleep);rm /tmp/*.bin;rm /etc/persistent/mf.tar;rm /etc/persistent/rc.poststart;rm -R /etc/persistent/.mf;cfgmtd -p /etc/persistent/ -w;reboot ", 3);
        }

        Log.v("UPDATE", res);

    }

    public void upgradeFirmware(String firmwareUpgradeLink) throws LoginException, JSchException, ConnectionFailedException {
        String res = executeWithRetry("rm /tmp/*.bin;wget -O/tmp/fwupdate.bin " + firmwareUpgradeLink + " && /usr/bin/ubntbox fwupdate.real -m /tmp/fwupdate.bin 2>&1", 3);
        Log.v("UPDATE", res);
    }
}

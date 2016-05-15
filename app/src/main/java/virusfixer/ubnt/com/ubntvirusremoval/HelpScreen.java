package virusfixer.ubnt.com.ubntvirusremoval;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

/**
 * Created by Vlad on 15.5.16.
 */
public class HelpScreen extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_screen);
        WebView webView = ((WebView)findViewById(R.id.webView1));
        webView.loadUrl("file:///android_asset/test.html");

    }
}

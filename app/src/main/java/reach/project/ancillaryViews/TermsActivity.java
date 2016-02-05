package reach.project.ancillaryViews;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;

import reach.project.R;

public class TermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.termsToolbar);
        toolbar.setTitle("Terms and Conditions");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        final WebView webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl("http://letsreach.co/terms");
    }
}

package cn.iinti.majora.adr.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import cn.iinti.majora.adr.R;
import cn.iinti.majora.adr.utils.NavUtil;
import cn.iinti.majora.adr.utils.RomUtils;
import cn.iinti.majora.adr.utils.ThemeUtil;


public class SupportActivity extends MajoraBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setTheme(this);
        setContentView(R.layout.activity_container);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(view -> finish());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.nav_item_support);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setFloating(toolbar, 0);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.container, new SupportFragment()).commit();
        }
    }

    public static class SupportFragment extends Fragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.tab_support, container, false);

            View installerSupportView = v.findViewById(R.id.installerSupportView);
            View faqView = v.findViewById(R.id.faqView);


            setupView(installerSupportView, R.string.about_support);
            setupView(faqView, R.string.support_faq_url);

            LinearLayout linearLayout = v.findViewById(R.id.donateWithAliPay);
            if (!RomUtils.checkApkExist(getActivity(), "com.eg.android.AlipayGphone")) {
                linearLayout.setVisibility(View.GONE);
            } else {
                linearLayout.setOnClickListener(v1 -> {
                    String intentFullUrl = "intent://platformapi/startapp?saId=10000007&" +
                            "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2FFKX05428F2CIZP9P1BZN9A%3F_s" +
                            "%3Dweb-other&_t=" + System.currentTimeMillis() + "#Intent;" +
                            "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
                    try {
                        Intent intent = Intent.parseUri(intentFullUrl, Intent.URI_INTENT_SCHEME);
                        getActivity().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            }

            return v;
        }

        public void setupView(View v, final int url) {
            v.setOnClickListener(v1 -> NavUtil.startURL(getActivity(), getString(url)));
        }
    }
}

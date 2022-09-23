package cn.iinti.majora.adr.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Browser;
import android.support.customtabs.CustomTabsIntent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import cn.iinti.majora.adr.R;
import cn.iinti.majora.adr.ui.DefaultSharedPreferenceHolder;

public final class NavUtil {

    private static Uri parseURL(String str) {
        if (str == null || str.isEmpty())
            return null;

        Spannable spannable = new SpannableString(str);
        Linkify.addLinks(spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        return (spans.length > 0) ? Uri.parse(spans[0].getURL()) : null;
    }

    private static void startURL(Activity activity, Uri uri) {
        if (!DefaultSharedPreferenceHolder.getInstance(activity).getPreferences().getBoolean("chrome_tabs", true)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, activity.getPackageName());
            activity.startActivity(intent);
            return;
        }

        CustomTabsIntent.Builder customTabsIntent = new CustomTabsIntent.Builder();
        customTabsIntent.setShowTitle(true);
        customTabsIntent.setToolbarColor(activity.getResources().getColor(R.color.colorPrimary));
        customTabsIntent.build().launchUrl(activity, uri);
    }

    public static void startURL(Activity activity, String url) {
        startURL(activity, parseURL(url));
    }

    public static void startApp(Context context, String packageName) {
        Intent launchIntent = getSettingsIntent(context, packageName);
        if (launchIntent != null) {
            // 优先使用 Shizuku
            // launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            ComponentName component = launchIntent.getComponent();
//            if (component != null) {
//                Intent intent = new Intent();
//                intent.setComponent(component);
//                if (ShizukuToolkit.startActivity(intent)) {
//                    Log.i(RatelManagerApp.TAG, "start app use Shizuku success");
//                    return;
//                }
//            }

            context.startActivity(launchIntent);
        }
//        else {
//            Toast.makeText(context,
//                    context.getString(R.string.module_no_ui),
//                    Toast.LENGTH_LONG).show();
//        }
    }


    private static Intent getSettingsIntent(Context activity, String packageName) {
        PackageManager pm = activity.getPackageManager();
        return pm.getLaunchIntentForPackage(packageName);
    }
}

package cn.iinti.majora.adr.utils;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import cn.iinti.majora.adr.R;
import cn.iinti.majora.adr.TheApp;
import cn.iinti.majora.adr.ui.DefaultSharedPreferenceHolder;
import cn.iinti.majora.adr.ui.MajoraBaseActivity;

public final class ThemeUtil {
    private static final int[] THEMES = new int[]{
            R.style.Theme_Majora_Light,
            R.style.Theme_Majora_Dark,
            R.style.Theme_Majora_Dark_Black,};

    private ThemeUtil() {
    }

    private static int getSelectTheme() {
        int theme = DefaultSharedPreferenceHolder.getInstance(TheApp.getApplication()).getPreferences().getInt("theme", 0);
        return (theme >= 0 && theme < THEMES.length) ? theme : 0;
    }

    public static void setTheme(MajoraBaseActivity activity) {
        activity.mTheme = getSelectTheme();
        activity.setTheme(THEMES[activity.mTheme]);
    }

    public static void reloadTheme(MajoraBaseActivity activity) {
        int theme = getSelectTheme();
        if (theme != activity.mTheme)
            activity.recreate();
    }

    public static int getThemeColor(Context context, int id) {
        Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(new int[]{id});
        int result = a.getColor(0, 0);
        a.recycle();
        return result;
    }

    public static void setTextView(View root, int id, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        TextView certificateIdTextView = root.findViewById(id);
        certificateIdTextView.setText(value);
    }

}

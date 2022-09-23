package cn.iinti.majora.adr.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import cn.iinti.majora.adr.R;
import cn.iinti.majora.adr.TheApp;
import cn.iinti.majora.adr.majora.CombineShellWrapper;
import cn.iinti.majora.adr.majora.MajoraClientService;
import cn.iinti.majora.adr.utils.ClientIdentifier;
import cn.iinti.majora.adr.utils.CommonUtils;

import eu.chainfire.libsuperuser.Shell;

public class MainPanelFragment extends Fragment {
    private View autoRedialPanel;

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(TheApp.getApplication());
        boolean autoRedial = spf.getBoolean("auto_redial", true)
                && CombineShellWrapper.available();

        autoRedialPanel.setVisibility(autoRedial ? View.VISIBLE : View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_main_panel, container, false);
        boolean rootAvailable = Shell.SU.available();

        TextView rootAv = view.findViewById(R.id.tv_root_available);
        rootAv.setText(rootAvailable ? "YES" : "NO");
        if (!rootAvailable) {
            rootAv.setTextColor(Color.RED);
        }

        TextView tvClientId = view.findViewById(R.id.tv_client_id);
        tvClientId.setText(ClientIdentifier.id());

        TextView tvServerConfig = view.findViewById(R.id.tv_server_config);
        tvServerConfig.setText(genServerConfigText());


        TextView tvBindingAccount = view.findViewById(R.id.tv_binding_account);
        tvBindingAccount.setText(getBindingAccountConf());


        Button btnReDial = view.findViewById(R.id.btn_redial);
        btnReDial.setOnClickListener(view1 -> {
            if (CombineShellWrapper.available()) {
                MajoraClientService.reDial(1000);
            } else {
                Toast.makeText(getActivity(), "重播需要root权限", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnResetClientId = view.findViewById(R.id.btn_reset_device_id);
        btnResetClientId.setOnClickListener(view12 -> {
            if (ClientIdentifier.reset()) {
                Toast.makeText(getActivity(), "重置成功,请重启app", Toast.LENGTH_LONG).show();
                getActivity().runOnUiThread(() -> System.exit(0));
            } else {
                Toast.makeText(getActivity(), "重置失败", Toast.LENGTH_SHORT).show();
            }
        });

        TextView reDialConfigTextView = view.findViewById(R.id.tv_redial_view);
        autoRedialPanel = view.findViewById(R.id.auto_redial_panel);
        SeekBar seekBar = view.findViewById(R.id.reDialSeekBar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.setMin(1);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                reDialConfigTextView.setText(String.valueOf(progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(TheApp.getApplication());
                spf.edit().putInt("auto_redial_duration", seekBar.getProgress()).apply();
            }
        });
        return view;
    }

    private String genServerConfigText() {
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(TheApp.getApplication());
        String serverHost = spf.getString("server_host", "majora.iinti.cn");
        int serverPort = CommonUtils.toInt(spf.getString("server_port", "5879"), 5879);
        return serverHost + ":" + serverPort;
    }

    private String getBindingAccountConf() {
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(TheApp.getApplication());

        return spf.getString("account_identifier", "");

    }
}

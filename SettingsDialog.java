package ru.ibecom.helpers;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import ru.ibecom.androidmap.R;
import ru.ibecom.api.interfaces.IBZoneManager;
import ru.ibecom.mapmodule.MapDataManager;
import ru.ibecom.mapmodule.Server;

/**
 * Created by Prog on 23.04.2015.
 */
public class SettingsDialog extends DialogFragment {
    public static final int BIND_CHANGE = 1;
    public static final int ROUTES_CHANGE = 2;
    public static final int SORT_CHANGE = 3;
    public static final int SERVER_CHANGE = 4;
    public static final int HIGHLIGHT_CHANGE = 5;
    public static final int DATA_DIR_CHANGE = 6;
    public static final int RESET_PUSH = 7;
    public static final int RESET_DATA = 8;
    public static final int UPDATE_CHANGE = 9;
    public static final int TEST_PUSH = 10;
    private Spinner sortSpinner;
    private Spinner serverSpinner;
    private Spinner dataSpinner;
    private CheckBox bindCheckBox;
    private CheckBox routesCheckBox;
    private CheckBox zonesCheckBox;
    private CheckBox updateCheckBox;
    private SettingsListener listener;
    private String[] dirs;
    private SettingsData settingsData;
    private EditText newAppId;
    public static Server[] servers = {Server.getLocal(), Server.getGlobal()};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsData = new SettingsData();
        settingsData.bind = SettingsHelper.isBindState(getActivity());
        settingsData.routes = SettingsHelper.isRoutesState(getActivity());
        settingsData.highlight = SettingsHelper.isZones(getActivity());
        settingsData.update = SettingsHelper.isUpdate(getActivity());
        settingsData.server = SettingsHelper.getServer(getActivity());
        settingsData.zoneSortRule = SettingsHelper.getZoneSortRule(getActivity());

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.settings_layout, null);
        onCreateView(rootView);
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        return dialog;
    }

    public void onCreateView(final View rootView) {
        sortSpinner = (Spinner) rootView.findViewById(R.id.sort_spinner);
        initSort();
        serverSpinner = (Spinner) rootView.findViewById(R.id.server_spinner);
        initServers();
        dataSpinner = (Spinner) rootView.findViewById(R.id.data_spinner);
        initData();
        bindCheckBox = (CheckBox) rootView.findViewById(R.id.bind_checkbox);
        bindCheckBox.setChecked(settingsData.bind);
        bindCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settingsData.bind = isChecked;
                SettingsHelper.setBindState(getActivity(), isChecked);
                sendCode(BIND_CHANGE);
            }
        });
        routesCheckBox = (CheckBox) rootView.findViewById(R.id.routes_checkbox);
        routesCheckBox.setChecked(settingsData.routes);
        routesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settingsData.routes = isChecked;
                SettingsHelper.setRoutesState(getActivity(), isChecked);
                sendCode(ROUTES_CHANGE);
            }
        });
        zonesCheckBox = (CheckBox) rootView.findViewById(R.id.zones_checkbox);
        zonesCheckBox.setChecked(settingsData.highlight);
        zonesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsHelper.setZones(getActivity(), isChecked);
                settingsData.highlight = isChecked;
                sendCode(HIGHLIGHT_CHANGE);
            }
        });
        updateCheckBox = (CheckBox) rootView.findViewById(R.id.update_checkbox);
        updateCheckBox.setChecked(settingsData.update);
        updateCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsHelper.setUpdate(getActivity(), isChecked);
                sendCode(UPDATE_CHANGE);
            }
        });
        rootView.findViewById(R.id.reset_push).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCode(RESET_PUSH);
            }
        });
        rootView.findViewById(R.id.reset_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.warning)
                        .setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendCode(RESET_DATA);
                            }
                        })
                        .setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.create().show();
                SettingsDialog.this.dismiss();
            }
        });
        rootView.findViewById(R.id.test_push).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCode(TEST_PUSH);
                SettingsDialog.this.dismiss();
            }
        });

        newAppId = (EditText) rootView.findViewById(R.id.app_id);

        final AppCompatButton button = (AppCompatButton) rootView.findViewById(R.id.create_app);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newAppId.getVisibility() == View.GONE) {
                    newAppId.setVisibility(View.VISIBLE);
                    button.setText(R.string.create);
                }else if(newAppId.getVisibility() == View.VISIBLE){
                    if(!newAppId.getText().toString().trim().isEmpty()){
                        String appId = newAppId.getText().toString();
                        String root = SettingsHelper.getRootPath();
                        String fullPath = root + "/" + appId;
                        DataManager.copyAssetFolder(getActivity().getAssets(), DataManager.getDefaultApplicationId(getActivity().getAssets()), fullPath, getActivity());
                        MapDataManager.removeMapSettings(fullPath);
                        MapDataManager.removeCanvas(fullPath);
                        SettingsHelper.setApplicationId(getActivity(), appId);
                        SettingsHelper.setPath(getActivity(), fullPath, appId);
                        settingsData.ignoreUpdateFlag = true;
                        sendCode(UPDATE_CHANGE);
                        newAppId.clearFocus();
                        dismiss();
                    }else{
                        Toast.makeText(getActivity(), R.string.create_app_message, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void sendCode(int code){
        if(listener != null){
            listener.settingsChanged(code, settingsData);
        }
    }

    private void initServers() {
        ArrayAdapter<Server> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, servers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverSpinner.setAdapter(adapter);
        serverSpinner.setSelection(adapter.getPosition(settingsData.server));
        serverSpinner.post(new Runnable() {
            @Override
            public void run() {
                serverSpinner.setOnItemSelectedListener(serverListener);
            }
        });
    }
    private void initSort() {
        ArrayAdapter<IBZoneManager.ZoneSortRule> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, IBZoneManager.ZoneSortRule.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setSelection(adapter.getPosition(settingsData.zoneSortRule));
        sortSpinner.post(new Runnable() {
            @Override
            public void run() {
                sortSpinner.setOnItemSelectedListener(sortListener);
            }
        });
    }

    private void initData(){
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = baseDir + "/ibecom";
        File file = new File(path);
        if(file.exists()){
            dirs = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    if(dir.isDirectory()){
                        return true;
                    }else {
                        return false;
                    }
                }
            });
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dirs);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dataSpinner.setAdapter(adapter);
            dataSpinner.setSelection(adapter.getPosition(SettingsHelper.getApplicationId(getActivity())));
            dataSpinner.post(new Runnable() {
                @Override
                public void run() {
                    dataSpinner.setOnItemSelectedListener(dataListener);
                }
            });
        }
    }

    private AdapterView.OnItemSelectedListener sortListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(listener != null){
                settingsData.zoneSortRule = IBZoneManager.ZoneSortRule.values()[position];
                SettingsHelper.setZoneSortRule(view.getContext(), settingsData.zoneSortRule);
                sendCode(SORT_CHANGE);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener serverListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(listener != null){
                settingsData.server = servers[position];
                SettingsHelper.setServer(view.getContext(), servers[position]);
                sendCode(SERVER_CHANGE);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener dataListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            String path = baseDir + "/ibecom/";
            String dir = dirs[position];
            String fullPath = path + dir;
            SettingsHelper.setApplicationId(getActivity(), dir);
            SettingsHelper.setPath(getActivity(), fullPath, dir);
            sendCode(DATA_DIR_CHANGE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };



    public interface SettingsListener{
        void settingsChanged(int code, SettingsData data);

    }

    public static class SettingsData{
        public boolean bind;
        public boolean routes;
        public IBZoneManager.ZoneSortRule zoneSortRule;
        public Server server;
        public boolean highlight;
        public boolean update;
        public boolean ignoreUpdateFlag;
    }
    public void setListener(SettingsListener listener) {
        this.listener = listener;
    }
}

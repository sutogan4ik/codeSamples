package ru.ibecom.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.ibecom.androidmap.MainActivity;
import ru.ibecom.androidmap.R;

/**
 * Created by Prog on 14.05.2015.
 */
public class InitActivity extends Activity {
    private boolean isPush;
    private String message;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        if(getIntent() != null){
            if(getIntent().getExtras() != null){
                isPush = getIntent().getExtras().getBoolean("isPush");
                message = getIntent().getExtras().getString("message");
            }
        }
        if(!DataManager.isDir(SettingsHelper.getDefaultPath(this))){
            DataManager.copyAssetFolder(getAssets(), DataManager.getDefaultApplicationId(getAssets()), SettingsHelper.getDefaultPath(this), this);
        }
        String path = SettingsHelper.getPath(this);
        if(DataManager.isDir(path)){
            startApp();
        }else {
            if (DataManager.copyAssetFolder(getAssets(), SettingsHelper.getApplicationId(this), path, this)){
                startApp();
            }else{
                Toast.makeText(this, R.string.copy_error, Toast.LENGTH_SHORT).show();
            }
        }


    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(getIntent() != null){
            if(getIntent().getExtras() != null){
                isPush = getIntent().getExtras().getBoolean("isPush");
                message = getIntent().getExtras().getString("message");
            }
        }
        super.onNewIntent(intent);
    }



    private void startApp(){
        if(isInternet(this)){
            update();
        }else {
            startAct();
        }
    }

    private void update(){
        final Handler handler = new Handler();
        if(SettingsHelper.isUpdate(this)) {
            RetrofitExecutor executor = new RetrofitExecutor(SettingsHelper.getServer(this).getEndPoint(), SettingsHelper.getApplicationId(this));
            executor.execute(SettingsHelper.getPath(this), new RetrofitExecutor.UpdateListener() {
                @Override
                public void updateComplete() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InitActivity.this, R.string.update_complete, Toast.LENGTH_SHORT).show();
                            startAct();
                        }
                    });
                }

                @Override
                public void someWrong() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InitActivity.this, R.string.update_error, Toast.LENGTH_SHORT).show();
                            startAct();
                        }
                    });
                }
            });
        }else{
            startAct();
        }
    }

    private void startAct(){
        final Intent intent = new Intent(InitActivity.this, MainActivity.class);
        if(isPush){
            intent.putExtra("message", message);
            intent.putExtra("isPush", isPush);
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    public boolean isInternet(Context context) {
        ConnectivityManager IM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = IM.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

}

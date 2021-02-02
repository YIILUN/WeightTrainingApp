package com.bestlab.weighttraining;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    Button btnSet,btnStart,btnResult;
    public static String CSV_FILE_PATH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initial();
    }

    private void initial() {
        CSV_FILE_PATH = MainActivity.this.getExternalFilesDir(null) + File.separator + "Download" + File.separator;
        btnSet = (Button)findViewById(R.id.btnSet);
        btnStart = (Button)findViewById(R.id.btnStart);
        btnResult = (Button)findViewById(R.id.btnResult);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //請先輸入身分再開始訓練
                File file = new File(MainActivity.CSV_FILE_PATH + "identityHost.csv");
                if (!file.exists()){
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Please enter your identity before training!")
                            .show();
                }else {
                    final String[] mode = {"Single", "Dual"};
                    AlertDialog.Builder dialog_list = new AlertDialog.Builder(MainActivity.this);
                    dialog_list.setTitle("Mode");
                    dialog_list.setItems(mode, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mode[which] == "Single") { //單人模式
                                Intent intent = new Intent();
                                intent.setClass(MainActivity.this, Single.class);
                                startActivity(intent);
                            } else if (mode[which] == "Dual") { //雙人模式
                                //輸入第二使用者資料
                                Intent intentSetGuestIdentity = new Intent();
                                intentSetGuestIdentity.setClass(MainActivity.this, SetGuestIdentity.class);
                                startActivity(intentSetGuestIdentity);
                            }
                        }
                    });
                    dialog_list.show();
                }
            }
        });

        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SetHostIdentity.class);
                startActivity(intent);
            }
        });

        btnResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,History.class);
                startActivity(intent);
            }
        });

    }

}
package com.bestlab.weighttraining;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ScanQRcode extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //提醒使用者掃描QRcode
        Toast.makeText(ScanQRcode.this, "Please Scan the QRcode on Training Machine", Toast.LENGTH_LONG).show();
        //開始掃描QRCode
        scan();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        //掃描結果
        String resultString;
        final IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(result == null){
            super.onActivityResult(requestCode,resultCode,data);
        }else {
            resultString = result.getContents();
            if(resultString == null){
                Toast.makeText(this, "scan failed", Toast.LENGTH_SHORT).show();
                finish();
            }else {
                Intent intent = new Intent();
                intent.putExtra("returnString", resultString);
                setResult(RESULT_OK, intent);
                finish();
            }
        }

    }

    private void scan() {
        //掃描設定
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureAct.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scanning Code");
        integrator.initiateScan();
    }
}

package com.bestlab.weighttraining;

import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.AlteredCharSequence;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedWriter;
import java.io.IOException;

public class InitialMachine extends AppCompatActivity {
    EditText etMinWeight,etMaxWeight,etEachWeight,etTOFHeight;
    Button btnOK;
    RadioGroup frequency,pose;
    String poseString,frequencyString;
    int min,max,each,height;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
        frequency = (RadioGroup)findViewById(R.id.frequency);
        pose = (RadioGroup)findViewById(R.id.pose);
        etMinWeight = (EditText)findViewById(R.id.etMinWeight);
        etMaxWeight = (EditText)findViewById(R.id.etMaxWeight);
        etEachWeight = (EditText)findViewById(R.id.etEachWeight);
        etTOFHeight = (EditText)findViewById(R.id.etTOFHeight);

        btnOK = (Button)findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String errorString = "";
                if(isInteger(etMinWeight.getText().toString()) == false){
                    errorString += "最輕重量 ";
                }
                if(isInteger(etMaxWeight.getText().toString()) == false){
                    errorString += "最重重量 ";
                }
                if(isInteger(etEachWeight.getText().toString()) == false){
                    errorString += "每塊重量 ";
                }
                if(isInteger(etTOFHeight.getText().toString()) == false){
                    errorString += "TOF高度 ";
                }

                if(errorString != ""){
                    Toast.makeText(InitialMachine.this, errorString + "輸入型態錯誤", Toast.LENGTH_SHORT).show();
                    Log.d("errorString",errorString);
                }else{
                    min = Integer.valueOf(etMinWeight.getText().toString());
                    max = Integer.valueOf(etMaxWeight.getText().toString());
                    each = Integer.valueOf(etEachWeight.getText().toString());
                    errorString = String.valueOf((max - min)/each);
                    if((max - min)/each > 0 && isInteger(errorString)){
                        GlobalVariable gv = (GlobalVariable)getApplicationContext();
                        gv.setMinWeight(Integer.parseInt(etMinWeight.getText().toString().trim()));
                        gv.setMaxWeight(Integer.parseInt(etMaxWeight.getText().toString().trim()));
                        gv.setEachWeight(Integer.parseInt(etEachWeight.getText().toString().trim()));
                        gv.setTOFHeight(Integer.parseInt(etTOFHeight.getText().toString().trim()));
                        RadioButton selectFrequency = (RadioButton)findViewById(frequency.getCheckedRadioButtonId());
                        frequencyString = selectFrequency.getText().toString();
                        gv.setFrequency(frequencyString);
                        RadioButton selectPose = (RadioButton)findViewById(pose.getCheckedRadioButtonId());
                        poseString = selectPose.getText().toString();
                        gv.setPose(poseString);
                        finish();
                    }else{
                        Toast.makeText(InitialMachine.this,"重量輸入錯誤",Toast.LENGTH_SHORT).show();
                    }

                }
            }

            private boolean isInteger(String s) {
                try {
                    Integer.parseInt(s);
                } catch(NumberFormatException e) {
                    return false;
                } catch(NullPointerException e) {
                    return false;
                }
                if(Integer.valueOf(s) > 0) {
                    return true;
                }else{
                    return false;
                }
            }
        });


    }
}

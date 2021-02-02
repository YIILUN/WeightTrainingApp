package com.bestlab.weighttraining;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.IOException;

public class SetGuestIdentity extends AppCompatActivity {
    EditText etName,etHeight,etWeight,etAge;
    Button btnSave;
    RadioGroup Gender;
    public String name,gender;
    public int height,weight,age;
    BufferedWriter bw;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        etName = (EditText)findViewById(R.id.etName);
        etHeight = (EditText)findViewById(R.id.etHeight);
        etWeight = (EditText)findViewById(R.id.etWeight);
        etAge = (EditText)findViewById(R.id.etAge);
        Gender = (RadioGroup)findViewById(R.id.Gender);
        btnSave = (Button)findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //若未輸入完整資訊顯示警告
                String warning = "";
                if(etName.getText().toString().matches("")){
                    warning += " name";
                }
                if(isInteger(etHeight.getText().toString()) == false){
                    warning += " height";
                }
                if(isInteger(etWeight.getText().toString()) == false){
                    warning += " weight";
                }
                if(isInteger(etAge.getText().toString()) == false){
                    warning += " age";
                }
                if(Gender.getCheckedRadioButtonId() == -1){
                    warning += " gender";
                }
                //輸入完整則儲存變數
                if (warning == ""){
                    name = etName.getText().toString();
                    height = Integer.valueOf(etHeight.getText().toString());
                    weight = Integer.valueOf(etWeight.getText().toString());
                    age = Integer.valueOf(etAge.getText().toString());
                    RadioButton selectGender = (RadioButton)findViewById(Gender.getCheckedRadioButtonId());
                    gender = selectGender.getText().toString();

                    //將個人資料儲存成檔案方便模式讀取
                    try {
                        bw = CsvWriter.createBW("identityGuest.csv");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        bw.write("name,height,weight,age,gender");
                        bw.newLine();
                        bw.write(name+","+height+","+weight+","+age+","+gender);
                        bw.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    CsvWriter.saveCsv(bw);
                    finish();

                    //跳至雙人模式
                    Intent intent = new Intent();
                    intent.setClass(SetGuestIdentity.this, Dual.class);
                    startActivity(intent);

                }else{
                    Toast.makeText(SetGuestIdentity.this, warning+"型態錯誤!", Toast.LENGTH_LONG).show();
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

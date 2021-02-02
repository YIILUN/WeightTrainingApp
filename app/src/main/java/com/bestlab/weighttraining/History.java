package com.bestlab.weighttraining;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class History extends AppCompatActivity {
    public SQLdata DH = null;
    public SQLiteDatabase db;
    public ListView LV;
    EditText editWeight,editTimes;
    Button btnDelete,btnUpdate,btnCSV;
    String id_text = null;
    BufferedWriter bw;
    Integer sum_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        DH = new SQLdata(this,null,null,1);
        db = DH.getWritableDatabase();//載入時就要先執行讀取資料
        LV = (ListView)findViewById(R.id.LV);//讀取元件
        select();
        editWeight = (EditText)findViewById(R.id.editWeight);
        editTimes = (EditText)findViewById(R.id.editTimes);

        btnDelete = (Button)findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                del(id_text);
                select();
                id_text = null;
            }
        });

        btnUpdate = (Button)findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(id_text==null){
                    Toast.makeText(History.this, "請先選擇欲更新資料", Toast.LENGTH_SHORT).show();
                }else {
                    update(id_text,Integer.valueOf(editWeight.getText().toString()), Integer.valueOf(editTimes.getText().toString()));
                    select();
                }
            }
        });

        btnCSV = (Button)findViewById(R.id.btnCSV);
        btnCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                export();
                Toast.makeText(History.this, "儲存檔案", Toast.LENGTH_SHORT).show();
            }
        });

        LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView txvID = (TextView)view.findViewById(R.id.txvID);
                id_text = txvID.getText().toString();
                TextView txvWeight = (TextView)view.findViewById(R.id.txvWeight);
                editWeight.setText(txvWeight.getText());
                TextView txvTimes = (TextView)view.findViewById(R.id.txvTimes);
                editTimes.setText(txvTimes.getText());
            }
        });
    }

    private void export() {
        //將所有資料儲存成檔案
        try {
            bw = CsvWriter.createBW("History.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bw.write("id,date,nowtime,name,pose,group,times,weight,mac,distanceIn,timeIn,powerIn,distanceOut,timeOut,powerOut");
            bw.newLine();
            Cursor c = db.rawQuery("SELECT * FROM TBHistory",null);
            if(c.getCount() > 0){
                String str = "";
                c.moveToFirst();
                do{
                    str = (c.getString(0)+","+c.getString(1)+","+c.getString(2)+","+c.getString(3)+","+c.getString(4)+","+c.getInt(5)+","+c.getInt(6)+","+c.getInt(7)+","+c.getString(8)+","+c.getDouble(9)+","+c.getDouble(10)+","+c.getDouble(11)+","+c.getDouble(12)+","+c.getDouble(13)+","+c.getDouble(14));
                    bw.write(str);
                    bw.newLine();
                }while(c.moveToNext());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        CsvWriter.saveCsv(bw);
    }

    private  void update(String id,Integer weight,Integer times){
        ContentValues values = new ContentValues();
        values.put("_weight",weight.toString());
        values.put("_times",times.toString());
        db.update("TBHistory",values,"_id="+id,null);
    }

    private void del(String id){
        db.delete("TBHistory","_id="+id,null);
        String up_del="update TBHistory set _id=_id-1 where _id>"+id;
        db.execSQL(up_del);
    }


    private void select() {
        //查詢資料庫並載入
        Cursor cursor = db.query("TBHistory", new String[]{"_id","_date","_nowTime","_name","_pose","_group","_times","_weight","_mac", "_distanceIn","_timeIn","_powerIn","_distanceOut","_timeOut","_powerOut"}, null,null,null,null,null);
        List<Map<String,Object>> items = new ArrayList<Map<String,Object>>();
        cursor.moveToFirst();
        sum_id = cursor.getCount();

        //叫出資料庫的資料
        for(int i = 0; i<cursor.getCount();i++){
            Map<String,Object> item = new HashMap<String,Object>();
            item.put("_id",cursor.getString(0));
            item.put("_date",cursor.getString(1));
            item.put("_nowTime",cursor.getString(2));
            item.put("_name",cursor.getString(3));
            item.put("_pose",cursor.getString(4));
            item.put("_group",cursor.getInt(5));
            item.put("_times",cursor.getInt(6));
            item.put("_weight",cursor.getInt(7));
            item.put("_mac",cursor.getString(8));
            item.put("_distanceIn",cursor.getDouble(9));
            item.put("_timeIn",cursor.getDouble(10));
            item.put("_powerIn",cursor.getDouble(11));
            item.put("_distanceOut",cursor.getDouble(12));
            item.put("_timeOut",cursor.getDouble(13));
            item.put("_powerOut",cursor.getDouble(14));
            items.add(item);//新增
            cursor.moveToNext();//移到下一筆資料
        }
        SimpleAdapter sa = new SimpleAdapter(this,items,R.layout.list_text,
                new String[]{"_id","_date","_nowTime","_name","_pose","_group","_times","_weight","_mac","_distanceIn","_timeIn","_powerIn","_distanceOut","_timeOut","_powerOut"},
                new int[]{R.id.txvID,R.id.txvDate,R.id.txvTime,R.id.txvName,R.id.txvPose,R.id.txvGroup,R.id.txvTimes,R.id.txvWeight,R.id.txvMac,R.id.txvDistanceIn,R.id.txvTimeIn,R.id.txvPowerIn,R.id.txvDistanceOut,R.id.txvTimeOut,R.id.txvPowerOut}){};
        LV.setAdapter(sa);
    }
}

package com.bestlab.weighttraining;

import android.util.Log;

public class Dataformat {

    public String[] data = new String[20];
    String temp;
    int result,count,countnew;

    public Dataformat(byte[] packages) {
        for (int i = 0; i < packages.length; i++) {
            temp = Integer.toHexString(packages[i]);
            data[i] = temp;
            if(temp.equals("3a")){
                count = i;
            }
        }

        Log.d("warning",String.valueOf(count));
        if(count + 1 < 20) {
            for (int i = count + 1; i <= count + 4; i++) {
                result += (Integer.valueOf(data[i]) % 10 *((int) Math.pow(10, count + 4 - i)));
            }
            Log.d("warning","正確");
        }else{
            String datashow = "";
            for (int i = 0; i < packages.length; i++) {
                datashow += data[i];
            }
            Log.d("warning", datashow);
            count = 99;
            Log.d("warning", "錯誤");
        }
    }
}
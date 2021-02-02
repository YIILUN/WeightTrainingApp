package com.bestlab.weighttraining;

import android.app.Application;

public class GlobalVariable extends Application {
    private int minWeight,maxWeight,eachWeight,TOFheight;
    private String frequency,pose;

    //修改 變數値
    //Machine parameter

    public void setMinWeight(int minWeight){
        this.minWeight = minWeight;
    }
    public void setMaxWeight(int maxWeight){
        this.maxWeight = maxWeight;
    }
    public void setEachWeight(int eachWeight){
        this.eachWeight = eachWeight;
    }
    public void setPose(String pose){
        this.pose = pose;
    }
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    public void setTOFHeight(int TOFheight) {
        this.TOFheight = TOFheight;
    }

    //取得 變數值
    //Machine parameter
    public int getMinWeight() {
        return minWeight;
    }
    public int getMaxWeight() {
        return maxWeight;
    }
    public int getEachWeight() {
        return eachWeight;
    }
    public String getPose(){
        return pose;
    }
    public String getFrequency(){
        return frequency;
    }
    public int getTOFHeight(){
        return TOFheight;
    }

}

package com.example.jiaqili.cropphototest;

/**
 * Created by Jiaqi Li on 2018/3/23.
 */

public class MyArray {
    public int width,height;
    public float[][] array;
    public void create(int Height,int Width) {
        this.width=Width;
        this.height=Height;
        this.array=new float[height][width];//用两个变量来实现动态定义了
    }
}

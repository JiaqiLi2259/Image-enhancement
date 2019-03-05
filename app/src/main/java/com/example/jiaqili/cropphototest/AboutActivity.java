package com.example.jiaqili.cropphototest;

import android.content.Intent;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {
    private TextView textView1,textView2,textView3,textView4,textView5,textView6,
            textView7,textView8,textView9,textView10,textView11,textView12;
    private String data1="1、应用介绍";
    private String data2="REPIC是一款可以本地使用的图像增强软件，是recreate picture的缩写，体现了本应用在图像处理上强大的功能。APP使用的是Lightness-aware Contrast Enhancement for Images with Different Illumination Conditions这篇论文上的算法。";
    private String data3="特别感谢郝老师和姜同学对APP的大力支持!若对APP有任何建议，欢迎发邮件告诉我，邮箱：956004067@qq.com" ;
    private String data4=" ";
    private String data5="2、应用功能";
    private String data6="①支持通过手机摄像头获取图像并根据需要裁剪图像尺寸";
    private String data7="②支持通过系统相册获取图像并根据需要裁剪图像尺寸";
    private String data8="③增强后的照片可以保存至本地";
    private String data9="④菜单栏中新增“关于应用”和“退出应用”两个选项";
    private String data10=" ";
    private String data11="3、手机要求";
    private String data12="本应用在Android 8.1上做了充分测试，并且向下兼容到Android 4.1，手机至少有4G运行内存。" +
            "本应用已在华为、小米、一加、OPPO、Vivo等机型上测试成功。";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initializeWidget();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_about,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int i=item.getItemId();
        switch (i){
            case R.id.itemExit_AboutActivity:
                ActivityCollector.finishAll();
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
            default:
                break;
        }
        return true;
    }
    public void initializeWidget(){
        textView1=(TextView) findViewById(R.id.txt1);
        textView2=(TextView) findViewById(R.id.txt2);
        textView3=(TextView) findViewById(R.id.txt3);
        textView4=(TextView) findViewById(R.id.txt4);
        textView5=(TextView) findViewById(R.id.txt5);
        textView6=(TextView) findViewById(R.id.txt6);
        textView7=(TextView) findViewById(R.id.txt7);
        textView8=(TextView) findViewById(R.id.txt8);
        textView9=(TextView) findViewById(R.id.txt9);
        textView10=(TextView) findViewById(R.id.txt10);
        textView11=(TextView) findViewById(R.id.txt11);
        textView12=(TextView) findViewById(R.id.txt12);
        textView1.setText(data1);
        textView2.setText("       "+data2);
        textView3.setText("       "+data3);
        textView4.setText("       "+data4);
        textView5.setText(data5);
        textView6.setText("   "+data6);
        textView7.setText("   "+data7);
        textView8.setText("           "+data8);
        textView9.setText("       "+data9);
        textView10.setText(data10);
        textView11.setText(data11);
        textView12.setText("       "+data12);

        textView6.getViewTreeObserver().addOnGlobalLayoutListener(new OnTvGlobalLayoutListener());
        textView7.getViewTreeObserver().addOnGlobalLayoutListener(new OnTvGlobalLayoutListener());
        textView8.getViewTreeObserver().addOnGlobalLayoutListener(new OnTvGlobalLayoutListener());
        textView9.getViewTreeObserver().addOnGlobalLayoutListener(new OnTvGlobalLayoutListener());
    }
    private class OnTvGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            textView6.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            final String newText6 = autoSplitText(textView6,"、、");
            if (!TextUtils.isEmpty(newText6)) {
                textView6.setText(newText6);
            }
            textView7.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            final String newText7 = autoSplitText(textView7,"、、");
            if (!TextUtils.isEmpty(newText7)) {
                textView7.setText(newText7);
            }
            textView8.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            final String newText8 = autoSplitText(textView8,"、、");
            if (!TextUtils.isEmpty(newText8)) {
                textView8.setText(newText8);
            }
            textView9.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            final String newText9 = autoSplitText(textView9,"、");
            if (!TextUtils.isEmpty(newText9)) {
                textView9.setText(newText9);
            }
        }
    }
    private String autoSplitText(final TextView tv, final String indent) {
        final String rawText = tv.getText().toString(); //原始文本
        final Paint tvPaint = tv.getPaint(); //paint，包含字体等信息
        final float tvWidth = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight(); //控件可用宽度
        //将缩进处理成空格
        String indentSpace = "";
        float indentWidth = 0;
        if (!TextUtils.isEmpty(indent)) {
            float rawIndentWidth = tvPaint.measureText(indent);
            if (rawIndentWidth < tvWidth) {
                while ((indentWidth = tvPaint.measureText(indentSpace)) < rawIndentWidth) {
                    indentSpace += " ";
                }
            }
        }
        //将原始文本按行拆分
        String [] rawTextLines = rawText.replaceAll("\r", "").split("\n");
        StringBuilder sbNewText = new StringBuilder();
        for (String rawTextLine : rawTextLines) {
            if (tvPaint.measureText(rawTextLine) <= tvWidth) {
                //如果整行宽度在控件可用宽度之内，就不处理了
                sbNewText.append(rawTextLine);
            } else {
                //如果整行宽度超过控件可用宽度，则按字符测量，在超过可用宽度的前一个字符处手动换行
                float lineWidth = 0;
                for (int cnt = 0; cnt != rawTextLine.length(); ++cnt) {
                    char ch = rawTextLine.charAt(cnt);
                    //若从手动换行的第二行开始加上悬挂缩进，条件应该为(lineWidth < 0.1f && cnt != 0)
                    if (lineWidth < 0.1f ) {
                        sbNewText.append(indentSpace);
                        lineWidth += indentWidth;
                    }
                    lineWidth += tvPaint.measureText(String.valueOf(ch));
                    if (lineWidth <= tvWidth) {
                        sbNewText.append(ch);
                    } else {
                        sbNewText.append("\n");
                        lineWidth = 0;
                        --cnt;
                    }
                }
            }
            sbNewText.append("\n");
        }
        //把结尾多余的\n去掉
        if (!rawText.endsWith("\n")) {
            sbNewText.deleteCharAt(sbNewText.length() - 1);
        }
        return sbNewText.toString();
    }
}

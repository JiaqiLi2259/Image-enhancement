package com.example.jiaqili.cropphototest;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private static final int UPDATE=7;
    //private static final int TAKE_PHOTO=0;
    private static final int TAKE_PHOTO_NEW=1;
    private static final int CHOOSE_PHOTO=2;
    private static final int CROP_PHOTO=3;
    private static final int CAMERA_REQUEST_CODE=4;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE=5;
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE=6;
    private ImageView imageView_output,imageView_input;
    private ProgressDialog pd;
    private Uri imageUri;
    private static File file= null;//处理前照片的文件
    private static File file_output=null;//处理后照片的文件
    private static Bitmap bitMap=null;//处理前的照片
    private static Bitmap bm=null;//处理后的照片
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE:
                    pd.dismiss();// 关闭ProgressDialog
                    imageView_output.setImageBitmap(bm);//展示处理后的图片
                    Toast.makeText(getApplicationContext(),"图像处理成功!",Toast.LENGTH_SHORT).show();//提示成功
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView_input=(ImageView)findViewById(R.id.inputImage);
        imageView_output=(ImageView)findViewById(R.id.outputImage);
        imageView_input.setImageDrawable(getResources().getDrawable(R.drawable.inputbg));
        imageView_output.setImageDrawable(getResources().getDrawable(R.drawable.outputbg));
        initializeRuntimePermissions();
        Log.d("MainActivity","Hello World!");
        measure();
    }
    private void  initializeRuntimePermissions(){
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }
    }
    private void measure(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Log.d("MainActivity", "屏幕高:" + dm.heightPixels);

        //应用区域
        Rect outRect1 = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect1);
        Log.d("MainActivity", "应用区顶部" + outRect1.top);
        Log.d("MainActivity", "应用区高" + outRect1.height());

        //View绘制区域
        Rect outRect2 = new Rect();
        getWindow().findViewById(Window.ID_ANDROID_CONTENT).getDrawingRect(outRect2);
        Log.d("MainActivity", "View绘制区域顶部-错误方法：" + outRect2.top);   //不能像上边一样由outRect2.top获取，这种方式获得的top是0，可能是bug吧
        int viewTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();   //要用这种方法
        Log.d("MainActivity", "View绘制区域顶部-正确方法：" + viewTop);
        Log.d("MainActivity", "View绘制区域高度：" + outRect2.height());
        /**
         * 获取标题栏高度-方法1
         * 标题栏高度 = View绘制区顶端位置 - 应用区顶端位置(也可以是状态栏高度，获取状态栏高度方法3中说过了)
         * */
        int titleHeight1 = viewTop - outRect1.top;
        Log.d("MainActivity", "标题栏高度-方法1：" + titleHeight1);
        int titleHeight2 = outRect1.height() - outRect2.height();
        Log.e("MainActivity", "标题栏高度-方法2：" + titleHeight2);

        Rect frame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        int contentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        //statusBarHeight是上面所求的状态栏的高度
        int titleBarHeight = contentTop - statusBarHeight;
        Log.e("MainActivity", "标题栏高度-方法3：" + titleBarHeight);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int i=item.getItemId();
        switch (i){
            case R.id.itemAbout:
                Intent intentAbout=new Intent(MainActivity.this,AboutActivity.class);
                startActivity(intentAbout);
                break;
            case R.id.itemExit:
                ActivityCollector.finishAll();
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
            default:
                break;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                //判断用户对启动相机的授权结果
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"您授权了相机的权限！",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this,"您拒绝授权，可能会导致程序崩溃！",Toast.LENGTH_LONG).show();
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST_CODE);
                    }
                }
                break;
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                //判断用户对存储器写操作的授权结果
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"您授权了对存储器写的权限！",Toast.LENGTH_SHORT).show();
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST_CODE);
                    }
                }
                else {
                    Toast.makeText(this,"您拒绝授权，可能会导致程序崩溃！",Toast.LENGTH_LONG).show();
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                    }
                }
                break;
            case READ_EXTERNAL_STORAGE_REQUEST_CODE:
                //判断用户对存储器读操作的授权结果
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"您授权了对存储器读的权限！",Toast.LENGTH_SHORT).show();
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                    }
                }
                else{
                    Toast.makeText(this,"您拒绝授权，可能会导致程序崩溃！",Toast.LENGTH_LONG).show();
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},READ_EXTERNAL_STORAGE_REQUEST_CODE);
                    }
                }
                break;
            default:
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode==RESULT_OK){  //检查相机能否正常返回数据
            switch (requestCode){  //检查相机的启动是否因按下“拍照”按钮
                //case TAKE_PHOTO:
                    //Bundle bundle = data.getExtras();
                    //bitMap = (Bitmap) bundle.get("data");  // 获取相机返回的数据，并转换为Bitmap图片格式
                    //String state = Environment.getExternalStorageState();
                    //if (state.equals(Environment.MEDIA_MOUNTED)){ // 检查SD卡是否可用
                            //saveImageToLocalPhone(this, bitMap);//将照片保存到本地
                            //更新图库图片,使得图片对用户可见
                            //MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, null);
                            //cropPhotoFromCamera();//启用裁剪图片程序
                          //}
                    //else{
                        //Toast.makeText(MainActivity.this,"请确认已插入SD卡！",Toast.LENGTH_LONG).show();
                    //}
                    //break;
                case TAKE_PHOTO_NEW:
                    if (file.exists()){
                        cropPhotoFromCamera();//启动裁剪图片程序
                    }
                    else{
                        Toast.makeText(MainActivity.this,"照片拍摄失败！",Toast.LENGTH_LONG).show();
                    }
                    break;
                case CHOOSE_PHOTO:
                    if (data!=null){
                        cropPhotoFromAlbum(data);
                    }
                    else {
                        Toast.makeText(MainActivity.this,"选择照片失败！",Toast.LENGTH_LONG).show();
                    }
                    break;
                case CROP_PHOTO:
                    try {
                         bitMap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        if (bitMap!=null) {
                            imageView_input.setImageBitmap(bitMap);
                        }
                        //更新图库图片,使得图片对用户可见
                        MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, null);
                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
        else{
            Toast.makeText(MainActivity.this,"无法获得数据！",Toast.LENGTH_LONG).show();
        }
    }
    public void btnTakePhotoClick(View view){
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST_CODE);
        }
        else{
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
            else{
                openCamera_new();
            }
        }
    }
    public void btnChoosePhotoClick(View view){
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST_CODE);
        }
        else{
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
            else{
                openAlbum();
            }
        }
    }
    public void btnImageFilterClick(View view){
        if (bitMap!=null){
            pd = ProgressDialog.show(MainActivity.this, "提示", "图像处理中，请稍后…");
            BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
            bfoOptions.inScaled = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bm=imageFilterAlgorithm(bitMap);// 耗时的方法
                    Message message = new Message();
                    message.what = UPDATE;
                    handler.sendMessage(message);// 执行耗时的方法之后发送消给handler
                }

            }).start();
            //bm=imageFilterAlgorithm(bitMap);
            //imageView_output.setImageBitmap(bm);
            //Toast.makeText(getApplicationContext(),"图像处理成功!",Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(MainActivity.this,"请拍照或选择照片！",Toast.LENGTH_LONG).show();
        }
    }
    public void btnSaveImageClick(View view){
        if (bm!=null){
            String state = Environment.getExternalStorageState();
            if (state.equals(Environment.MEDIA_MOUNTED)){//检查SD卡是否可用
                saveImageToLocalPhone(MainActivity.this, bm);//将照片保存到本地
                //更新图库图片,使得图片对用户可见
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{file_output.getAbsolutePath()}, null, null);
            }
            else {
                Toast.makeText(MainActivity.this,"请确认已插入SD卡！",Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(MainActivity.this,"未检测到增强后的图像！",Toast.LENGTH_LONG).show();
        }
    }
    public static void saveImageToLocalPhone(Context context, Bitmap bitmap){
        FileOutputStream fos = null;//写文件操作的输出流
        String str=null;//照片的文件名
        Date currentDate=null;//当下的时间
        File appDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/DCIM/Camera");
        if(!appDir.exists()){
            appDir.mkdirs();
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); //指定格式
        currentDate =new Date();   //获取当前日期
        str=dateFormatter.format(currentDate);  //通过format(Date date)方法将指定的日期对象格式化为指定格式的字符串.
        String fileName = "IMG_"+str+ ".jpg";  //加上.jpg后缀形成完整的照片文件名
        file_output = new File(appDir, fileName);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 75表示压缩质量，压缩范围0~100，100以最高画质压缩，并保存bitmap至本地
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            fos = new FileOutputStream(file_output);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            Toast.makeText(context,"图片另存为"+file_output.getAbsolutePath(),Toast.LENGTH_SHORT).show();
        }
    }
    private void openCamera(){
        //Intent intentTakePhoto=new Intent("android.media.action.IMAGE_CAPTURE");//调用摄像头拍照
        //startActivityForResult(intentTakePhoto,TAKE_PHOTO);
    }
    private void openAlbum(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }
    private void openCamera_new(){
        createFile();//创建保存照片的file文件
        createImageUri();//创建Uri，这里使用了FileProvider
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO_NEW);
    }
    private void cropPhotoFromCamera(){
        createImageUri();
        Intent intent = new Intent("com.android.camera.action.CROP");
        //对目标应用临时授权该Uri所代表的文件,这是对Android7.0及以后版本的适配
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(imageUri, "image/*");//设置执行时要操作的数据，并显式指定Intent的数据类型
        intent.putExtra("crop", "true");// crop=true 有这句才能出来最后的裁剪页面.
        intent.putExtra("scale", true);//处理黑边
        intent .putExtra("scaleUpIfNeeded", true);//处理黑边
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);//指定图片的输出地址
        startActivityForResult(intent, CROP_PHOTO); // 启动裁剪程序
    }
    private void cropPhotoFromAlbum(Intent data){
        ContentResolver resolver = getContentResolver();
        Uri uri=data.getData();//获得相册中选中图片的Uri
        String path=getFilePathFromContentUri(uri,resolver);//将图片的uri转换为绝对路径
        file = new File(path);//将绝对路径转换为file文件
        createImageUri();//再将file文件转换为Uri
        Intent intent = new Intent("com.android.camera.action.CROP");
        //对目标应用临时授权该Uri所代表的文件,这是对Android7.0及以后版本的适配
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(imageUri, "image/*");//设置执行时要操作的数据，并显式指定Intent的数据类型
        intent.putExtra("crop", "true");// crop=true 有这句才能出来最后的裁剪页面.
        intent.putExtra("scale", true);//处理黑边
        intent .putExtra("scaleUpIfNeeded", true);//处理黑边
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);//指定图片的输出地址
        startActivityForResult(intent, CROP_PHOTO); // 启动裁剪程序
    }
    private void createFile(){
        String str=null;//照片的文件名
        Date currentDate=null;//当下的时间
        File appDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/DCIM/Camera");
        if(!appDir.exists()){
            appDir.mkdirs();
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); //指定格式
        currentDate =new Date();   //获取当前日期
        str=dateFormatter.format(currentDate);  //通过format(Date date)方法将指定的日期对象格式化为指定格式的字符串.
        String fileName = "IMG_"+str+ ".jpg";  //加上.jpg后缀形成完整的照片文件名
        file = new File(appDir, fileName);
    }
    private void createImageUri(){
        if (Build.VERSION.SDK_INT>=24){
            imageUri=FileProvider.getUriForFile(MainActivity.this,"com.example.jiaqili.cropphototest.fileprovider",file);
        }
        else {
            imageUri=Uri.fromFile(file);
        }
    }
    public static String getFilePathFromContentUri(Uri selectedVideoUri, ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
//      也可用下面的方法拿到cursor
//      Cursor cursor = this.context.managedQuery(selectedVideoUri, filePathColumn, null, null, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }
    private Bitmap imageFilterAlgorithm(Bitmap bitmap){
        final int HEIGHT=bitmap.getHeight();
        final int WIDTH=bitmap.getWidth();
        final int R=(int) (Math.floor((HEIGHT<=WIDTH)?(WIDTH/5):(HEIGHT/5)));
        final double eps=Math.pow(10,-6);
        final float epsilon=0.3f;
        final float thresh=0.2f;
        final int r_smooth=(int) (Math.floor(0.1*((HEIGHT>=WIDTH)?HEIGHT:WIDTH)));
        final double eps_smooth=0.01;
        //1.创建位图对象
        Bitmap bitmapCopy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        //2.创建画笔对象
        Paint paint = new Paint();
        //3.创建画板对象，把白纸铺在画板上
        Canvas canvas = new Canvas(bitmapCopy);
        //4.开始作画，把原图的内容绘制在白纸上
        canvas.drawBitmap(bitmap, new Matrix(), paint);
        //5.算法处理开始
        float[] hsv=new float[3];//单个像素点的hsv向量
        float[][] v=new float[WIDTH][HEIGHT];//获取图像的亮度通道即v通道
        float[][] RGB_max=new float[WIDTH][HEIGHT];//获取GRB通道的最大值
        float[][] image_R=new float[WIDTH][HEIGHT];//获取原图的R通道
        float[][] image_G=new float[WIDTH][HEIGHT];//获取原图的G通道
        float[][] image_B=new float[WIDTH][HEIGHT];//获取原图的B通道
        float[][] image_mask=new float[WIDTH][HEIGHT];//获取考虑阈值后的图像值,即二值化
        float[][] image_mask_finish=new float[WIDTH][HEIGHT];//获取形态学开运算后的图像值
        float[][] image_mask_refined;//获取经过引导滤波后的Lr
        float[][] image_maxGuidedFilter;//获取平滑的光照分布Ts
        float[][] image_gray_recover=new float[WIDTH][HEIGHT];//获取中间增强结果Ie的v通道
        int[][] image_recover=new int[WIDTH][HEIGHT];//获取中间增强结果Ie的color值
        //int[][] image_output=new int[WIDTH][HEIGHT];//获取最终结果的color值
        for(int column=0;column<WIDTH;column++){
            for(int row=0;row<HEIGHT;row++){
                int color=bitmap.getPixel(column,row);
                Color.colorToHSV(color,hsv);
                v[column][row]=hsv[2];
                int max=compare(Color.red(color),Color.green(color),Color.blue(color));
                /******************尝试纠错************************/
                if(max>230) {max=max-20;}
                /**************************************************/
                RGB_max[column][row]=max/255f;
                image_R[column][row]=Color.red(color)/255f;
                image_G[column][row]=Color.green(color)/255f;
                image_B[column][row]=Color.blue(color)/255f;
                /*********************尝试纠错********************/
                if (Color.red(color)>225) {image_R[column][row]=(Color.red(color)-20)/255f;}
                if (Color.green(color)>225) {image_G[column][row]=(Color.green(color)-20)/255f;}
                if (Color.blue(color)>225) {image_B[column][row]=(Color.blue(color)-20)/255f;}
                /*************************************************/
                if (RGB_max[column][row]>thresh){
                    image_mask[column][row]=1.0f;
                }
                else {
                    image_mask[column][row]=0f;
                }
            }
        }
        float[][] image_mask_erode=morphologicalOpenOperation(image_mask,1,HEIGHT,WIDTH);
        float[][] image_mask_dilate=morphologicalOpenOperation(image_mask_erode,0,HEIGHT,WIDTH);
        for ( int column=0;column<WIDTH;column++){
            for (int row=0;row<HEIGHT;row++){
                image_mask_finish[column][row]=1-image_mask_dilate[column][row];
            }
        }
        image_mask_refined = guidedfilter(RGB_max, image_mask_finish, WIDTH, HEIGHT,R,eps);
        image_maxGuidedFilter = guidedfilter(RGB_max, RGB_max, WIDTH,HEIGHT,r_smooth, eps_smooth);
        for (int column=0;column<WIDTH;column++){
            for (int row=0;row<HEIGHT;row++){
                image_gray_recover[column][row] = v[column][row]/(image_maxGuidedFilter[column][row]+epsilon);
                //I_recover = hsv2rgb(cat(3,yhsv(:,:,1:2),y_gray_recover));
                int color=bitmap.getPixel(column,row);
                Color.colorToHSV(color,hsv);
                hsv[2]=image_gray_recover[column][row];
                image_recover[column][row]=Color.HSVToColor(hsv);
            }
        }
        //y_output = y_ori.*(1-repmat(y_mask_refined,[1,1,3]))+(I_recover).*repmat(y_mask_refined,[1,1,3]);
        for (int i=0;i<WIDTH;i++){
            for (int j=0;j<HEIGHT;j++){
                //int color=bitmap.getPixel(i,j);
                float red,green,blue;
                int Red,Green,Blue;
                red=(image_R[i][j])*(1-image_mask_refined[i][j])+(Color.red(image_recover[i][j])/255f)*image_mask_refined[i][j];
                Red=(int) (Math.floor(red*255f));
                green=(image_G[i][j])*(1-image_mask_refined[i][j])+(Color.green(image_recover[i][j])/255f)*image_mask_refined[i][j];
                Green=(int) (Math.floor(green*255f));
                blue=(image_B[i][j])*(1-image_mask_refined[i][j])+(Color.blue(image_recover[i][j])/255f)*image_mask_refined[i][j];
                Blue=(int) (Math.floor(blue*255f));
                bitmapCopy.setPixel(i, j, Color.rgb(Red, Green, Blue));
            }
        }
        return bitmapCopy;
    }
    public int compare(int r,int g,int b){
        int max;
        if (r>=g){
            max=r;
            if (b>=max){
                max=b;
            }
        }
        else{
            max=g;
            if (b>=max){
                max=b;
            }
        }
        return max;
    }
    public static float[][] morphologicalOpenOperation(float[][] origin,int flag,int H,int W){
        int mask[][]={{0,0,1,1,1,1,1,0,0},{0,1,1,1,1,1,1,1,0},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1},{0,1,1,1,1,1,1,1,0},{0,0,1,1,1,1,1,0,0}};
        float[][] result=new float[W][H];
        int k=0;
        int []row=new int[69];
        int []col=new int[69];
        for (int i=0;i<W;i++){
            for (int j=0;j<H;j++){
                result[i][j]=origin[i][j];
            }
        }
        for (int i=-4;i<=4;i++){
            for (int j=-4;j<=4;j++){
                if (mask[4+i][4+j]==1){
                    row[k]=i;
                    col[k]=j;
                    k++;
                }
            }
        }
        int total=k-1;
        if (flag==1){                                                       //此时为erode操作
            for (int I=4;I<=W-5;I++){
                for (int J=4;J<=H-5;J++){
                    for (k=1;k<=total;k++){
                        result[I][J]=1.0f;
                        if (origin[I+row[k]][J+col[k]]<0.5){
                            result[I][J]=0f;
                            break;
                        }
                    }
                }
            }
        }
        if (flag==0){                                                      //此时为dilate操作
            for (int I=4;I<=W-5;I++){
                for (int J=4;J<=H-5;J++){
                    for (k=1;k<=total;k++){
                        result[I][J]=0f;
                        if (origin[I+row[k]][J+col[k]]>0.5){
                            result[I][J]=1.0f;
                            break;
                        }
                    }
                }
            }
        }
        return  result;
    }
    public static float[][] guidedfilter(float[][] I,float[][] P,int HEIGHT,int WIDTH,int R,double eps){
        MyArray imSrc_I=new MyArray();
        MyArray imSrc_P=new MyArray();
        imSrc_I.create(HEIGHT, WIDTH);
        imSrc_P.create(HEIGHT, WIDTH);
        for (int i=0;i<HEIGHT;i++){                                   //初始化imSrc_I和imSrc_P
            for (int j=0;j<WIDTH;j++){
                imSrc_I.array[i][j]=I[i][j];
                imSrc_P.array[i][j]=P[i][j];
            }
        }
        MyArray Ones=new MyArray();Ones.create(HEIGHT, WIDTH);
        for (int i = 0; i < HEIGHT; i++)//ones(hei, wid)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                Ones.array[i][j] = 1.0f;
            }
        }
        MyArray Number=new MyArray();Number.create(HEIGHT, WIDTH);
        //N = boxfilter(ones(hei, wid), r)
        Number=boxfilter(Ones, HEIGHT, WIDTH, R);
        MyArray mean_I=new MyArray();mean_I.create(HEIGHT, WIDTH);
        //mean_I = boxfilter(I, r) ./ N
        mean_I=boxfilter(imSrc_I, HEIGHT, WIDTH, R);
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                mean_I.array[i][j] = mean_I.array[i][j] / Number.array[i][j];
            }
        }
        MyArray mean_P=new MyArray();mean_P.create(HEIGHT, WIDTH);
        //mean_p = boxfilter(p, r) . / N
        mean_P=boxfilter(imSrc_P, HEIGHT, WIDTH, R);
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                mean_P.array[i][j] = mean_P.array[i][j] / Number.array[i][j];
            }
        }
        MyArray imSrc_IP=new MyArray();imSrc_IP.create(HEIGHT, WIDTH);
        //mean_Ip = boxfilter(I.*p, r) . / N
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                imSrc_IP.array[i][j] = imSrc_I.array[i][j] * imSrc_P.array[i][j];
            }
        }
        MyArray mean_IP=new MyArray();mean_IP.create(HEIGHT, WIDTH);
        mean_IP=boxfilter(imSrc_IP, HEIGHT, WIDTH, R);
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                mean_IP.array[i][j] = mean_IP.array[i][j] / Number.array[i][j];
            }
        }
        MyArray cov_IP=new MyArray();cov_IP.create(HEIGHT, WIDTH);
        //cov_Ip = mean_Ip - mean_I .* mean_p
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                cov_IP.array[i][j] = mean_IP.array[i][j] - (mean_I.array[i][j] * mean_P.array[i][j]);
            }
        }
        MyArray imSrc_II=new MyArray();imSrc_II.create(HEIGHT, WIDTH);
        //mean_II = boxfilter(I.*I, r) ./ N
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                imSrc_II.array[i][j] = imSrc_I.array[i][j] * imSrc_I.array[i][j];
            }
        }
        MyArray mean_II=new MyArray();mean_II.create(HEIGHT, WIDTH);
        mean_II=boxfilter(imSrc_II, HEIGHT, WIDTH, R);
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                mean_II.array[i][j] = mean_II.array[i][j] / Number.array[i][j];
            }
        }
        MyArray var_I=new MyArray();var_I.create(HEIGHT, WIDTH);
        //var_I = mean_II - mean_I.*mean_I
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                var_I.array[i][j] = mean_II.array[i][j] - (mean_I.array[i][j] * mean_I.array[i][j]);
            }
        }
        MyArray a=new MyArray();a.create(HEIGHT, WIDTH);
        //a = cov_Ip ./ (var_I + eps)
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                a.array[i][j] = (float) (cov_IP.array[i][j] / (var_I.array[i][j] + eps));  //针对float类型的操作
            }
        }
        MyArray b=new MyArray();b.create(HEIGHT, WIDTH);
        //b = mean_p - a .* mean_I
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                b.array[i][j] = mean_P.array[i][j] - (mean_I.array[i][j] * a.array[i][j]);
            }
        }
        MyArray mean_a=new MyArray();mean_a.create(HEIGHT, WIDTH);
        //mean_a = boxfilter(a, r) ./ N
        mean_a=boxfilter(a, HEIGHT, WIDTH, R);
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                mean_a.array[i][j] = mean_a.array[i][j] / Number.array[i][j];
            }
        }
        MyArray mean_b=new MyArray();mean_b.create(HEIGHT, WIDTH);
        //mean_b = boxfilter(b, r) ./ N
        mean_b=boxfilter(b, HEIGHT, WIDTH, R);
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                mean_b.array[i][j] = mean_b.array[i][j] / Number.array[i][j];
            }
        }
        MyArray imDst=new MyArray();imDst.create(HEIGHT, WIDTH);
        //q = mean_a .* I + mean_b;
        for (int i = 0; i < HEIGHT; i++)
        {
            for (int j = 0; j < WIDTH; j++)
            {
                imDst.array[i][j] = (mean_a.array[i][j] * imSrc_I.array[i][j]) + mean_b.array[i][j];
            }
        }
        /**************************************************************************/
        /***      以上部分是引导滤波器的算法，图像imDst是最终结果          ***/
        /**************************************************************************/
        return imDst.array;
    }
    public static MyArray boxfilter(MyArray imSrc,int height,int width,int r) {
        int row, col, count1, count2;
        MyArray imSum=new MyArray();
        MyArray imDst=new MyArray();
        imSum.create(height, width);
        imDst.create(height, width);
        for (row = 0; row < height; row++)//imDst = zeros(size(imSrc))
        {
            for (col = 0; col < width; col++)
            {
                imDst.array[row][col] = 0;
            }
        }
        for (col = 0; col < width; col++)//imSum = cumsum(imSrc, 1)
        {
            float sum = 0.0f;                 //针对float型数据
            for (row = 0; row < height; row++)
            {
                imSum.array[row][col] = sum + imSrc.array[row][col];
                sum = imSum.array[row][col];
            }
        }
        count1 = r;
        for (row = 0; row <= r; row++)//imDst(1:r + 1, : ) = imSum(1 + r:2 * r + 1, : )
        {
            for (col = 0; col < width; col++)
            {
                imDst.array[row][col] = imSum.array[count1][col];
            }
            count1++;
        }
        count1 = 2 * r + 1; count2 = 0;
        for (row = r + 1; row <= height - r - 1; row++)//imDst(r + 2:hei - r, : ) = imSum(2 * r + 2:hei, : ) - imSum(1:hei - 2 * r - 1, : )
        {
            for (col = 0; col < width; col++)
            {
                imDst.array[row][col] = imSum.array[count1][col] - imSum.array[count2][col];
            }
            count1++; count2++;
        }
        MyArray imRepeat1=new MyArray();
        imRepeat1.create(r, width);
        for (row = 0; row < r; row++)// repmat(imSum(hei, :), [r, 1])
        {
            for (col = 0; col < width; col++)
            {
                imRepeat1.array[row][col] = imSum.array[height - 1][col];
            }
        }
        count1 = 0; count2 = height - 2 * r - 1;
        for (row = height - r; row < height; row++)//imDst(hei-r+1:hei, :) = repmat(imSum(hei, :), [r, 1]) - imSum(hei-2*r:hei-r-1, :)
        {
            for (col = 0; col < width; col++)
            {
                imDst.array[row][col] = imRepeat1.array[count1][col] - imSum.array[count2][col];
            }
            count1++; count2++;
        }
        for (row = 0; row < height; row++)//imSum = cumsum(imDst, 2)
        {
            float sum = 0.0f;                  //针对float型数据
            for (col = 0; col < width; col++)
            {
                imSum.array[row][col] = sum + imDst.array[row][col];
                sum = imSum.array[row][col];
            }
        }
        count1 = r;
        for (col = 0; col <= r; col++)//imDst(:, 1:r+1) = imCum(:, 1+r:2*r+1)
        {
            for (row = 0; row < height; row++)
            {
                imDst.array[row][col] = imSum.array[row][count1];
            }
            count1++;
        }
        count1 = 2 * r + 1; count2 = 0;
        for (col = r + 1; col <= width - r - 1; col++)//imDst(:, r+2:wid-r) = imCum(:, 2*r+2:wid) - imCum(:, 1:wid-2*r-1)
        {
            for (row = 0; row < height; row++)
            {
                imDst.array[row][col] = imSum.array[row][count1] - imSum.array[row][count2];
            }
            count1++; count2++;
        }
        MyArray imRepeat2=new MyArray();
        imRepeat2.create(height, r);
        for (col = 0; col < r; col++)// repmat(imCum(:, wid), [1, r])
        {
            for (row = 0; row < height; row++)
            {
                imRepeat2.array[row][col] = imSum.array[row][width - 1];
            }
        }
        count1 = 0; count2 = width - 2 * r - 1;
        for (col = width - r; col < width; col++)//imDst(:, wid-r+1:wid) = repmat(imCum(:, wid), [1, r]) - imCum(:, wid-2*r:wid-r-1)
        {
            for (row = 0; row < height; row++)
            {
                imDst.array[row][col] = imRepeat2.array[row][count1] - imSum.array[row][count2];
            }
            count1++; count2++;
        }
        return imDst;
    }
}

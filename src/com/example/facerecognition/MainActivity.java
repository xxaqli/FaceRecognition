package com.example.facerecognition;

import java.io.ByteArrayOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

//@TargetApi(23)
public class MainActivity extends Activity {
	
	private static final int CHOOSE_PIC=1;
	private static final String TAG="MainActivity";
	
	final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
	
	private Button get_image;
	private Button detect;
	private TextView textView;
	private ImageView imageView;
	private View mWaiting;
	//private ProgressBar progressBar;
	private Bitmap img;
	
	private String fileSrc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//判断系统版本，6.0系统以上才需要申请权限
		if(Build.VERSION.SDK_INT>=23){
			insertDummyContactWrapper();
		}
		
		
		get_image=(Button)findViewById(R.id.get_image);
		imageView=(ImageView)findViewById(R.id.photo);
		textView=(TextView)findViewById(R.id.tip);
		detect=(Button)findViewById(R.id.detect);
		mWaiting=findViewById(R.id.waiting);
		//progressBar=(ProgressBar)findViewById(R.layout.waiting);
		
		get_image.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//设置get_image按钮的点击事件
				// TODO Auto-generated method stub
				Intent photoPickerIntent=new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, CHOOSE_PIC);
				
			}
		});
		
		detect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				textView.setText("Waiting...");
				FaceppDetect faceDetect=new FaceppDetect();
				//在传入img之前，需要判断是否选择了图片
				if(fileSrc!=null && !fileSrc.trim().equals(""))
				{
					//此处不需要添加操作，因为在选择图片的时候，已经对图片进行压缩操作了，而且img是全局变量
					//resizePhoto();
					
				}else{
					//否则传入当前默认图片
					img=BitmapFactory.decodeResource(getResources(), R.drawable.t4);
				}
				
				//给回调机制传入实例
				faceDetect.detect(img, new DetectCallback() {
					
					@Override
					//回调机制，访问Internet出现错误时，调用该方法
					public void onError(Exception e) {
						// TODO Auto-generated method stub
						e.printStackTrace();
						MainActivity.this.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								textView.setText("Network Error!");
							}
						});
					}
					
					@Override
					//对返回的Json数据进行处理
					public void detectResult(final JSONObject result) {
						// TODO Auto-generated method stub
						Log.d(TAG, result.toString());
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								prepareBitmap(result);
								imageView.setImageBitmap(img);
							}
						});
						
						
					}
				});
				
				
			}
		});
		
	}
	
	
	//对网络请求返回的json数据进行处理
	private void prepareBitmap(JSONObject result){
		//新建一个指定宽，高，和配置参数的位图对象
		Bitmap bitmap=Bitmap.createBitmap(img.getWidth(),img.getHeight(),img.getConfig());
		//实例化一块画布,这里操作时忘了把bitmap作为参数传进去，导致一直无法显示图像
		Canvas canvas=new Canvas(bitmap);
		//将原图先画到画布上
		canvas.drawBitmap(img, new Matrix(), null);
		
		//设置画笔参数
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		//paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 300f);
		paint.setStrokeWidth(3);
		
		try{
			//解析JSON数据
			JSONArray faceInfo=result.getJSONArray("face");
			final int count=faceInfo.length();
			textView.setText("Finished, "+count+" faces.");
			
			for(int i=0;i<count;i++){
				float x,y,w,h;
				//获取中心点在图片中的百分比
				x=(float)faceInfo.getJSONObject(i).getJSONObject("position")
						.getJSONObject("center").getDouble("x");
				y=(float)faceInfo.getJSONObject(i).getJSONObject("position")
						.getJSONObject("center").getDouble("y");
				
				//获取脸部大小在图片中的百分比
				w = (float)faceInfo.getJSONObject(i)
						.getJSONObject("position").getDouble("width");
				h = (float)faceInfo.getJSONObject(i)
						.getJSONObject("position").getDouble("height");
				
				//将百分比转换成实数
				x = x / 100 * img.getWidth();
				w = w / 100 * img.getWidth() * 0.7f;
				y = y / 100 * img.getHeight();
				h = h / 100 * img.getHeight() * 0.7f;

				//画矩形框标记出人脸
				canvas.drawLine(x - w, y - h, x - w, y + h, paint);
				canvas.drawLine(x - w, y - h, x + w, y - h, paint);
				canvas.drawLine(x + w, y + h, x - w, y + h, paint);
				canvas.drawLine(x + w, y + h, x + w, y - h, paint);
				
				//得到年龄信息
				int age=faceInfo.getJSONObject(i).getJSONObject("attribute").getJSONObject("age").getInt("value");
				//得到性别信息  
                String gender=faceInfo.getJSONObject(i).getJSONObject("attribute").getJSONObject("gender").getString("value");
                
                /*
                //绘制年龄，性别
                Bitmap ageBitmap=buildAgeBitmap(age, "Male".equals(gender));
                //对气泡与原图按比例缩放，避免在小图片上显示过大的气泡
                if(bitmap.getWidth()<imageView.getWidth() && bitmap.getHeight()<imageView.getHeight()){
                	//设置缩放比
                	float ratio=Math.max(bitmap.getWidth()*1.0f/imageView.getWidth(),
                			bitmap.getHeight()*1.0f/imageView.getHeight());
                	
                	//对气泡进行缩放
                	ageBitmap=Bitmap.createScaledBitmap(ageBitmap, (int)(ageBitmap.getWidth()*ratio), (int)(ageBitmap.getHeight()*ratio), false);
                	
                }
                
                //在画布上画出气泡
                canvas.drawBitmap(ageBitmap, x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);
				*/
			
			}
			
			//得到标记出人脸的图像
			img=bitmap;

			
			
		}catch(Exception e){
			e.printStackTrace();
			textView.setText("Error.");

		}
        
        
		
	}
	
	/*
	//绘制Bitmap对象，显示年龄性别
	//@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private Bitmap buildAgeBitmap(int age,boolean isMale){
		TextView tv=(TextView)mWaiting.findViewById(R.id.id_age_and_gender);
		//显示年龄时，加上空格使其转换成字符型
		tv.setText(age+"");
		if(isMale){
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male,null), null,null,null);
		}else{
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female,null), null,null,null);
		}

		//把view转换成bitmap
		//使用setDrawingCacheEnabled(boolean flag)提高绘图速度  
        //View组件显示的内容可以通过cache机制保存为bitmap  
        //这里要获取它的cache先要通过setDrawingCacheEnable方法把cache开启，  
        // 然后再调用getDrawingCache方法就可 以获得view的cache图片了。  
        tv.setDrawingCacheEnabled(true);  
        Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());  
        //关闭许可  
        tv.destroyDrawingCache();  
        return bitmap;  
			
	}
	*/
	
	
	
	//6.0系统获取读写存储卡权限
	@TargetApi(23)
	private void insertDummyContactWrapper() {
	    int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
	    if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
	        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
	                REQUEST_CODE_ASK_PERMISSIONS);
	        return;
	    }
	}
	
	@Override
	//响应选取图片的操作
	protected void onActivityResult(int requestCode,int resultCode,Intent intent){
		super.onActivityResult(requestCode, resultCode,intent);
		switch(requestCode){
		case CHOOSE_PIC:
			if(resultCode==RESULT_OK){
				if(intent !=null){
					Cursor cursor = getContentResolver().query(intent.getData(), null, null, null, null);
					cursor.moveToFirst();
					int idx=cursor.getColumnIndex(ImageColumns.DATA);
					fileSrc=cursor.getString(idx);
					cursor.close();
					//获取选中图片的路径
					Log.d(TAG, "Picture:"+fileSrc);
					//System.out.println(fileSrc);

					//对图片进行处理，使其大小规范化
					resizePhoto();
					
					//设置显示图片
					imageView.setImageBitmap(img);
					textView.setText("Click Detect==>");
					
			
				}else{
					Log.d(TAG,"取消图片选取");
				}
				
			}
		}
		
	}
	
	//对图片进行处理,使其大小规范化
	private void resizePhoto(){
		//为了避免图片过大，造成OOM,首先仅获取图片的宽高
		Options options=new Options();
		options.inJustDecodeBounds=true;
		img=BitmapFactory.decodeFile(fileSrc,options);
		
		//通过获取到的宽和高，将图片规范化大小，并真正获取图片
		options.inSampleSize=Math.max(1, (int)Math.ceil(Math.max((double)options.outWidth / 1024f, (double)options.outHeight / 1024f)));
		options.inJustDecodeBounds=false;
		img=BitmapFactory.decodeFile(fileSrc,options);
	}
	
	
	//构造人脸检测类
	private class FaceppDetect{
		/*
		DetectCallback callback=null;
		
		public void setDetectCallback(DetectCallback detectCallback){
			callback=detectCallback;
		}
		*/
		
		public void detect(final Bitmap image,final DetectCallback callback){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					HttpRequests httpRequests=new HttpRequests(Constant.KEY,Constant.SECRET,true,true);
					
					//可以捕捉内存缓冲区的数据，转换陈字节数组
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    		float scale = Math.min(1, Math.min(600f / img.getWidth(), 600f / img.getHeight()));
		    		Matrix matrix = new Matrix();
		    		matrix.postScale(scale, scale);
		    		
		    		//从原始位图剪切图像
		    		Bitmap imgSmall = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
		    		//100表示不压缩，然后将压缩后的图片传到stream中
		    		imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		    		
		    		byte[] array=stream.toByteArray();
		    		
		    		try{
		    			//调用API，并且通过网络传输数据检测，返回result
		    			JSONObject result=httpRequests.detectionDetect(new PostParameters().setImg(array));
		    			if(callback!=null){
		    				//回调detectResult方法
		    				callback.detectResult(result);
		    			}
		    		}catch(Exception e){
		    			if(callback!=null){
		    				//回调onError方法
		    				callback.onError(e);
		    			}
		    		}
				}
			}).start();
		}
		
		
	}
	
	//设置回调机制
	interface DetectCallback {
    	void detectResult(JSONObject result);
    	void onError(Exception e);
	}
}

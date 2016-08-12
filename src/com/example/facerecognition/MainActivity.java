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
		
		//�ж�ϵͳ�汾��6.0ϵͳ���ϲ���Ҫ����Ȩ��
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
				//����get_image��ť�ĵ���¼�
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
				//�ڴ���img֮ǰ����Ҫ�ж��Ƿ�ѡ����ͼƬ
				if(fileSrc!=null && !fileSrc.trim().equals(""))
				{
					//�˴�����Ҫ��Ӳ�������Ϊ��ѡ��ͼƬ��ʱ���Ѿ���ͼƬ����ѹ�������ˣ�����img��ȫ�ֱ���
					//resizePhoto();
					
				}else{
					//�����뵱ǰĬ��ͼƬ
					img=BitmapFactory.decodeResource(getResources(), R.drawable.t4);
				}
				
				//���ص����ƴ���ʵ��
				faceDetect.detect(img, new DetectCallback() {
					
					@Override
					//�ص����ƣ�����Internet���ִ���ʱ�����ø÷���
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
					//�Է��ص�Json���ݽ��д���
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
	
	
	//���������󷵻ص�json���ݽ��д���
	private void prepareBitmap(JSONObject result){
		//�½�һ��ָ�����ߣ������ò�����λͼ����
		Bitmap bitmap=Bitmap.createBitmap(img.getWidth(),img.getHeight(),img.getConfig());
		//ʵ����һ�黭��,�������ʱ���˰�bitmap��Ϊ��������ȥ������һֱ�޷���ʾͼ��
		Canvas canvas=new Canvas(bitmap);
		//��ԭͼ�Ȼ���������
		canvas.drawBitmap(img, new Matrix(), null);
		
		//���û��ʲ���
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 300f);
		//paint.setStrokeWidth(3);
		
		//�����������
		Paint paint_text=new Paint();
		paint_text.setColor(Color.BLUE);
		paint_text.setTextSize(24);
		//paint_text.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 10f);
		
		try{
			//����JSON����
			JSONArray faceInfo=result.getJSONArray("face");
			final int count=faceInfo.length();
			textView.setText("Finished, "+count+" faces.");
			
			for(int i=0;i<count;i++){
				float x,y,w,h;
				//��ȡ���ĵ���ͼƬ�еİٷֱ�
				x=(float)faceInfo.getJSONObject(i).getJSONObject("position")
						.getJSONObject("center").getDouble("x");
				y=(float)faceInfo.getJSONObject(i).getJSONObject("position")
						.getJSONObject("center").getDouble("y");
				
				//��ȡ������С��ͼƬ�еİٷֱ�
				w = (float)faceInfo.getJSONObject(i)
						.getJSONObject("position").getDouble("width");
				h = (float)faceInfo.getJSONObject(i)
						.getJSONObject("position").getDouble("height");
				
				//���ٷֱ�ת����ʵ��
				x = x / 100 * img.getWidth();
				w = w / 100 * img.getWidth() * 0.7f;
				y = y / 100 * img.getHeight();
				h = h / 100 * img.getHeight() * 0.7f;

				//�����ο��ǳ�����
				canvas.drawLine(x - w, y - h, x - w, y + h, paint);
				canvas.drawLine(x - w, y - h, x + w, y - h, paint);
				canvas.drawLine(x + w, y + h, x - w, y + h, paint);
				canvas.drawLine(x + w, y + h, x + w, y - h, paint);
				
				//�õ�������Ϣ
				int age=faceInfo.getJSONObject(i).getJSONObject("attribute").getJSONObject("age").getInt("value");
				//�õ��Ա���Ϣ  
                String gender=faceInfo.getJSONObject(i).getJSONObject("attribute").getJSONObject("gender").getString("value");
                
                
                canvas.drawText(gender+":"+age+"", x-w, y, paint_text);
                /*
                //�������䣬�Ա�
                Bitmap ageBitmap=buildAgeBitmap(age, "Male".equals(gender));
                //��������ԭͼ���������ţ�������СͼƬ����ʾ���������
                if(bitmap.getWidth()<imageView.getWidth() && bitmap.getHeight()<imageView.getHeight()){
                	//�������ű�
                	float ratio=Math.max(bitmap.getWidth()*1.0f/imageView.getWidth(),
                			bitmap.getHeight()*1.0f/imageView.getHeight());
                	
                	//�����ݽ�������
                	ageBitmap=Bitmap.createScaledBitmap(ageBitmap, (int)(ageBitmap.getWidth()*ratio), (int)(ageBitmap.getHeight()*ratio), false);
                	
                }
                
                //�ڻ����ϻ�������
                canvas.drawBitmap(ageBitmap, x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);
				*/
			
			}
			
			//�õ���ǳ�������ͼ��
			img=bitmap;

			
			
		}catch(Exception e){
			e.printStackTrace();
			textView.setText("Error.");

		}
        
        
		
	}
	
	/*
	//����Bitmap������ʾ�����Ա�
	//@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private Bitmap buildAgeBitmap(int age,boolean isMale){
		TextView tv=(TextView)mWaiting.findViewById(R.id.id_age_and_gender);
		//��ʾ����ʱ�����Ͽո�ʹ��ת�����ַ���
		tv.setText(age+"");
		if(isMale){
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male,null), null,null,null);
		}else{
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female,null), null,null,null);
		}

		//��viewת����bitmap
		//ʹ��setDrawingCacheEnabled(boolean flag)��߻�ͼ�ٶ�  
        //View�����ʾ�����ݿ���ͨ��cache���Ʊ���Ϊbitmap  
        //����Ҫ��ȡ����cache��Ҫͨ��setDrawingCacheEnable������cache������  
        // Ȼ���ٵ���getDrawingCache�����Ϳ� �Ի��view��cacheͼƬ�ˡ�  
        tv.setDrawingCacheEnabled(true);  
        Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());  
        //�ر����  
        tv.destroyDrawingCache();  
        return bitmap;  
			
	}
	*/
	
	
	
	//6.0ϵͳ��ȡ��д�洢��Ȩ��
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
	//��ӦѡȡͼƬ�Ĳ���
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
					//��ȡѡ��ͼƬ��·��
					Log.d(TAG, "Picture:"+fileSrc);
					//System.out.println(fileSrc);

					//��ͼƬ���д���ʹ���С�淶��
					resizePhoto();
					
					//������ʾͼƬ
					imageView.setImageBitmap(img);
					textView.setText("Click Detect==>");
					
			
				}else{
					Log.d(TAG,"ȡ��ͼƬѡȡ");
				}
				
			}
		}
		
	}
	
	//��ͼƬ���д���,ʹ���С�淶��
	private void resizePhoto(){
		//Ϊ�˱���ͼƬ�������OOM,���Ƚ���ȡͼƬ�Ŀ��
		Options options=new Options();
		options.inJustDecodeBounds=true;
		img=BitmapFactory.decodeFile(fileSrc,options);
		
		//ͨ����ȡ���Ŀ�͸ߣ���ͼƬ�淶����С����������ȡͼƬ
		options.inSampleSize=Math.max(1, (int)Math.ceil(Math.max((double)options.outWidth / 1024f, (double)options.outHeight / 1024f)));
		options.inJustDecodeBounds=false;
		img=BitmapFactory.decodeFile(fileSrc,options);
	}
	
	
	//�������������
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
					
					//���Բ�׽�ڴ滺���������ݣ�ת�����ֽ�����
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    		float scale = Math.min(1, Math.min(600f / img.getWidth(), 600f / img.getHeight()));
		    		Matrix matrix = new Matrix();
		    		matrix.postScale(scale, scale);
		    		
		    		//��ԭʼλͼ����ͼ��
		    		Bitmap imgSmall = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
		    		//100��ʾ��ѹ����Ȼ��ѹ�����ͼƬ����stream��
		    		imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		    		
		    		byte[] array=stream.toByteArray();
		    		
		    		try{
		    			//����API������ͨ�����紫�����ݼ�⣬����result
		    			JSONObject result=httpRequests.detectionDetect(new PostParameters().setImg(array));
		    			if(callback!=null){
		    				//�ص�detectResult����
		    				callback.detectResult(result);
		    			}
		    		}catch(Exception e){
		    			if(callback!=null){
		    				//�ص�onError����
		    				callback.onError(e);
		    			}
		    		}
				}
			}).start();
		}
		
		
	}
	
	//���ûص�����
	interface DetectCallback {
    	void detectResult(JSONObject result);
    	void onError(Exception e);
	}
}

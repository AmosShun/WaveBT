package com.google.wave_bt;



import java.io.File;
import java.util.ArrayList;
import java.util.List;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WaveActivity extends Activity {

	private Button btnUp;
	private Button btnDown;
	private Button btnX;
	private Button btnx;
	private Button btnTG;
	private Button btnSampleRate;
	//private Button btnMotion;
	private EditText editSR;
	//private EditText recvMsg;
	private SurfaceView sfv;	//ECG画布
	private SurfaceView sfv_acc;	//Acc画布
	//private int refresh_stage = 0;
	private TextView RR_interval;
	private TextView heart_rate;
	private TextView QRS_interval;
	private int prev_alive = 0;
	public static Handler handler;

	ClsOscilloscope clsOscilloscope = new ClsOscilloscope();
	
	Paint mPaint;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// 按键
		btnUp = (Button) this.findViewById(R.id.btnUp);
		btnUp.setOnClickListener(new ClickEvent());
		btnDown = (Button) this.findViewById(R.id.btnDown);
		btnDown.setOnClickListener(new ClickEvent());
		btnX = (Button) this.findViewById(R.id.btnX);
		btnX.setOnClickListener(new ClickEvent());
		btnx = (Button) this.findViewById(R.id.btnx);
		btnx.setOnClickListener(new ClickEvent());
		btnTG = (Button) this.findViewById(R.id.btnTG);
		btnTG.setOnClickListener(new ClickEvent());
		//btnSampleRate = (Button) this.findViewById(R.id.btnSampleRate);
		//btnSampleRate.setOnClickListener(new ClickEvent());
		//btnMotion = (Button) this.findViewById(R.id.btnMotion);
		//btnMotion.setOnClickListener(new ClickEvent());
		//editSR = (EditText) this.findViewById(R.id.editSR);
		//recvMsg = (EditText) this.findViewById(R.id.recvMsg);
		
		//参数显示
		RR_interval = (TextView)this.findViewById(R.id.rR);
		RR_interval.setText("RR间期：初始化中...");
		heart_rate = (TextView)this.findViewById(R.id.heartRate);
		heart_rate.setText("心率：初始化中...");
		QRS_interval = (TextView)this.findViewById(R.id.qRs);
		QRS_interval.setText("QRS间期：初始化中...");
		
		// 画板和画笔
		sfv = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		//sfv.setOnTouchListener(new TouchEvent());
		sfv.setOnClickListener(new ClickEvent());
		sfv_acc = (SurfaceView) this.findViewById(R.id.SurfaceView02);
		mPaint = new Paint();
		mPaint.setColor(Color.GREEN);// 画笔为绿色
		mPaint.setStrokeWidth(1);// 设置画笔粗细
		// 示波器类库
		clsOscilloscope.initOscilloscope();

		/* start ASAP */		
		clsOscilloscope.Start(sfv,sfv_acc, mPaint);
		
		handler = new Handler() {
			@Override
            public void handleMessage(Message msg) {
				if (msg.getData().containsKey("msg")) {
					Log.i("EXG Wave", "handleMessage: " + msg.getData().getString("msg"));
					//recvMsg.setText(msg.getData().getString("msg"));
				} else if (msg.getData().containsKey("seq")) {
					refresh_title(msg.getData().getString("seq"));
				}
				if(msg.what == 1){
					RR_interval.setText("RR间期："+msg.arg1+" ms");
				}
				if(msg.what == 2){
					QRS_interval.setText("QRS间期："+msg.arg1+" ms");
				}
				if(msg.what == 3){
					heart_rate.setText("心率："+msg.arg1+" bpm");
				}
			}
		};
	}
	/*菜单*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	/*菜单按钮*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case R.id.save:
			SaveDialog();
			break;
		case R.id.load:
			LoadDialog();
			break;
		}
		return super.onOptionsItemSelected(item);
	}	
	// Method 
	static public void updateMessage (String msg) {
		
	}
	
	@Override
	protected void onDestroy() {
		clsOscilloscope.Stop();
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	public void refresh_title(String seq) {
		setTitle(seq);
	}
	
	/**
	 * 按键事件处理
	 */
	class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (v == btnUp) {
				clsOscilloscope.adjust_start_y(-1);
			} else if (v == btnDown) {
				clsOscilloscope.adjust_start_y(1);
			} else if (v == btnX) {
				clsOscilloscope.adjust_total_y(1);
			} else if (v == btnx) {
				clsOscilloscope.adjust_total_y(-1);
			} else if (v == btnTG) {
				clsOscilloscope.set_trigger();
			} else if (v == btnSampleRate) {
				if (editSR.getText().toString().matches("[0-9]+.[0-9]+.[0-9]+.[0-9]+:[0-9]+")) {
					Log.i("EXG Wave", "Got socket request to address: "+editSR.getText().toString());
					//clsOscilloscope.start_socket(editSR.getText().toString());
				} else {
					int val = Integer.parseInt(editSR.getText().toString());
					clsOscilloscope.set_sample_rate(val);
				}
			//} else if (v == btnMotion) {
			//	clsOscilloscope.motion_toggle();
			}else if (v == sfv){
				/*点击SurfaceView,冻结屏幕*/
				if(clsOscilloscope.PAUSE == false){
					clsOscilloscope.PAUSE = true;
				}
				else
					clsOscilloscope.PAUSE = false;
			}
		}
	}

	/**
	 * 触摸屏动态设置波形图基线
	 */
	class TouchEvent implements OnTouchListener {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (prev_alive != clsOscilloscope.alive) {
				prev_alive = clsOscilloscope.alive;
			}
			return true;
		}

	}
	
	/*
	 * 保存波形的对话框
	 */
	public void SaveDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(WaveActivity.this);
		//设置对话框布局文件
		View view = View.inflate(this,R.layout.save_dialog,null);
		builder.setView(view);
		final EditText edit_text = (EditText)view.findViewById(R.id.filename);
		//按键
        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String file_name = edit_text.getText().toString();
                clsOscilloscope.save(clsOscilloscope.paint_buffer_pause,file_name);
            }
        });

		

		builder.create().show();
	}
	/*
	 * 读取波形的对话框
	 */
	public void LoadDialog(){
		/*获得文件列表*/
		File root = new File(Environment.getExternalStorageDirectory()+"/EXG_DATA/save");
		List<String> items = new ArrayList<String>();
		for(File file : root.listFiles()){
			items.add(file.getName());
		}	//得到文件名列表
		final String[] files = new String[items.size()];	//转成String数组
		items.toArray(files);
		/*设置对话框*/
		AlertDialog.Builder builder = new AlertDialog.Builder(WaveActivity.this);
		builder.setTitle("选择波形文件");
		builder.setItems(files, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				clsOscilloscope.load(files[which]);
			}
			
		});
		builder.create().show();
	}
}

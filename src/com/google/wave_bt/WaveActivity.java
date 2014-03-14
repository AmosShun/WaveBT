package com.google.wave_bt;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
	private SurfaceView sfv;
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
		setContentView(R.layout.main2);
		
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
		btnSampleRate = (Button) this.findViewById(R.id.btnSampleRate);
		btnSampleRate.setOnClickListener(new ClickEvent());
		//btnMotion = (Button) this.findViewById(R.id.btnMotion);
		//btnMotion.setOnClickListener(new ClickEvent());
		editSR = (EditText) this.findViewById(R.id.editSR);
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
		sfv.setOnTouchListener(new TouchEvent());
		mPaint = new Paint();
		mPaint.setColor(Color.GREEN);// 画笔为绿色
		mPaint.setStrokeWidth(1);// 设置画笔粗细
		// 示波器类库
		clsOscilloscope.initOscilloscope();

		/* start ASAP */		
		clsOscilloscope.Start(sfv, mPaint);
		
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
}

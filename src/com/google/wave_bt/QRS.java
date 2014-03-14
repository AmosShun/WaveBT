package com.google.wave_bt;
import java.util.Arrays;

import android.os.Message;

public class QRS {
	public static final int N_buf = 20;
	private int[] ecg_buf = new int[N_buf*3];
	private int D_THRESHOLD = 10;	//差分阈值
	private int d;	//差分值
	private int qrs_state = 0;
	private int rr_counter = 0;		//RR间期计数器
	private int r_down_counter = 0;		//寻找R波下降沿计数器
	private int RR_interval = 0;//RR间期
	private int QRS_interval = 0;//QRS间期
	private boolean ALREADY_FIND_Q = false;		//Q波找到 标志位
	private int R_position;
	private int heart_rate = 0;//心率
	private int[] RR_intervals = new int[8];	//RR间期数组，用来计算心率
	private int heart_rate_counter = 0;
	
	/*
	 * 寻找R波
	 * 状态0：初始化差分阈值。
	 * 状态1：寻找R波上升沿。
	 * 状态2：寻找R波峰值。
	 * 状态3：寻找R波下降沿，确定R波。
	 */
	public void find_r(int[] ecg){
		//更新ecg_buf
		System.arraycopy(ecg_buf,N_buf,ecg_buf,0,N_buf*2);
		System.arraycopy(ecg,0,ecg_buf,N_buf*2,N_buf);
		//计算差分
		if(ALREADY_FIND_Q){
			find_s(R_position-N_buf);//Q波已经找到，寻找S波
			System.out.print("QRS: "+QRS_interval+"\n");
			//向handler发送message
			Message msg_qrs = WaveActivity.handler.obtainMessage();
			msg_qrs.what = 2;
			msg_qrs.arg1 = QRS_interval;
			WaveActivity.handler.sendMessage(msg_qrs);
		}
		//状态0
		if(qrs_state == 0){
			init_threshold();//初始化差分阈值
		}
		if(qrs_state>0){
			for(int i=N_buf*2; i<N_buf*3; i++){
				//状态1
				if(qrs_state ==1){
					d = ecg_buf[i] - ecg_buf[i-1];
					if(d>max)
						max = d;
					if(d>D_THRESHOLD){
						qrs_state = 2;
						
						
					}
					rr_counter++;
				}
				//状态2
				else if(qrs_state == 2){
					d = ecg_buf[i] - ecg_buf[i-1];
					if(d>max)
						max=d;
					if(d<0){
						qrs_state = 3;
						D_THRESHOLD = (D_THRESHOLD*2*7+max)/8/2;//找到R波峰值，更新阈值
						max = 0;
						
					}
					rr_counter++;
				}
				//状态3
				else if(qrs_state == 3){
					if(r_down_counter<4){
						r_down_counter++;
						d = ecg_buf[i-1] - ecg_buf[i];
						if(d>D_THRESHOLD){
							RR_interval = (rr_counter - r_down_counter)*1000/200;//计算RR间期
							//向handler发送message
							Message msg_rr = WaveActivity.handler.obtainMessage();
							msg_rr.what = 1;
							msg_rr.arg1 = RR_interval;
							WaveActivity.handler.sendMessage(msg_rr);
							
							RR_intervals[heart_rate_counter] = RR_interval;
							heart_rate_counter++;
							if(heart_rate_counter == 8)
								heart_rate_counter = 0;
							if(RR_intervals[7]>0){
								long sum = 0;
								for(int ii=0; ii<8; ii++){
									sum += RR_intervals[ii];
								}
								sum = sum/8;
								heart_rate = (int)(60000/sum);//计算心率
								//向handler发送message
								Message msg_heartrate = WaveActivity.handler.obtainMessage();
								msg_heartrate.what = 3;
								msg_heartrate.arg1 = heart_rate;
								WaveActivity.handler.sendMessage(msg_heartrate);
							}
							R_position = i - r_down_counter - 1;
							find_q(R_position);//寻找Q波
							System.out.print("RR: "+RR_interval + "  HEART RATE: "+heart_rate+" ");
							rr_counter = r_down_counter;
							r_down_counter = 0;
							qrs_state = 1;
							
						}
						rr_counter++;
					}
					else{
						r_down_counter = 0;
						qrs_state = 1;
					}
				}
			}
		}
	}
	
	/*
	 * 寻找Q波
	 * 在当前数据帧中，从R波向前寻找2个拐点。
	 */
	private void find_q(int n){
		int state = 0;
		int counter = 0;
		int first = 0;
		for(int i = n; i>0; i--){
			counter++;
			if(state == 0 && (ecg_buf[i]-ecg_buf[i-1])<=0){
				state = 1;		//找到第一个拐点
				first = counter;
				
			}
			if(state == 1 && (ecg_buf[i]-ecg_buf[i-1])>=0){
				QRS_interval = counter * 1000 /200;		//找到第二个拐点，算入QRS间期
				ALREADY_FIND_Q = true;
				return;
			}
		}
		//没有找到第二个拐点，用第一个拐点
		QRS_interval = first * 1000 / 200;
		ALREADY_FIND_Q = true;
	}

	/*
	 * 寻找S波
	 * 在下一数据帧中，从R波向后寻找2个拐点
	 */
	private void find_s(int n){
		int state = 0;
		int counter = 0;
		int first = 0;
		for(int i=n; i<3*N_buf; i++){
			counter++;
			if(state == 0 &&(ecg_buf[i+1]-ecg_buf[i])>=0){
				state = 1;		//找到第一个拐点
				first = counter;
			}
			if(state == 1 && (ecg_buf[i+1]-ecg_buf[i])<=0){
				QRS_interval += counter * 1000 / 200;	//找到第二个拐点，算入QRS间期
				ALREADY_FIND_Q = false;
				return;
			}
		}
		//没有找到第二个拐点，用第一个拐点
		QRS_interval += first * 1000 / 200;
		ALREADY_FIND_Q = false;
	}
	
	private int[] d_max = new int[10];
	private int init_counter1 = 0;
	private int init_counter2 = 0;
	private int max = 0;
	/*
	 * 初始化差分阈值
	 * 以2秒数据为一组，共十组20秒。每组的差分最大值存入数组，排序后去掉首尾，求平均除以2。
	 */
	private void init_threshold(){
		int d;
		//扫描一个数据包内的差分值
		for(int i=N_buf*2; i<N_buf*3; i++){
			d = ecg_buf[i] - ecg_buf[i-1];
			if(d>max)
				max = d;
		}
		init_counter1++;
		if(init_counter1 == 20){	//2秒结束
			d_max[init_counter2] = max;
			max = 0;
			init_counter2++;
			init_counter1 = 0;
		}
		if(init_counter2 == 10){	//10组结束
			init_counter2 = 0;
			Arrays.sort(d_max);
			int sum = 0;
			for(int i=1;i<9;i++){
				sum += d_max[i];
			D_THRESHOLD = sum/8/2;
			if(D_THRESHOLD>10)		//保证阈值不会太小
				qrs_state = 1;
			}
		}
	}
}
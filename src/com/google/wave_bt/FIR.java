package com.google.wave_bt;

import android.util.Log;

public class FIR {
	//High-pass FIR coefficients.
	private static int[] hn = {
	      934,     82,     82,     79,     73,     63,     51,     35,     16,
	       -7,    -33,    -63,    -96,   -132,   -172,   -214,   -259,   -307,
	     -356,   -408,   -460,   -513,   -567,   -620,   -673,   -725,   -776,
	     -825,   -871,   -914,   -955,   -991,  -1023,  -1051,  -1074,  -1092,
	    -1106,  -1113,  31651,  -1113,  -1106,  -1092,  -1074,  -1051,  -1023,
	     -991,   -955,   -914,   -871,   -825,   -776,   -725,   -673,   -620,
	     -567,   -513,   -460,   -408,   -356,   -307,   -259,   -214,   -172,
	     -132,    -96,    -63,    -33,     -7,     16,     35,     51,     63,
	       73,     79,     82,     82,    934
	};
	private static final int N_hn = 77;	//FIR coefficients length
	private int N_buf = 0;	//ECG Samples length
	private int[] ecg_current = null;
	private int[] ecg_overlap = new int[N_hn - 1];
	private int R_state = 0;
	private static final int OFFSET = 100;	//数据的直流偏移量
	private static final int R_THRESHOLD = 30;
	private static int heart_rate_counter = 0;
	private int heart_rate = 0;
	private static final int SAMPLE_RATE = 200;
	
	/*
	 * Convolution
	 */
	private int[] conv(int[] x, int[] y){
		int[] temp = new int[N_buf+N_hn - 1];
		for(int n=0; n<(N_buf+N_hn - 1); n++){
			long sum = 0;
			for(int i=0; i<(n+1); i++){
				sum += x[i]*y[n-i];
			}
			sum = sum>>14;
			temp[n] = (int)sum;
		}
		return temp;
	}
	
	/*
	 * Get heart rate
	 */
	private void get_heart_rate()
	{
		heart_rate = 60 * SAMPLE_RATE / heart_rate_counter;
		heart_rate_counter = 0;
	}
	
	/*
	 * Find R
	 */
	private void find_R(int[] ecg){
		for(int i=0; i<N_buf; i++){
			if(R_state==0 && ecg[i]>R_THRESHOLD){
				R_state = 1;
				Log.d("R_STATE","R_state: 1");
			}
			if(R_state==1 && ecg[i]<R_THRESHOLD){
				R_state = 0;
				Log.d("R_STATE","R_state: 0");
				get_heart_rate();
				Log.d("HEART RATE","HEART RATE"+heart_rate);
			}
			heart_rate_counter++;
		}
	}
	
	/*
	 * High-pass FIR
	 */
	public int[] highpass(int[] buffer){
		N_buf = buffer.length;
		//Augment zeros to hn
		int[] hn_buffer = new int[N_hn + N_buf - 1];
		System.arraycopy(hn, 0, hn_buffer, 0, N_hn);
		int[] ecg_buffer = new int[N_buf + N_hn - 1];
		System.arraycopy(buffer, 0, ecg_buffer, 0, N_buf);
		//Convolution
		ecg_current = conv(ecg_buffer, hn_buffer);
		//Overlap_add
		for(int i=0; i<(N_hn - 1); i++){
			ecg_current[i] = ecg_current[i] + ecg_overlap[i];
		}
		System.arraycopy(ecg_current, N_buf, ecg_overlap, 0, N_hn-1);
		//Find R
		find_R(ecg_current);
		
		//OFFSET
		int[] temp = new int[N_buf];
		for(int i=0; i<N_buf; i++){
			temp[i] = ecg_current[i] + OFFSET;
		}

		return temp;
	}
	}

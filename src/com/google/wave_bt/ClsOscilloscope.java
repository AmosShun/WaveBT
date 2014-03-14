package com.google.wave_bt;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Set;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class ClsOscilloscope {

	class FileLogger {
		private FileOutputStream exg_output = null;
		private FileInputStream exg_input = null;
		private File exg_folder;
		File current_file;
		private String exg_folder_name;
		private int prev_unix_time = 0;
		private int current_unix_time = 0;
		private int next_unix_time = 0;
		private int current_file_size = 0;
		private int current_pkt_count = 0;
		private List<Integer> next_ut_list = new ArrayList<Integer>();
		private int buffer_current_unix_time = 0;
		 
		/*
		 * Initialize folder
		 */
		public FileLogger(String folderName) {
			current_file_size = 0;
			current_pkt_count = 0;
			exg_folder = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
			if (!exg_folder.exists()) {
				exg_folder.mkdir();
			}
			exg_folder_name = new String(Environment.getExternalStorageDirectory() + "/" + folderName + "/");
		}
		
		public void Stop() {
			if (exg_output != null) {
			try {
				exg_output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		}
				
		public void ReaderInit() {
			do {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (current_unix_time == 0);
			buffer_current_unix_time = current_unix_time;
			Log.i("EXG Wave", "Buffer Management open initial file: " + Integer.toString(buffer_current_unix_time));
			try {
				exg_input = new FileInputStream(new File(exg_folder_name + Integer.toString(buffer_current_unix_time)));
				byte tmpBuf[] = new byte[12];
				int bytesToRead = 12;
				int readRet = 0;
				
				while (bytesToRead > 0) {
					readRet = exg_input.read(tmpBuf, 0, bytesToRead);
					if (readRet < 0) {
						Log.e("EXG Wave", "Failed to read file");
					}
					bytesToRead -= readRet;
				}
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		public int ReaderAvailable() throws IOException {
			return exg_input.available();
		}
		
		public int ReaderRead(byte[] buf, int offset, int length) throws IOException {
			return exg_input.read(buf, offset, length);
		}
		
		public void ReaderSwitchFile() {
			if (next_ut_list.size() > 0) {
				buffer_current_unix_time = next_ut_list.get(0);
				next_ut_list.remove(0);
				try {
					exg_input.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				Log.i("EXG Wave", "Buffer switched to file " + Integer.toString(buffer_current_unix_time));
				try {
					exg_input = new FileInputStream(new File(exg_folder_name + Integer.toString(buffer_current_unix_time)));
					byte tmpBuf[] = new byte[12];
					int bytesToRead = 12;
					int readRet = 0;
					
					while (bytesToRead > 0) {
						readRet = exg_input.read(tmpBuf, 0, bytesToRead);
						if (readRet < 0) {
							Log.e("EXG Wave", "Failed to read file");
						}
						bytesToRead -= readRet;
					}
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		}
		
		private void echoInt (int n) {
			byte[] b = new byte[4];
			for(int i = 0; i < 4; i++){
				b[i] = (byte)(n >> (i * 8)); 
			}
			try {
				exg_output.write(b, 0, 4);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void LogData(byte[] buf) {
			if (current_unix_time == 0 && prev_unix_time == 0) {
				// Create the initial file
				current_unix_time = (int) (System.currentTimeMillis() / 1000L);
				prev_unix_time = current_unix_time;
				
				Log.i("EXG Wave", "Recorder write initial file: " + Integer.toString(current_unix_time));
				current_file = new File(exg_folder_name + Integer.toString(current_unix_time));
				if (!current_file.exists()) {
					try {
						current_file.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					exg_output = new FileOutputStream(current_file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
				current_file_size = 0;
				current_pkt_count = 0;
				echoInt(prev_unix_time);
				echoInt(0);
				echoInt(0);
			}
			
			// Log the packet
			try {
				exg_output.write(buf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			current_file_size += buf.length;
			current_pkt_count++;
			
			// Create a new file if file size larger than 1K
			if (current_file_size > 10000) {
				try {
					exg_output.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				next_unix_time = (int) (System.currentTimeMillis() / 1000L);
				byte b[] = new byte[8];
				for(int i = 0; i < 4; i++){
					b[i] = (byte)(next_unix_time >> (i * 8)); 
					b[4+i] = (byte)(current_pkt_count >> (i * 8));
				}
				next_ut_list.add(next_unix_time);
				RandomAccessFile rf;
				try {
					rf = new RandomAccessFile(exg_folder_name + Integer.toString(current_unix_time), "rw");
					rf.seek(4);
					rf.write(b);
					rf.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				prev_unix_time = current_unix_time;
				current_unix_time = next_unix_time;
				next_unix_time = 0;
				Log.i("EXG Wave", "Recorder change file to: " + Integer.toString(current_unix_time));
				current_file = new File(exg_folder_name + Integer.toString(current_unix_time));
				if (!current_file.exists()) {
					try {
						current_file.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					exg_output = new FileOutputStream(current_file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
				current_file_size = 0;
				current_pkt_count = 0;
				echoInt(prev_unix_time);
				echoInt(0);
				echoInt(0);
			}
		}
		
	};
	
	public int skip_buf_num = 1;
	public int skip_buf_num_delta = 1;
	
	public int start_y = 0;
	public int start_y_delta = 20;
	
	public int total_y = 1024;
	public int total_y_delta = 2;
		
	public RecordThread usb_recv_thread = null;
	public int alive = 0;
	public int frames_per_sec = 5;
	public int total_write_count = 0;
	public int total_read_count = 0;
    public OutputStream outStream = null;
    private FileLogger exgLogger = new FileLogger("EXG_DATA");
    private FileLogger motionLogger = new FileLogger("MOTION_DATA");

    private Derivative derivative = new Derivative();
    private Filter filter = new Filter();
    /*
    int send_over_socket = 0;
    Socket client = null;
    DataOutputStream dout;
	
    public void start_socket(String sock_info) {
    	String [] temp = null;
    	temp = sock_info.split(":");
    	SocketAddress addr = new InetSocketAddress(temp[0], Integer.parseInt(temp[1]));
    	
    	try {
    		Log.i("EXG Wave", "begin connect to: "+temp[0]+":"+temp[1]);
			client = new Socket();
			client.connect(addr, 2000);
			Log.i("EXG Wave", "Socket connected");
			dout = new DataOutputStream(client.getOutputStream());
			send_over_socket = 1;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    */
    
	/**
	 * 初始化
	 */
	public void initOscilloscope() {
	}

	public void motion_toggle() {
		byte[] msg = new byte[6];
		/* 0x03 means motion rate toggle */
		msg[0] = 'C';
		msg[1] = 'M';
		msg[2] = 'D';		
		msg[3] = 0x03;
		msg[4] = 0x00;
		msg[5] = 0x00;
		try {
			outStream.write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 	
	
	public void set_trigger() {
		byte[] msg = new byte[6];
		/* 0x04 means trigger */
		msg[0] = 'C';
		msg[1] = 'M';
		msg[2] = 'D';		
		msg[3] = 0x04;
		msg[4] = 0x00;
		msg[5] = 0x00;
		try {
			outStream.write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	
	public void set_sample_rate(int rate) {
		byte[] msg = new byte[6];
		/* 0x05 means sample rate */
		msg[0] = 'C';
		msg[1] = 'M';
		msg[2] = 'D';		
		msg[3] = 0x05;
		msg[4] = (byte)(rate & 0xff);
		msg[5] = (byte)((rate >> 8) & 0xff);
		try {
			outStream.write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void adjust_speed(int delta) {
		if (delta == 1) {
			if (skip_buf_num < 5) {
				skip_buf_num += skip_buf_num_delta;
			}
		} else {
			if (skip_buf_num > 1) {
				skip_buf_num -= skip_buf_num_delta;
			}
		}
	}
	
	public void adjust_start_y(int delta) {
		if (delta == 1) {
			if (start_y < (1023-start_y_delta)) {
				start_y += start_y_delta;
			}
		} else {
			if (start_y > (start_y_delta)) {
				start_y -= start_y_delta;
			}
		}
	}
	
	public void adjust_total_y(int delta) {
		if (delta == 1) {
			total_y /= total_y_delta;
		} else {
			if (total_y > total_y_delta) {
				total_y *= total_y_delta;
			}
		}
	}
	
	/**
	 * 开始
	 * 
	 * @param recBufSize
	 *            AudioRecord的MinBufferSize
	 */
	public void Start(SurfaceView sfv, Paint mPaint) {
		usb_recv_thread = new RecordThread();
		usb_recv_thread.start();// 开始录制线程
		//new BufferListThread().start();
		new DrawThread(sfv, mPaint).start();// 开始绘制线程
		//Register with server
		List<NameValuePair> login_parm = new ArrayList<NameValuePair>();
		login_parm.add(new BasicNameValuePair("user", "user"));
		login_parm.add(new BasicNameValuePair("password", "USER"));
		/*
		try {
			String ret_msg = HttpUtils.post("http://dan.dminorstudio.com/mobile/user/login", 
					"{\"username\":\"user\", \"password\":\"USER\"}");
			Log.i("EXG_Wave", "login result: "+ret_msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new PostData().start();
		*/
	}

	/**
	 * 停止
	 */
	public void Stop() {
		exgLogger.Stop();
		motionLogger.Stop();

		/*
		try {
			String ret_msg = HttpUtils.post("http://dan.dminorstudio.com/mobile/user/logout", "");
			Log.i("EXG_Wave", "logout result: "+ret_msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

	// Temporary HTTP debug thread
	/*
	class PostData extends Thread {
		private String postMsg;
		public FileInputStream http_input = null;
		private byte[] head_buf = new byte[3];
		private int head_idx = 0;
		private byte[] data_buf;
		private int data_idx = 0;
		private int rc = 0;
		private int msg_length;
		private short seq_num;
		private String http_data;
		
		public PostData() {
			try {
				http_input = new FileInputStream(exg_file);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		private String int_to_str(int i) {
			String s = "";
			if (i == 0)
				s = "0";
			if (i == 1)
				s = "1";
			if (i == 2)
				s = "2";
			if (i == 3)
				s = "3";
			if (i == 4)
				s = "4";
			if (i == 5)
				s = "5";
			if (i == 6)
				s = "6";
			if (i == 7)
				s = "7";
			if (i == 8)
				s = "8";
			if (i == 9)
				s = "9";
			if (i == 10)
				s = "a";
			if (i == 11)
				s = "b";
			if (i == 12)
				s = "c";
			if (i == 13)
				s = "d";
			if (i == 14)
				s = "e";
			if (i == 15)
				s = "f";
			return s;
		}
		
		public void run() {
			while (true) {
				head_idx = 0;
				try {
					while (http_input.available() < 3) {
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while (head_idx < 3) {
					try {
						rc = http_input.read(head_buf, head_idx, (3-head_idx));
						head_idx += rc;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (head_buf[0] != 0x02) {
					Log.e("EXG Wave", "Error in exg.data: header type ne 0x02");
				}
				msg_length = (short) ((head_buf[1] & 0xff) | ((head_buf[2] & 0xff) << 8));
				data_buf = new byte[msg_length+3];
				data_buf[0] = head_buf[0];
				data_buf[1] = head_buf[1];
				data_buf[2] = head_buf[2];
				
				data_idx = 3;
				// wait until there is at least msg_length data to read
				try {
					while (http_input.available() < msg_length) {
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while (data_idx < msg_length+3) {
					try {
						rc = http_input.read(data_buf, data_idx, (msg_length+3-data_idx));
						data_idx += rc;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				seq_num = (short) ((data_buf[3] & 0xff) | ((data_buf[4] & 0xff) << 8));
				http_data = "";
				for(int i = 0; i < data_buf.length; i++){					
				    http_data += int_to_str((data_buf[i] & 0xff)/16);
				    http_data += int_to_str((data_buf[i] & 0xff)%16);
				    http_data += " ";
				}
				Log.i("EXG Wave", http_data);
				postMsg = new String("{\"title\":\"EXG data "+Integer.toString(seq_num)+"\","+
									"\"body\":{\"und\":[{\"value\":\""+http_data+"\"}]},"+
									"\"type\":\"inquiry\","+
									"\"name\":\"user\","+
									"\"language\":\"und\"}");
				Log.i("EXG Wave", "request: "+postMsg);
				try {
					String ret_msg = HttpUtils.post("http://dan.dminorstudio.com/mobile/node", postMsg);
					Log.i("EXG Wave", "post data result: "+ret_msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	};
*/
	
	/**
	 * 负责从BT接收数据，并对数据流进行识别，辨别出：
	 * EXGBT/MOTION/Message
	 */
	class RecordThread extends Thread {
		private BluetoothAdapter mBluetoothAdapter = null;
		private BluetoothDevice device = null;
	    private BluetoothSocket btSocket = null;
	    private InputStream inStream = null;
	    private byte[] activeBuffer = new byte[128];
	    private int activeBufLen = 0;
	    private int exgSeq = -1;
	    private int motionSeq = -1;
	    
	    private final UUID MY_UUID =
	            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

		public RecordThread() {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	        if (mBluetoothAdapter == null) {
	            Log.e("EXG Wave", "Cannot find BT adapter, please enable BT first!");
	        }			
	        if (!mBluetoothAdapter.isEnabled()) {
	        	Log.e("EXG Wave", "BT not enabled");
	        }
	        Set<BluetoothDevice> bondSet = mBluetoothAdapter.getBondedDevices();
	        if (bondSet.size() > 0) {
	        Object objs[] = bondSet.toArray();
	        device = (BluetoothDevice)objs[0];
	        if (device == null) {
	        	Log.e("EXG Wave", "BT device not found");
	        }
	        
	        try {
	            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) {
	            Log.e("EXG Wave", "Socket creation failed.");
	        }
	        
	        try {
	            btSocket.connect();
	        } catch (IOException e) {
	            Log.e("EXG Wave", "Socket connect failed.");
	        }
	        
	        try {
	            outStream = btSocket.getOutputStream();
	            inStream = btSocket.getInputStream();
	        } catch (IOException e) {
	            Log.e("EXG Wave", "stream creation failed.");
	        }
	        
			Log.i("EXG Wave", "BT device init done!"); 
			
			// Try to send "XXX"
			String message = "XXX";
	        byte[] msgBuffer = message.getBytes();
	        try {
	            outStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.e("EXG Wave", "Exception during write.", e);
	        }
	        }
		}
				
		/*
		 * State machine searching for
		 * 1. "EXGBT"
		 * 2. "<<<"
		 * 3. "MOTION"
		 */
		public int[] search_next_tag (byte[] buf, int bufLen, int startIdx, String[] patterns, int hint) {
			int i = 0, j = 0;
			int[] ret = new int[2];
			String tmpStr;
			ret[0] = -1;
			ret[1] = -1;
			
			if (hint < 0) {
				for (i = startIdx; i < bufLen; i++) {
					for (j = 0; j < patterns.length; j++) {
						if ((i + patterns[j].length()) <= bufLen) {
							tmpStr = new String(buf, i, patterns[j].length());
							if (tmpStr.equals(patterns[j]) == true) {
								ret[0] = j;
								ret[1] = i;
								return ret;
							}
						}
					}
				}
			} else {
				if (hint == 0) {
					if ((bufLen - startIdx) < (4 + 3)) {
						return search_next_tag(buf, bufLen, startIdx, patterns, -1);
					} else {
						int tmpLen = buf[startIdx + 5] + (buf[startIdx + 6] << 8);
						if ((bufLen - startIdx) < (tmpLen + 3 + 4 + 6)) {
							return ret;
						} else {
							for (i = 0; i < patterns.length; i++) {
								tmpStr = new String(buf, (startIdx + 4 + 3 + tmpLen), patterns[i].length());
								if (tmpStr.equals(patterns[i]) == true) {
									ret[0] = i;
									ret[1] = (startIdx + 4 + 3 + tmpLen);
									return ret;
								}
							}
							return search_next_tag(buf, bufLen, startIdx, patterns, -1);
						}
					}
				} else if (hint == 1) {
					if ((bufLen - startIdx) < (5 + 1)) {
						return search_next_tag(buf, bufLen, startIdx, patterns, -1);
					} else {
						int tmpLen = buf[startIdx + 5];
						if ((bufLen - startIdx) < (tmpLen + 5 + 1 + 6)) {
							return ret;
						} else {
							for (i = 0; i < patterns.length; i++) {
								tmpStr = new String(buf, (startIdx + 5 + 1 + tmpLen), patterns[i].length());
								if (tmpStr.equals(patterns[i]) == true) {
									ret[0] = i;
									ret[1] = (startIdx + 5 + 1 + tmpLen);
									return ret;
								}
							}
							return search_next_tag(buf, bufLen, startIdx, patterns, -1);
						}
					}					
				} else if (hint == 2) {
					if ((bufLen - startIdx) < (5 + 2)) {
						return search_next_tag(buf, bufLen, startIdx, patterns, -1);
					} else {
						int tmpLen = buf[startIdx + 5] + (buf[startIdx + 6] << 8);
						if ((bufLen - startIdx) < (tmpLen + 5 + 2 + 6)) {
							return ret;
						} else {
							for (i = 0; i < patterns.length; i++) {
								tmpStr = new String(buf, (startIdx + 5 + 2 + tmpLen), patterns[i].length());
								if (tmpStr.equals(patterns[i]) == true) {
									ret[0] = i;
									ret[1] = (startIdx + 5 + 2 + tmpLen);
									return ret;
								}
							}
							return search_next_tag(buf, bufLen, startIdx, patterns, -1);
						}
					}						
				}
				
			}
			return ret;
		}
		
		private void display_message (byte[] buf) {
			Message message= Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putString("msg", new String(buf));
			message.setData(bundle);
			WaveActivity.handler.sendMessage(message);
		}

		private void display_seq (int exg_seq, int motion_seq) {
			Message message= Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putString("seq", "EXG seq: "+Integer.toString(exg_seq)+", Motion seq: "+Integer.toString(motion_seq));
			message.setData(bundle);
			WaveActivity.handler.sendMessage(message);
		}
		
		private void display_string (String str) {
			Message message= Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putString("msg", str);
			message.setData(bundle);
			WaveActivity.handler.sendMessage(message);
		}
		
		public void handle_message (byte[] buf, int type) {
			int length = 0;
			int seqNum = 0;
			
			if (type == 0) {
				length = (buf[6] & 0xFF) | ((buf[7] & 0xFF) << 8);
				seqNum = (buf[8] & 0xFF) | ((buf[9] & 0xFF) << 8);
				if ((length + 8) != buf.length) { 
					Log.e("EXG Wave", "Expected length: " + Integer.toString(length) +
							" , but got: " + Integer.toString(buf.length));
				}
				//Log.i("EXG Wave", "EXGBT: " + Integer.toString(seqNum));
				if (exgSeq == -1) {
					exgSeq = seqNum; 
				} else {
					if (exgSeq + 1 != seqNum) {
						Log.e("EXG Wave", "EXGBT expected: " + Integer.toString(exgSeq + 1) + ", but got " + Integer.toString(seqNum));
						display_string("EXGBT expected: " + Integer.toString(exgSeq + 1) + ", but got " + Integer.toString(seqNum));
					}
					exgSeq = seqNum;
				}
				byte[] tmpBuf = new byte[buf.length - 5];
				System.arraycopy(buf, 5, tmpBuf, 0, buf.length - 5);
				exgLogger.LogData(tmpBuf);
				display_seq(exgSeq, motionSeq);
			} else if (type == 1) { 
				byte[] subBuf = new byte[buf.length - 7];
				System.arraycopy(buf, 7, subBuf, 0, buf.length - 7);
				//Log.i("EXG Wave", "MSG: " + new String(subBuf));	
				display_message(subBuf);
			} else if (type == 2) {
				length = (buf[6] & 0xFF) | ((buf[7] & 0xFF) << 8);
				seqNum = (buf[8] & 0xFF) | ((buf[9] & 0xFF) << 8);
				if ((length + 8) != buf.length) {
					Log.e("EXG Wave", "Expected length: " + Integer.toString(length) +
							" , but got: " + Integer.toString(buf.length));
				}
				//Log.i("EXG Wave", "MOTION: " + Integer.toString(seqNum));
				if (motionSeq == -1) {
					motionSeq = seqNum;
				} else {
					if (motionSeq + 1 != seqNum) {
						Log.e("EXG Wave", "MOTION expected: " + Integer.toString(motionSeq + 1) + ", but got " + Integer.toString(seqNum));
						display_string("MOTION expected: " + Integer.toString(motionSeq + 1) + ", but got " + Integer.toString(seqNum));
					}
					motionSeq = seqNum;
				}
				byte[] tmpBuf = new byte[buf.length - 6];
				System.arraycopy(buf, 6, tmpBuf, 0, buf.length - 6);
				motionLogger.LogData(tmpBuf);
				/*
				if (send_over_socket == 1) {
					try {
						dout.write(tmpBuf);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				*/
				display_seq(exgSeq, motionSeq);
			} else {
				Log.e("EXG Wave", "Unknown type: " + Integer.toString(type));
			}
		}
		
		/*
		 * Return leftover index in buf
		 */
		public int greedy_dispatcher (byte[] buf, int bufLen) {
			int prevPattern = -1, prevIdx = -1, hint = -1;
			int[] searchResult;
			String[] patterns = new String[3];
			patterns[0] = new String("EXGBT");
			patterns[1] = new String("<<<>>>"); 
			patterns[2] = new String("MOTION"); 
			 
			do {
				//Log.i("EXG Wave", "Search " + Integer.toString((prevIdx + 1)) + "/" + Integer.toString(bufLen));
				searchResult = search_next_tag(buf, bufLen, (prevIdx + 1), patterns, hint);
				if (searchResult[0] != -1) { 
					if (prevPattern == -1) {
						if (searchResult[1] != 0) {
							// Warning: there *may* be some garbage
							// in the front of buf?
							Log.i("EXG Wave", "greedy_dispatcher found " +
									searchResult[1] + " bytes of garbage " +
									"at the front");
						}
						prevPattern = searchResult[0];
						prevIdx = searchResult[1];
						// Skip this turn
					} else {
						byte[] tmpBuf = new byte[searchResult[1] - prevIdx];
						System.arraycopy(buf, prevIdx, tmpBuf, 0, searchResult[1] - prevIdx);
						handle_message(tmpBuf, prevPattern);
						prevPattern = searchResult[0];
						prevIdx = searchResult[1];
					}
					hint = prevPattern;
				}
			} while (searchResult[0] != -1);
			
			if (prevIdx == -1) {
				prevIdx = 0;
			}
			return prevIdx;
		}
		
		public void handle_one_pkt (byte[] buf, int bufLen) {
			byte[] tmpBuf;
			int remainIdx = 0, i = 0;

			if (activeBufLen > 0) {
				/* 
				 * There are leftover bytes on activeBuffer,
				 * concatenate two buffers together into
				 * activeBuffer, and then processed there
				 */ 
				if ((activeBufLen + bufLen) > activeBuffer.length) {
					tmpBuf = new byte[2 * (activeBufLen + bufLen)];
					System.arraycopy(activeBuffer, 0, tmpBuf, 0, activeBufLen);
					System.arraycopy(buf, 0, tmpBuf, activeBufLen, bufLen);
					activeBuffer = tmpBuf;
				} else {
					System.arraycopy(buf, 0, activeBuffer, activeBufLen, bufLen);
				}
				activeBufLen += bufLen;
				
				/*
				 * Process activeBuffer/activeBufLen
				 */
				remainIdx = greedy_dispatcher(activeBuffer, activeBufLen);
				for (i = 0; i < (activeBufLen - remainIdx); i++) {
					activeBuffer[i] = activeBuffer[remainIdx + i];
				}
				activeBufLen = (activeBufLen - remainIdx);
			} else {
				remainIdx = greedy_dispatcher(buf, bufLen);
				if (activeBuffer.length < (bufLen - remainIdx)) {
					activeBuffer = new byte[2 * (bufLen - remainIdx)];
				}
				System.arraycopy(buf, remainIdx, activeBuffer, 0, (bufLen - remainIdx));
				activeBufLen = bufLen - remainIdx;
			}
		}
		
		public void run() {	
			byte[] buf = new byte[128];
			byte[] printBuf;
			int bufLen = 0;
			File exgFile = new File("/sdcard/exg.data");
			FileOutputStream exgOut = null;
			
			if (!exgFile.exists()) {
				try {
					exgFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				exgOut = new FileOutputStream(exgFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/* First search for magic "EXGBT" */
			while (true) {
				if (inStream == null) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
				try {
					bufLen = inStream.read(buf);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				if (bufLen > 0) {
					//Log.i("EXG Wave", "Got " + Integer.toString(bufLen) + " bytes");
					//printBuf = new byte[bufLen];
					//System.arraycopy(buf, 0, printBuf, 0, bufLen);
					//Log.i("EXG Wave", new String(printBuf));
					try {
						exgOut.write(buf, 0, bufLen);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					handle_one_pkt(buf, bufLen);
				}
			}
		}
	};

	class BufferManagement {
		
		/* paint_list to keep track of historical buffers */
		private ArrayList<int[]> paint_list = new ArrayList<int[]>();
		
		/* number of points per buffer */
		private int per_buf_points = 0;
		
		/* 
		 * input buffer and its related state machine,
		 * to keep track of data fragments
		 */
		private byte[] head_buf = new byte[3];
		private int head_idx = 0;
		private byte[] data_buf;
		private int data_idx = 0;
		private int[] data_pool;
		
		private int screen_width = 0;
		/*
		 * FIXME: assume sample_per_sec is larger than screen_width!!!
		 */
		private int sample_per_sec = 0;		/* number of samples/sec */
		private int prev_sample_per_sec = 0;
		private int down_sample_rate = 0;	/* number of samples/shown sample */
		private int sub_sample_counter = 0;
		private int remain_points = 0;
		private int read_head_stage = 0;
		private short msg_length = 0;		
		/* the single temporary buffer */
		int[] tmp_buf = null;
		int tmp_i = 0;
		int tmp_buf_debt = 0;
		
		public BufferManagement(int width) {
			// We choose the latest header among all headers in EXG_DATA folder
			// First wait until EXG_DATA folder show up
			exgLogger.ReaderInit();
			screen_width = width;
		}
		
		public void fill_buffer(int buffer[]) {
			int i = 0;
			int paint_i = 0;
			int paint_buf_i = 0;
			int[] paint_buf = null;
			
			for (i = 0; i < buffer.length; i++) {
				buffer[i] = 0;
			}
			if (paint_list.isEmpty()) {
				//Log.i("EXG Wave", "fill buffer exit because NULL");
				return;
			}
			i = 0;
			paint_buf = paint_list.get(0);
			while (i < buffer.length) {
				if (paint_buf_i < per_buf_points) {
					buffer[i] = paint_buf[paint_buf_i];
					i++;
					paint_buf_i++;
				} else {
					paint_buf_i = 0;
					paint_i++;
					if (paint_list.size() > paint_i) {
						paint_buf = paint_list.get(paint_i);
					} else {
						return;
					}
				}
			}
		}
		
		public int get_show_buffer(int buffer[]) {	
			int remove_threshold = 0;
			
			//Log.i("EXG Wave", "get_show_buffer");
			if (per_buf_points == 0) {
				remove_threshold = 20;
			} else {
				remove_threshold = (screen_width/per_buf_points);
			}
			if (paint_list.size() > remove_threshold) {
				//Log.i("EXG Wave", "remove head buffer");
				paint_list.remove(0);
			}
			if (remain_points == 0) {
				if (pull_more_data() != 0) {
					fill_buffer(buffer);
					return -1;
				}
			}
			tmp_buf_debt++;
			while (tmp_buf_debt > 0) {
				while (tmp_i < per_buf_points) {
					/* get one sample point from data_pool */
					while (sub_sample_counter != (down_sample_rate-1)) {
						if (remain_points > 0) {
							/* skip over data_idx */
							//Log.i("EXG Wave", "skip one");
							data_idx++;
							remain_points--;
							sub_sample_counter++;
							total_read_count++;
						} else {
							if (pull_more_data() != 0) {
								fill_buffer(buffer);
								return -1;
							}
						}
					}
					if (remain_points > 0) {
						//Log.i("EXG Wave", "sample: "+data_pool[data_idx]);
						tmp_buf[tmp_i] = data_pool[data_idx];
						tmp_i++;
						sub_sample_counter = 0;
						data_idx++;
						remain_points--;
						total_read_count++;
					} else {
						if (pull_more_data() != 0) {
							fill_buffer(buffer);
							return -1;
						}
					}
				}
				paint_list.add(paint_list.size(), tmp_buf);
				tmp_buf_debt--;
				tmp_i = 0;
				tmp_buf = new int[per_buf_points];
			}
			fill_buffer(buffer);
			return 0;
		}

		private int pull_more_data() {
			int rc = 0;
			
			//Log.i("EXG Wave", "pull_more_data");
			if (remain_points != 0) {
				Log.e("EXG Wave", "should not be called when still has data");
				return -1;
			}
			if (read_head_stage == 0) {
				//Log.i("EXG Wave", "read_head_stage == 0");
				head_idx = 0;
				data_idx = 0;
				try {
					if (exgLogger.ReaderAvailable() < 3) {
						//Log.i("EXG Wave", "header not available");
						//At this point, need to search for alternative file
						exgLogger.ReaderSwitchFile();
						return -1;
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while (head_idx < 3) {
					try {
						rc = exgLogger.ReaderRead(head_buf, head_idx, (3-head_idx));
						head_idx += rc;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (head_buf[0] != 0x02) {
					Log.e("EXG Wave", "Error in exg.data: header type ne 0x02");
				}
				msg_length = (short) ((head_buf[1] & 0xff) | ((head_buf[2] & 0xff) << 8));
				data_buf = new byte[msg_length];
				read_head_stage = 1;
			}
			
			/* wait until there is at least msg_length data to read */
			try {
				if (exgLogger.ReaderAvailable() < msg_length) {
					//Log.i("EXG Wave", "msg_length not available");
					return -1;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while (data_idx < msg_length) {
				try {
					rc = exgLogger.ReaderRead(data_buf, data_idx, (msg_length-data_idx));
					data_idx += rc;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			/* Now we have data, add them to paint_list */
			//short channel_num;
			//short seq_num;
			//seq_num = (short) ((data_buf[0] & 0xff) | ((data_buf[1] & 0xff) << 8));
			//channel_num = (short) ((data_buf[2] & 0xff) | ((data_buf[3] & 0xff) << 8));
			sample_per_sec = (short) ((data_buf[4] & 0xff) | ((data_buf[5] & 0xff) << 8));
			remain_points = (msg_length-6)/2;
			read_head_stage = 0;
			data_pool = new int[remain_points];
			for (int i = 0; i < remain_points; i++) {
				data_pool[i] = (int) ((data_buf[6+i*2] & 0xff) | ((data_buf[6+i*2+1] & 0xff) << 8));
			}
			
			//差分法寻找QRS波群
			derivative.process(data_pool);
			//高通滤波消除基线漂移
			data_pool = filter.highpass(data_pool);
			
			if (prev_sample_per_sec != sample_per_sec) {
				/* 
				 * sample/sec changed, this could be the init case, 
				 * or user changed sample rate
				 */
				prev_sample_per_sec = sample_per_sec;
				paint_list.clear();
				sub_sample_counter = 0;
				tmp_i = 0;
				tmp_buf_debt = 0;
				if (sample_per_sec >= screen_width) {
					/* down sampling */
					down_sample_rate = sample_per_sec/screen_width;
					per_buf_points = (sample_per_sec/down_sample_rate)/frames_per_sec;
					
				} else {
					/* full sample */
					down_sample_rate = 1;
					per_buf_points = sample_per_sec/frames_per_sec;
					if (per_buf_points < 1) {
						per_buf_points = 1;
					}
				}
				tmp_buf = new int[per_buf_points];
				data_idx = 0;
				return -1;
			} 
			data_idx = 0;	
			return 0;
		}
	}
	
	/**
	 * 负责绘制inBuf中的数据
	 */
	class DrawThread extends Thread {
		private SurfaceView sfv;// 画板
		private Paint mPaint;// 画笔
		private int max_y = 0;
		private int max_x = 0;
		Timer timer = new Timer();
		private int [] paint_buffer = null;
		private BufferManagement buf_mgmt = null;
		private int rc;
		private int debug_i = 0;
		
	    TimerTask task = new TimerTask(){
	    	/* 
	    	 * Each run period shows a frame of data
	    	 */
	        public void run() {
	        	debug_i++;
	        	if (debug_i == frames_per_sec) {
	        		debug_i = 0;
	        		//Log.i("EXG Wave", "total_write_count: "+total_write_count+" total_read_count: "+total_read_count);
	        	}
	        	/* Initialize everything */
	        	if (max_y == 0) {
	        		max_y = sfv.getHeight();
	        		max_x = sfv.getWidth();
	        		paint_buffer = new int[max_x];
	        		buf_mgmt = new BufferManagement(max_x);
	        	}
	        	rc = buf_mgmt.get_show_buffer(paint_buffer);
	        	if (rc == -1) {
	        		//Log.i("EXG Wave", "get_show_buffer returned error");
	        	}
	        	//画在画布正中间
	        	
	        	SimpleDraw(paint_buffer);
	        }  	          
	    }; 

		public DrawThread(SurfaceView sfv, Paint mPaint) {
			this.sfv = sfv;
			this.mPaint = mPaint;
			timer.schedule(task, 1000, 1000/frames_per_sec); 
		}

		public void run() {			
			/* Everything runs in timer handler */
		}

		/**
		 * 绘制指定区域
		 * 
		 * @param start
		 *            X轴开始的位置(全屏)
		 * @param buffer
		 *            缓冲区
		 * @param rate
		 *            Y轴数据缩小的比例
		 * @param baseLine
		 *            Y轴基线
		 */
		void SimpleDraw(int[] buffer) {
			Canvas canvas = sfv.getHolder().lockCanvas(
					new Rect(0, 0, sfv.getWidth(), sfv.getHeight()));// 关键:获取画布
			canvas.drawColor(Color.BLACK);// 清除背景
			//Log.i("EXG Wave", "create "+sfv.getWidth()+" X "+sfv.getHeight());
			int y;
			int oldX = 0, oldY = 0;
			for (int i = 0; i < buffer.length; i++) {// 有多少画多少
				int x = i;
				if ((buffer[i]/* + total_y/2*/) < start_y) {
					buffer[i] = start_y;
				} else if (buffer[i] > (start_y+total_y)) {
					buffer[i] = (start_y+total_y);
				}
				y = ((buffer[i] - start_y) * max_y) / total_y;
				y = max_y - y /*- max_y/2*/;
				if (oldX == 0) {
					oldY = y;
				}
				canvas.drawLine(oldX, oldY, x, y, mPaint);
				oldX = x;
				oldY = y;
			}
			sfv.getHolder().unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像z
		}
	}
}

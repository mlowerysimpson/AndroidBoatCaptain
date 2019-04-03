package com.example.boatcaptain;

public class LM303_DATASAMPLE {
		public double []acc_data;//acceleration data in G
		public double []mag_data;//magnetometer data (Gauss)
		public double temperature;//temperature of the LM303D chip
		public double heading;//computed heading value in degrees (direction that the +X axis of the LM303D is pointed) 0 to 360
		public double pitch;//computed pitch angle in degrees (direction above horizontal that the +X axis of the LM303D is pointed -90 to 90
		public double roll;//computed roll angle in degrees (direction around +X axis that the +Y axis of the LM303D is pointed -180 to +180
		
		LM303_DATASAMPLE() {
			acc_data = new double[3];
			mag_data = new double[3];
			temperature = 0.0;
			heading = 0.0;
			pitch = 0.0;
			roll = 0.0;
		}
		
		public static int dataSize() {
			return 24+24+4*8;
		}
		
		public void setData(byte []buf) {//sets compass-related data from a byte buffer
			byte []acc_x_bytes = new byte[8];
			byte []acc_y_bytes = new byte[8];
			byte []acc_z_bytes = new byte[8];
			byte []mag_x_bytes = new byte[8];
			byte []mag_y_bytes = new byte[8];
			byte []mag_z_bytes = new byte[8];
			byte []temp_bytes = new byte[8];
			byte []heading_bytes = new byte[8];
			byte []pitch_bytes = new byte[8];
			byte []roll_bytes = new byte[8];
			for (int i=0;i<8;i++) {
				acc_x_bytes[i] = buf[i];
				acc_y_bytes[i] = buf[8+i];
				acc_z_bytes[i] = buf[16+i];
				mag_x_bytes[i] = buf[24+i];
				mag_y_bytes[i] = buf[32+i];
				mag_z_bytes[i] = buf[40+i];
				temp_bytes[i] = buf[48+i];
				heading_bytes[i] = buf[56+i];
				pitch_bytes[i] = buf[64+i];
				roll_bytes[i] = buf[72+i];
			}
			acc_data[0] = Util.toDouble(acc_x_bytes);
			acc_data[1] = Util.toDouble(acc_y_bytes);
			acc_data[2] = Util.toDouble(acc_z_bytes);
			mag_data[0] = Util.toDouble(mag_x_bytes);
			mag_data[1] = Util.toDouble(mag_y_bytes);
			mag_data[2] = Util.toDouble(mag_z_bytes);
			temperature = Util.toDouble(temp_bytes);
			heading = Util.toDouble(heading_bytes);
			pitch = Util.toDouble(pitch_bytes);
			roll = Util.toDouble(roll_bytes);
		}
		
}



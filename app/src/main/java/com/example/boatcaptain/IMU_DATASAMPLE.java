package com.example.boatcaptain;

public class IMU_DATASAMPLE {
		public double sample_time_sec;//the time of the sample in seconds
		public double []acc_data;//acceleration data in G
		public double []mag_data;//magnetometer data (Gauss)
		public double []angular_rate;//angular rate (deg/sec)
		public double mag_temperature;//temperature of the LIS3MDL chip
		public double acc_gyro_temperature;//temperature of the LSM6DS33 chip
		public double heading;//computed heading value in degrees (direction that the +X axis of the IMU is pointed) 0 to 360
		public double pitch;//computed pitch angle in degrees (direction above horizontal that the +X axis of the IMU is pointed -90 to 90
		public double roll;//computed roll angle in degrees (direction around +X axis that the +Y axis of the IMU is pointed -180 to +180
		
		IMU_DATASAMPLE() {
			acc_data = new double[3];
			mag_data = new double[3];
			angular_rate = new double[3];
			mag_temperature = 0.0;
			acc_gyro_temperature = 0.0;
			sample_time_sec = 0.0;
			heading = 0.0;
			pitch = 0.0;
			roll = 0.0;
		}
		
		public static int dataSize() {
			return 24+24+24+6*8;
		}
		
		public void setData(byte []buf) {//sets compass-related data from a byte buffer
			byte []sample_time_bytes = new byte[8];
			byte []acc_x_bytes = new byte[8];
			byte []acc_y_bytes = new byte[8];
			byte []acc_z_bytes = new byte[8];
			byte []mag_x_bytes = new byte[8];
			byte []mag_y_bytes = new byte[8];
			byte []mag_z_bytes = new byte[8];
			byte []gyro_x_bytes = new byte[8];
			byte []gyro_y_bytes = new byte[8];
			byte []gyro_z_bytes = new byte[8];
			byte []mag_temp_bytes = new byte[8];
			byte []acc_gyro_temp_bytes = new byte[8];
			byte []heading_bytes = new byte[8];
			byte []pitch_bytes = new byte[8];
			byte []roll_bytes = new byte[8];
			for (int i=0;i<8;i++) {
				sample_time_bytes[i] = buf[i];
				acc_x_bytes[i] = buf[8+i];
				acc_y_bytes[i] = buf[16+i];
				acc_z_bytes[i] = buf[24+i];
				mag_x_bytes[i] = buf[32+i];
				mag_y_bytes[i] = buf[40+i];
				mag_z_bytes[i] = buf[48+i];
				gyro_x_bytes[i] = buf[56+i];
				gyro_y_bytes[i] = buf[64+i];
				gyro_z_bytes[i] = buf[72+i];
				mag_temp_bytes[i] = buf[80+i];
				acc_gyro_temp_bytes[i] = buf[88+i];
				heading_bytes[i] = buf[96+i];
				pitch_bytes[i] = buf[104+i];
				roll_bytes[i] = buf[112+i];
			}
			acc_data[0] = Util.toDouble(acc_x_bytes);
			acc_data[1] = Util.toDouble(acc_y_bytes);
			acc_data[2] = Util.toDouble(acc_z_bytes);
			mag_data[0] = Util.toDouble(mag_x_bytes);
			mag_data[1] = Util.toDouble(mag_y_bytes);
			mag_data[2] = Util.toDouble(mag_z_bytes);
			angular_rate[0] = Util.toDouble(gyro_x_bytes);
			angular_rate[1] = Util.toDouble(gyro_y_bytes);
			angular_rate[2] = Util.toDouble(gyro_z_bytes);
			mag_temperature = Util.toDouble(mag_temp_bytes);
			acc_gyro_temperature = Util.toDouble(acc_gyro_temp_bytes);
			heading = Util.toDouble(heading_bytes);
			pitch = Util.toDouble(pitch_bytes);
			roll = Util.toDouble(roll_bytes);
		}
		
}



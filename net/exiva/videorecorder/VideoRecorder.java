package net.exiva.videorecorder;

import danger.app.Application;
import danger.app.AppResources;
import danger.app.Event;
import danger.app.EventType;
import danger.app.Timer;

import danger.audio.Meta;

import danger.storage.MountPoint;
import danger.storage.StorageManager;

import danger.ui.AlertWindow;
import danger.ui.MarqueeAlert;
import danger.ui.NotificationManager;
import danger.ui.Pen;
import danger.ui.SplashScreen;
import danger.ui.StaticTextBox;
import danger.ui.View;

import danger.system.Hardware;
import danger.system.CameraDevice;

import danger.util.format.DateFormat;

import danger.video.Recorder;
import danger.video.VideoException;
import danger.video.VideoManager;

import java.io.File;

import java.lang.System;

import java.util.Date;

import danger.util.DEBUG;

public class VideoRecorder extends Application implements Resources, Commands {
	public SplashScreen mSplashScreen;
	public static AlertWindow aError;
	public static Date date;
	public static Event tmp;
	public static int videoNumber;
	public static int status;
	public static Recorder rec;
	public static String sDate;
	public static Timer mRecBlinkTimer;
	public static videoView mWindow;
	private static boolean isRecording = false, mIsAppForeground;
	private static StaticTextBox sFreeSpace;
 	static final int kCmd_RecordingDone = 1;
	MarqueeAlert mMarquee;
	static {
		try {
			rec = Recorder.getInstance(null);
		} catch (VideoException e) {} //DEBUG.p("Exception: "+e); }
	}

	public VideoRecorder() {
		mWindow = videoView.create();
		mWindow.show();
		rec.setCaptureView(mWindow.vview);
		aError = Application.getCurrentApp().getResources().getAlert(ID_ERROR, this);
		mMarquee = new MarqueeAlert("null", getResources().getBitmap(ID_MARQUEE),1);
		mSplashScreen = getResources().getSplashScreen(AppResources.ID_SPLASH_SCREEN_RESOURCE);
		sFreeSpace = (StaticTextBox) mSplashScreen.getDescendantWithID(ID_TIME_LEFT);
		StorageManager.registerStorageDeviceListener(this);
		VideoManager.registerForVideoEvents(this);
		mRecBlinkTimer = new Timer(500, true, this, 1);
	}

	public void launch() {
		updateTime();
	}

	public void resume() {
		rec.startPreview();
		updateTime();
		mIsAppForeground=true;
	}

	public void suspend() {
		rec.stopPreview();
		updateTime();
		if (isRecording == true) {
			stop();
		}
		mIsAppForeground=false;
	}

	public void quit() {
		StorageManager.unregisterStorageDeviceListener(this);
		VideoManager.unregisterForVideoEvents(this);
	}

	public void renderSplashScreen(View inView, Pen inPen) {
		mSplashScreen.paint(inView, inPen);
	}

	public static void record() {
		if (!rec.isRecordingActive()) {
			try { rec.setMaxDuration(50000);
			} catch (IllegalStateException d) {} //DEBUG.p("Caught! "+d); }
			String[] storage;
			storage = StorageManager.getRemovablePaths();
			if(storage.length > 0) {
				date = new Date();
				sDate = DateFormat.withFormat("MMddyyyy_hhmmss", date);
				rec.startRecording(storage[0]+"/DCIM/Video_"+sDate+".3gp", new Event(Application.getCurrentApp(), kCmd_RecordingDone));
				mRecBlinkTimer.start();
				isRecording=true;
			} else {
				aError.setMessage("Error: This error should never happen...\nCheck your SD card.");
				aError.show();
			}
		} else if (rec.isRecordingActive()) {
			stop();
			mRecBlinkTimer.stop();
		}
	}

	public static void stop() {
		if (rec.isRecordingActive()) {
			rec.stopRecording();
		}
	}

	public static void pause() {
		if (!rec.isRecordingPaused()) {
			rec.pauseRecording();
			mWindow.setState(2);
			mRecBlinkTimer.stop();
		} else if (rec.isRecordingPaused()) {
			rec.resumeRecording();
			mWindow.setState(1);
			mRecBlinkTimer.start();
		}
	}

	public void updateTime() {
		if (status==1) {
			sFreeSpace.setText("No Storage Device Found");
			mWindow.setSubTitle("No Storage Device Found");
		} else {
			String[] storage;
			storage = StorageManager.getRemovablePaths();
			if(storage.length > 0) {
				int sTime = rec.getAvailableFileRecordSeconds(StorageManager.findMountByPath(storage[0]));
				String time = tiemToString(rec.getAvailableFileRecordSeconds(StorageManager.findMountByPath(storage[0])));
				if (sTime <= 600) {
					mWindow.setState(3);
					sFreeSpace.setText("Storage Device Full");
					mWindow.setSubTitle("Storage Device Full");
				} else {
					mWindow.setState(0);
					sFreeSpace.setText(time+" remaining");
					mWindow.setSubTitle(time+" remaining");
				}
			} else {
				mWindow.setState(3);
				sFreeSpace.setText("No Storage Device Found");
				mWindow.setSubTitle("No Storage Device Found");
			}
		}
		updatePreviewScreen();
	}

	public static String tiemToString(int lSeconds) {
		if (lSeconds >= 0) {
			int	lMinutes = lSeconds / 60;
			lSeconds %= 60;
			int	lHours = lMinutes / 60;
			lMinutes %= 60;
			return ((lHours < 10 ? "0" : "") + lHours
			+ ":" + (lMinutes < 10 ? "0" : "") + lMinutes
			+ ":" + (lSeconds< 10 ? "0" : "") + lSeconds );
			} else {
				return "00:00:00";
		}
	}

	public static void abort() {
		rec.abortRecording();
		mWindow.updateTime(0);
		rec.getCameraDevice().setPowerState(CameraDevice.POWER_ON);
		rec.startPreview();
	}

	public boolean receiveEvent(Event e) {
		switch (e.getType()) {
			case Event.EVENT_TIMER: {
				if (e.data==1) {
					mWindow.recBlink();
					mWindow.updateTime(rec.getCurrentDuration()/1000);
				}
			return true;
			}
			case kCmd_RecordingDone: {
				switch (e.getWhat()) {
					case Recorder.FILE_STATUS:
						if (e.data == Recorder.Errors.OK) {
							mMarquee.setText("Video Saved!");
							NotificationManager.marqueeAlertNotify(mMarquee);
							Meta.play(Meta.BEEP_ACTION_SUCCESS);
						}
						if (e.data != Recorder.Errors.OK) {
							mMarquee.setText("Video did not save.");
							NotificationManager.marqueeAlertNotify(mMarquee);
							NotificationManager.playErrorSound();
							//delete the failed file since the OS fails to.
							String[] storage;
							storage = StorageManager.getRemovablePaths();
							if(storage.length > 0) {
								File f = new File(storage[0]+"/DCIM/Video_"+sDate+".3gp");
								f.delete();
							}
							switch (e.data) {
								case Recorder.Errors.ABORTED:
								break;
								case Recorder.Errors.FILE_SYSTEM_FULL:
								aError.setMessage("The SD card inserted is full. Remove some items and try to record again.");
								aError.show();
								break;
								case Recorder.Errors.HARDWARE_FAILED:
								aError.setMessage("There was an unkown hardware error. If this persists, contact support.");
								aError.show();
								break;
								case Recorder.Errors.HARDWARE_IN_USE:
								aError.setMessage("The camera is in use.");
								aError.show();							
								break;
								case Recorder.Errors.IO_FAILED:
								aError.setMessage("There was an unknown Filesystem error. If this persists, contact support.");
								aError.show();
								break;
								case Recorder.Errors.ON_PHONE:
								aError.setMessage("Cannot record while phone is in use. Please hang up and try again.");
								aError.show();
								break;
								case Recorder.Errors.OUT_OF_MEMORY:
								aError.setMessage("The device is out of memory. Try removing some Applications or Rebooting.");
								aError.show();
								break;
								case Recorder.Errors.WRITE_SUSPENDED_ON_FULL:
								aError.setMessage("The video was not saved because SD card inserted is full. Remove some items and try to record again.");
								aError.show();
								break;
								case Recorder.Errors.WRITE_SUSPENDED_ON_IO_ERROR:
								aError.setMessage("The video was not saved due to an unknown Filesystem error. If this persists, contact support.");
								aError.show();
								break;
							}
						}
					return true;
					case Recorder.RECORDING_FINISHED:
					isRecording = false;
					mRecBlinkTimer.stop();
						if (e.data == Recorder.Errors.OK) {
							mWindow.setState(0);
							mWindow.updateTime(0);
							//restart the camera.
							rec.getCameraDevice().setPowerState(CameraDevice.POWER_ON);
						}
						if (e.data != Recorder.Errors.OK) {
							rec.stopRecording();
							mWindow.setState(0);
							rec.startPreview();
							switch (e.data) {
								case Recorder.Errors.ABORTED:
								break;
								case Recorder.Errors.FILE_SYSTEM_FULL:
								aError.setMessage("The SD card inserted is full. Remove some items and try to record again.");
								aError.show();
								break;
								case Recorder.Errors.HARDWARE_FAILED:
								aError.setMessage("There was an unkown hardware error. If this persists, contact support.");
								aError.show();
								break;
								case Recorder.Errors.HARDWARE_IN_USE:
								aError.setMessage("The camera is in use.");
								aError.show();
								break;
								case Recorder.Errors.IO_FAILED:
								aError.setMessage("There was an unknown Filesystem error. If this persists, contact support.");
								aError.show();
								break;
								case Recorder.Errors.ON_PHONE:
								aError.setMessage("Cannot record while phone is in use. Please hang up and try again.");
								aError.show();
								break;
								case Recorder.Errors.OUT_OF_MEMORY:
								aError.setMessage("The device is out of memory. Try removing some Applications or Rebooting.");
								aError.show();
								break;
								case Recorder.Errors.WRITE_SUSPENDED_ON_FULL:
								aError.setMessage("The video was not saved because SD card inserted is full. Remove some items and try to record again.");
								aError.show();
								break;
								case Recorder.Errors.WRITE_SUSPENDED_ON_IO_ERROR:
								aError.setMessage("The video was not saved due to an unknown Filesystem error. If this persists, contact support.");
								aError.show();
								break;
							}
						}
					return true;
				}
			}
			break;
			default:
			break;
		}
		switch (e.what) {
			case EventType.WHAT_STORAGE_DEVICE_MOUNTED: {
				mWindow.setState(0);
				status=0;
				updateTime();
				return true;
			}
			case EventType.WHAT_STORAGE_DEVICE_UNMOUNTED: {
				mWindow.setState(3);
				status=1;
				updateTime();
				return true;
			}
			case EventType.WHAT_STORAGE_DEVICE_MODIFIED: {
				updateTime();
				return true;
			}
		}
		return super.receiveEvent(e);
	}
}
package net.exiva.videorecorder;

import danger.app.Application;
import danger.app.Event;

import danger.ui.ScreenWindow;
import danger.ui.AlertWindow;
import danger.ui.Button;
import danger.ui.ImageView;
import danger.ui.MenuItem;
import danger.ui.StaticText;
import danger.ui.VideoCaptureView;
import danger.ui.Menu;
import danger.ui.DialogWindow;

import danger.util.DEBUG;

public class videoView extends ScreenWindow implements Resources, Commands {
	AlertWindow vAbortAlert;
	Button record, pause, abort, stop;
	ImageView screenRecord, screenPause, screenStop;
	MenuItem menuRecord, menuPause, menuStop, menuAbort;
	StaticText recording, paused, stopped, time;
	VideoCaptureView vview;
	public static boolean recShowing;
	public static int recstate;

	public videoView() {
		vview = new VideoCaptureView(VideoRecorder.rec, 176, 144);
		vview.setPosition(112,18);
		addChild(vview);
		vview.show();
		vAbortAlert = getApplication().getAlert(abortAlert, this);
	}

	public static videoView create() {
		videoView me = (videoView) Application.getCurrentApp().getResources().getScreen(ID_MAIN_SCREEN, null);
		return me;
	}

	public void onDecoded() {
		record = (Button)this.getDescendantWithID(vRecordButton);
		stop = (Button)this.getDescendantWithID(vStopButton);
		pause = (Button)this.getDescendantWithID(vPauseButton);
		abort = (Button)this.getDescendantWithID(vAbortButton);
		recording = (StaticText)this.getDescendantWithID(vRecordText);
		paused = (StaticText)this.getDescendantWithID(vPausedText);
		stopped = (StaticText)this.getDescendantWithID(vStoppedText);
		time = (StaticText)this.getDescendantWithID(vTimeStatic);
		screenRecord = (ImageView)this.getDescendantWithID(vRecordIcon);
		screenPause = (ImageView)this.getDescendantWithID(vPausedIcon);
		screenStop = (ImageView)this.getDescendantWithID(vStoppedIcon);
		setState(0);
		recstate=0;
	}
	
	public boolean blocksAutomaticKeyGuard() {
		return true;
	}
	
	public boolean blocksKeyGuard() {
		return true;
	}

	public void recBlink() {
		if (!recShowing) {
			recording.setVisible(true);
			screenRecord.setVisible(true);
			recShowing=true;
		} else {
			recording.setVisible(false);
			screenRecord.setVisible(false);
			recShowing=false;			
		}
	}

	public void recClear() {
		recording.setVisible(false);
		screenRecord.setVisible(false);
		recShowing=false;
	}
	
	public void updateTime(int tiem) {
		if (tiem < 10) {
			time.setText(" "+Integer.toString(tiem)+" sec / 50 sec ");
		} else {
			time.setText(Integer.toString(tiem)+" sec / 50 sec ");
		}
		
	}
	
	public final void adjustActionMenuState(Menu menu) {
		menu.removeAllItems();
		menu.addFromResource(Application.getCurrentApp().getResources(), ID_MAIN_MENU, this);
		menuRecord = menu.getItemWithID(vMenuRecord);
		menuPause = menu.getItemWithID(vMenuPause);
		menuStop = menu.getItemWithID(vMenuStop);
		menuAbort = menu.getItemWithID(vMenuAbort);
		if (recstate==0) {
			menuPause.setTitle("Pause");
			menuRecord.enable();
			menuPause.disable();
			menuStop.disable();
			menuAbort.disable();
		} else if (recstate==1) {
			menuPause.setTitle("Pause");
			menuRecord.disable();
			menuPause.enable();
			menuAbort.enable();
			menuStop.enable();
		} else if (recstate==2) {
			menuPause.setTitle("Resume");
			menuRecord.disable();
			menuPause.enable();
			menuAbort.enable();
			menuStop.enable();
		} else if (recstate==3) { 
			menuPause.setTitle("Pause");
			menuRecord.disable();
			menuPause.disable();
			menuStop.disable();
			menuAbort.disable();
		}
    }

 	public void setState(int state) {
		switch (state) {
			case 0:
				pause.setTitle("Pause");
				record.enable();
				pause.disable();
				stop.disable();
				abort.disable();
				setFocusedChild(record);
				stopped.setVisible(true);
				paused.setVisible(false);
				recording.setVisible(false);
				screenRecord.setVisible(false);
				screenPause.setVisible(false);
				screenStop.setVisible(true);
				recstate=0;
				break;
			case 1:
				pause.setTitle("Pause");
				record.disable();
				pause.enable();
				stop.enable();
				abort.enable();
				setFocusedChild(stop);
				stopped.setVisible(false);
				paused.setVisible(false);
				recording.setVisible(true);
				screenRecord.setVisible(true);
				screenPause.setVisible(false);
				screenStop.setVisible(false);
				recstate=1;
				break;
			case 2:
				pause.setTitle("Resume");
				stopped.setVisible(false);
				paused.setVisible(true);
				recording.setVisible(false);
				screenRecord.setVisible(false);
				screenPause.setVisible(true);
				screenStop.setVisible(false);
				recstate=2;
				break;
			case 3:
				pause.setTitle("Pause");
				record.disable();
				pause.disable();
				stop.disable();
				abort.disable();
				stopped.setVisible(true);
				paused.setVisible(false);
				recording.setVisible(false);
				screenRecord.setVisible(false);
				screenPause.setVisible(false);
				screenStop.setVisible(true);
				recstate=3;
				break;
		}
	}
	
	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case EVENT_START_RECORDING: {
				VideoRecorder.record();
				setState(1);
				return false;
			}
			case EVENT_STOP_RECORDING: {
				VideoRecorder.stop();
				setState(0);
				return false;
			}
			case EVENT_PAUSE_RECORDING: {
				VideoRecorder.pause();
				return false;
			}
			case EVENT_ABORT_RECORDING: {
				vAbortAlert.show();
				return false;
			}
			case EVENT_ABORTED: {
				VideoRecorder.abort();
				setState(0);
				return false;
			}
			case EVENT_ABOUT: {
				AlertWindow about = getApplication().getAlert(ID_ABOUT, this);
				about.show();
				return true;
			}
			case EVENT_TIPS: {
				DialogWindow tips = getApplication().getDialog(helpDialog, this);
				tips.show();
				return true;
			}
			default:
			break;
		}
		return super.receiveEvent(e);
	}

	public boolean eventWidgetUp(int inWidget, Event e) {
		switch (inWidget) {
			case Event.DEVICE_BUTTON_CANCEL:
				if (recstate==0 || recstate==3) {
					Application.getCurrentApp().returnToLauncher();
				} else if (recstate==1 || recstate==2) {
					vAbortAlert.show();
				}
			return true;
			case Event.DEVICE_BUTTON_BACK:
			Application.getCurrentApp().returnToLauncher();
			return true;
		}
		return super.eventWidgetUp(inWidget, e);
	}
} //NOM NOM NOM
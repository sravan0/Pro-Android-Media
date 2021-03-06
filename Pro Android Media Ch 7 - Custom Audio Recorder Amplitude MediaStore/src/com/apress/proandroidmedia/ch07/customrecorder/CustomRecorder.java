package com.apress.proandroidmedia.ch07.customrecorder;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

public class CustomRecorder extends Activity implements OnClickListener,
		OnCompletionListener {

	TextView statusTextView, amplitudeTextView;
	Button startRecording, stopRecording, playRecording, finishButton;
	MediaRecorder recorder;
	MediaPlayer player;
	File audioFile;

	RecordAmplitude recordAmplitude;
	boolean isRecording = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		statusTextView = (TextView) this.findViewById(R.id.StatusTextView);
		statusTextView.setText("Ready");

		amplitudeTextView = (TextView) this
				.findViewById(R.id.AmplitudeTextView);
		amplitudeTextView.setText("0");

		stopRecording = (Button) this.findViewById(R.id.StopRecording);
		startRecording = (Button) this.findViewById(R.id.StartRecording);
		playRecording = (Button) this.findViewById(R.id.PlayRecording);
		finishButton = (Button) this.findViewById(R.id.FinishButton);

		startRecording.setOnClickListener(this);
		stopRecording.setOnClickListener(this);
		playRecording.setOnClickListener(this);
		finishButton.setOnClickListener(this);

		stopRecording.setEnabled(false);
		playRecording.setEnabled(false);
	}

	public void onClick(View v) {
		if (v == finishButton) {
			finish();
		} else if (v == stopRecording) {
			isRecording = false;
			recordAmplitude.cancel(true);

			recorder.stop();
			recorder.release();

			ContentValues contentValues = new ContentValues();
			contentValues
					.put(MediaStore.MediaColumns.TITLE, "This Isn't Music");
			contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System
					.currentTimeMillis());
			contentValues.put(MediaStore.Audio.Media.DATA, audioFile
					.getAbsolutePath());
			Uri newUri = getContentResolver().insert(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);

			player = new MediaPlayer();
			player.setOnCompletionListener(this);
			try {
				player.setDataSource(audioFile.getAbsolutePath());
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(
						"Illegal Argument to MediaPlayer.setDataSource", e);
			} catch (IllegalStateException e) {
				throw new RuntimeException(
						"Illegal State in MediaPlayer.setDataSource", e);
			} catch (IOException e) {
				throw new RuntimeException(
						"IOException in MediaPalyer.setDataSource", e);
			}

			try {
				player.prepare();
			} catch (IllegalStateException e) {
				throw new RuntimeException(
						"IllegalStateException in MediaPlayer.prepare", e);
			} catch (IOException e) {
				throw new RuntimeException(
						"IOException in MediaPlayer.prepare", e);
			}

			statusTextView.setText("Ready to Play");

			playRecording.setEnabled(true);
			stopRecording.setEnabled(false);
			startRecording.setEnabled(true);

		} else if (v == startRecording) {
			recorder = new MediaRecorder();

			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

			File path = new File(
					Environment.getExternalStorageDirectory().getAbsolutePath()
							+ "/Android/data/com.apress.proandroidmedia.ch07.customrecorder/files/");
			path.mkdirs();
			try {
				audioFile = File.createTempFile("recording", ".3gp", path);
			} catch (IOException e) {
				throw new RuntimeException(
						"Couldn't create recording audio file", e);
			}
			recorder.setOutputFile(audioFile.getAbsolutePath());

			try {
				recorder.prepare();
			} catch (IllegalStateException e) {
				throw new RuntimeException(
						"IllegalStateException on MediaRecorder.prepare", e);
			} catch (IOException e) {
				throw new RuntimeException(
						"IOException on MediaRecorder.prepare", e);
			}
			recorder.start();

			isRecording = true;
			recordAmplitude = new RecordAmplitude();
			recordAmplitude.execute();

			statusTextView.setText("Recording");

			playRecording.setEnabled(false);
			stopRecording.setEnabled(true);
			startRecording.setEnabled(false);
		} else if (v == playRecording) {
			player.start();
			statusTextView.setText("Playing");
			playRecording.setEnabled(false);
			stopRecording.setEnabled(false);
			startRecording.setEnabled(false);
		}
	}

	public void onCompletion(MediaPlayer mp) {
		playRecording.setEnabled(true);
		stopRecording.setEnabled(false);
		startRecording.setEnabled(true);
		statusTextView.setText("Ready");
	}

	private class RecordAmplitude extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			while (isRecording) {

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				publishProgress(recorder.getMaxAmplitude());
			}
			return null;
		}

		protected void onProgressUpdate(Integer... progress) {
			amplitudeTextView.setText(progress[0].toString());
		}
	}
}
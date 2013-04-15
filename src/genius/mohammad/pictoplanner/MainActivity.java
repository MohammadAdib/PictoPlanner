package genius.mohammad.pictoplanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class MainActivity extends Activity implements OnItemClickListener {

	private int index = 0;
	private Uri uri;
	final static private String APP_KEY = "6471m5c5exxjfcf";
	final static private String APP_SECRET = "38wfde103yl5b8l";
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
	private DropboxAPI<AndroidAuthSession> mDBApi;
	static final int CAPTURE_IMAGE = 0;
	private boolean progressDone = true;
	private int progressBarStatus = 0;
	private boolean sharing = false;
	private Handler progressBarHandler = new Handler();
	private ProgressDialog progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
		String action = intent.getAction();
		Log.d("Share", action);
		ImageView imageView = (ImageView) findViewById(R.id.imageViewDB2);
		imageView.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				try {
					Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android");
					startActivity(LaunchIntent);
				} catch (Exception e) {
					Toast.makeText(MainActivity.this, "Dropbox application not installed", Toast.LENGTH_SHORT).show();
				}
			}

		});
		// List
		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
		// Storage
		CheckBox dropboxCB = (CheckBox) findViewById(R.id.checkBoxDropbox);
		final CheckBox checkBoxG = (CheckBox) findViewById(R.id.checkBoxGallery);
		checkBoxG.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("gallery", isChecked);
				editor.commit();
			}
		});
		dropboxCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && mDBApi == null) {
					// Dropbox
					AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
					AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
					mDBApi = new DropboxAPI<AndroidAuthSession>(session);
					AccessTokenPair access = getStoredKeys();
					mDBApi.getSession().setAccessTokenPair(access);
				}
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("sync", isChecked);
				editor.commit();
				checkBoxG.setEnabled(isChecked);
				if (!isChecked)
					checkBoxG.setChecked(!isChecked);
			}
		});
		// Read schedule
		refreshList();

		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action)) {
			Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			uri = Uri.fromFile(new File(getRealPathFromURI(imageUri)));
			Log.d("URI", uri.getPath());
			sharing = true;
			dropboxCB.setEnabled(false);
			dropboxCB.setChecked(true);
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		@SuppressWarnings("deprecation")
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	private void refreshList() {
		ListView listView = (ListView) findViewById(R.id.listView);
		if (getSchedule().equals("")) {
			listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] { "Add Classes" }));
		} else {
			String[] classes = getSchedule().split("\n");
			listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, classes));
		}
		CheckBox dropboxCB = (CheckBox) findViewById(R.id.checkBoxDropbox);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		dropboxCB.setChecked(prefs.getBoolean("sync", false));
		CheckBox checkBoxG = (CheckBox) findViewById(R.id.checkBoxGallery);
		checkBoxG.setEnabled(prefs.getBoolean("sync", false));
		checkBoxG.setChecked(prefs.getBoolean("gallery", false));
		if (!prefs.getBoolean("sync", false)) {
			checkBoxG.setChecked(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = new Intent(MainActivity.this, EditActivity.class);
		startActivity(i);
		return true;
	}

	private String getSchedule() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		return prefs.getString("classes", "");
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CAPTURE_IMAGE:
			try {
				if (resultCode == Activity.RESULT_OK) {
					Runnable r = new Runnable() {
						public void run() {
							galleryAddPic();
							saveFileToDropbox();
						}
					};
					new Thread(r).start();
				} else {
					new File(uri.getPath()).delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void progressBar() {
		runOnUiThread(new Runnable() {

			public void run() {
				// prepare for a progress bar dialog
				progressBar = new ProgressDialog(MainActivity.this);
				progressBar.setCancelable(true);
				ListView listView = (ListView) findViewById(R.id.listView);
				progressBar.setMessage("Uploading picture to Dropbox/School/" + listView.getItemAtPosition(index).toString() + "/...");
				progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressBar.setProgress(0);
				progressBar.setMax(100);
				progressBar.show();

				// reset progress bar status
				progressBarStatus = 0;
			}

		});

		new Thread(new Runnable() {
			public void run() {
				while (!progressDone) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Update the progress bar
					progressBarHandler.post(new Runnable() {
						public void run() {
							progressBar.setProgress(progressBarStatus);
						}
					});
				}
				progressBarHandler.post(new Runnable() {
					public void run() {
						progressBar.setProgress(100);
					}
				});
				// sleep 2 seconds, so that you can see the 100%
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				runOnUiThread(new Runnable() {
					public void run() {
						progressBar.dismiss();
						finish();
					}
				});
			}
		}).start();
	}

	private void saveFileToDropbox() {
		progressDone = false;
		progressBar();
		CheckBox cb = (CheckBox) findViewById(R.id.checkBoxDropbox);
		if (cb.isChecked()) {
			// Uploading content.
			FileInputStream inputStream = null;
			try {
				File file = new File(uri.getPath());
				inputStream = new FileInputStream(file);
				ListView listView = (ListView) findViewById(R.id.listView);
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
				String dbPath = "/School/" + listView.getItemAtPosition(index).toString() + "/" + "/IMG_" + timeStamp + ".jpg";
				UploadRequest mRequest = mDBApi.putFileOverwriteRequest(dbPath, inputStream, file.length(), new ProgressListener() {
					@Override
					public long progressInterval() {
						return 100;
					}

					@Override
					public void onProgress(final long bytes, final long total) {
						MainActivity.this.progressBarStatus = (int) (100.0 * (double) bytes / (double) total);
					}
				});

				mRequest.upload();
				progressDone = true;
			} catch (DropboxUnlinkedException e) {
				// User has unlinked, ask them to link again here.
				Log.e("PictoPlanner", "User has unlinked.");
			} catch (DropboxException e) {
				Log.e("PictoPlanner", "Something went wrong while uploading.");
			} catch (FileNotFoundException e) {
				Log.e("PictoPlanner", "File not found.");
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(uri);
		this.sendBroadcast(mediaScanIntent);
	}

	@SuppressLint("SimpleDateFormat")
	private Uri createImageFile() {
		try {
			// Create an image file name
			ListView listView = (ListView) findViewById(R.id.listView);
			String className = listView.getItemAtPosition(index).toString();
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PictoPlanner/" + className;
			new File(dir).mkdirs();
			String imageFileName = "IMG_" + timeStamp;
			File image = File.createTempFile(imageFileName, ".jpg", new File(dir));
			return Uri.fromFile(image);
		} catch (Exception e) {

		}
		return null;
	}

	private void copyImageFile() {
		File src = new File(uri.getPath());
		// Create an image file name
		ListView listView = (ListView) findViewById(R.id.listView);
		String className = listView.getItemAtPosition(index).toString();
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PictoPlanner/" + className;
		new File(dir).mkdirs();
		String imageFileName = "IMG_" + timeStamp;
		String filenameArray[] = uri.getPath().split("\\.");
		String extension = filenameArray[filenameArray.length - 1];
		File dst = new File(dir + "/" + imageFileName + "." + extension);
		uri = Uri.fromFile(dst);
		InputStream in;
		try {
			in = new FileInputStream(src);

			OutputStream out = new FileOutputStream(dst);
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void storeKeys(String key, String secret) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		// write keys
		editor.putString("key", key);
		editor.putString("secret", secret);
		editor.commit();
	}

	private AccessTokenPair getStoredKeys() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		Log.d("PictoPlanner", "STORED KEY: " + prefs.getString("key", "nothing"));
		if (prefs.getString("key", "nothing").equals("nothing")) {
			mDBApi.getSession().startAuthentication(this);
		}
		return new AccessTokenPair(prefs.getString("key", ""), prefs.getString("secret", ""));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mDBApi != null) {
			if (mDBApi.getSession().authenticationSuccessful()) {
				try {
					// MANDATORY call to complete auth.
					// Sets the access token on the session
					mDBApi.getSession().finishAuthentication();
					AccessTokenPair tokens = mDBApi.getSession().getAccessTokenPair();
					storeKeys(tokens.key, tokens.secret);
				} catch (IllegalStateException e) {
					Log.i("DbAuthLog", "Error authenticating", e);
				}
			}
		}
		refreshList();
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
		if (!getSchedule().equals("")) {
			this.index = index;
			if (sharing) {
				Runnable r = new Runnable() {
					public void run() {
						copyImageFile();
						galleryAddPic();
						saveFileToDropbox();
					}
				};
				new Thread(r).start();
			} else {
				uri = createImageFile();
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
				startActivityForResult(takePictureIntent, CAPTURE_IMAGE);
			}
		} else {
			Intent i = new Intent(MainActivity.this, EditActivity.class);
			startActivity(i);
		}
	}
}

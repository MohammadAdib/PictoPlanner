package genius.mohammad.pictoplanner;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class EditActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editlayout);
		ImageView imageView = (ImageView) findViewById(R.id.imageViewDB1);
		imageView.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				try {
					Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android");
					startActivity(LaunchIntent);
				} catch (Exception e) {
					Toast.makeText(EditActivity.this, "Dropbox application not installed", Toast.LENGTH_SHORT).show();
				}
			}

		});
		String[] classes = getSchedule().split("\n");
		EditText[] ets = new EditText[] { (EditText) findViewById(R.id.EditText01), (EditText) findViewById(R.id.EditText02), (EditText) findViewById(R.id.EditText03), (EditText) findViewById(R.id.EditText04), (EditText) findViewById(R.id.EditText05), (EditText) findViewById(R.id.EditText06), (EditText) findViewById(R.id.EditText07), };
		CheckBox cpp0 = (CheckBox) findViewById(R.id.checkBox2);
		CheckBox cbTut = (CheckBox) findViewById(R.id.checkBox1);
		for (String c : classes) {
			try {
				if (c.startsWith("0")) {
					cpp0.setChecked(true);
				} else if (c.startsWith("T")) {
					cbTut.setChecked(true);
				}
				ets[Integer.parseInt(c.substring(0, 1)) - 1].setText(c.substring(2).trim());
			} catch (Exception e) {

			}
		}
	}

	private String getSchedule() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		return prefs.getString("classes", "");
	}

	public void save(View v) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		String classes = "";
		CheckBox cpp0 = (CheckBox) findViewById(R.id.checkBox2);
		if (cpp0.isChecked()) {
			classes = "0 Period\n";
		}
		EditText[] ets = new EditText[] { (EditText) findViewById(R.id.EditText01), (EditText) findViewById(R.id.EditText02), (EditText) findViewById(R.id.EditText03), (EditText) findViewById(R.id.EditText04), (EditText) findViewById(R.id.EditText05), (EditText) findViewById(R.id.EditText06), (EditText) findViewById(R.id.EditText07), };
		for (int i = 0; i < ets.length; i++) {
			if (ets[i].getText().toString().length() > 0)
				classes += i + 1 + ") " + ets[i].getText().toString() + "\n";
		}
		CheckBox cbTut = (CheckBox) findViewById(R.id.checkBox1);
		if (cbTut.isChecked()) {
			classes += "Tutorial";
		}
		editor.putString("classes", classes);
		editor.commit();
		finish();
	}

	public void cancel(View v) {
		finish();
	}
}

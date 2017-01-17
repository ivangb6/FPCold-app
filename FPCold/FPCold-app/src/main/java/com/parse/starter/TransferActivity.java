package com.parse.starter;

import android.IntentIntegrator;
import android.IntentResult;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import sapphire.StorageListView;

public class TransferActivity extends AppCompatActivity implements View.OnClickListener{

	// Environment variables
	final private Context context = this;
	final private Activity activity = this;

	// Variables for the UI
	private RelativeLayout RL;
	private TextView instructions;
	private EditText pallet_tag, old_location, new_location;
	private ImageButton first_image, second_image, third_image;

	// Variables for ListView setup
	private ListView lv;
	private StorageListView SA;
	private ArrayList<String> pallets;
	private ArrayList<String> locations;

	// Variables to store virtual storage information
	private ArrayList<String> virtual_tags;
	private ArrayList<String> virtual_ids;

	// Supporting flag for calling the external Barcode Scanner app
	private int flag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transfer);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		setTitle("TRANSFER");

		// Initialization of all variables
		RL = (RelativeLayout)findViewById(R.id.transferRelativeLayout);
		instructions = (TextView)findViewById(R.id.transferInstructionsTextView);

		pallet_tag = (EditText)findViewById(R.id.transferFirstEditText);
		old_location = (EditText)findViewById(R.id.transferSecondEditText);
		new_location = (EditText)findViewById(R.id.transferThirdEditText);

		first_image = (ImageButton)findViewById(R.id.transferFirstImageButton);
		second_image = (ImageButton)findViewById(R.id.transferSecondImageButton);
		third_image = (ImageButton)findViewById(R.id.transferThirdImageButton);

		virtual_tags = new ArrayList<>();
		virtual_ids = new ArrayList<>();

		flag = 0;

		///// Prepare ListView
		lv = (ListView)findViewById(R.id.checkedInoutListView);
		pallets = new ArrayList<>();
		locations = new ArrayList<>();
		SA = new StorageListView(activity, 'i', pallets, locations);
		lv.setAdapter(SA);

		// Assigning the implementaiton of OnClickListener
		RL.setOnClickListener(this);
		instructions.setOnClickListener(this);

		// Set IME Action behavior
		pallet_tag.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				boolean handled = false;
				if (i == EditorInfo.IME_ACTION_NEXT) {
					old_location.requestFocus();
					handled = true;
				}

				return handled;
			}
		});

		old_location.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				boolean handled = false;
				if (i == EditorInfo.IME_ACTION_NEXT) {
					new_location.requestFocus();
					handled = true;
				}

				return handled;
			}
		});

		// Set the extra OnClickListeners to call external app Barcode Scanner
		first_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				flag = 1;
				IntentIntegrator scanIntegrator = new IntentIntegrator(activity);
				scanIntegrator.initiateScan();
			}
		});

		second_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				flag = 2;
				IntentIntegrator scanIntegrator = new IntentIntegrator(activity);
				scanIntegrator.initiateScan();
			}
		});

		third_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				flag = 3;
				IntentIntegrator scanIntegrator = new IntentIntegrator(activity);
				scanIntegrator.initiateScan();
			}
		});

		// Retrieve all of the racks and their IDs from the server
		ParseQuery<ParseObject> query = new ParseQuery<>("Product");
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					if (objects.size() > 0) {
						for (ParseObject obj : objects) {
							virtual_tags.add(obj.getString("tag"));
							virtual_ids.add(obj.getObjectId());
						}
					}
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (item.getItemId()) {
			case R.id.logout:
				ParseUser.logOut();
				Intent i = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(i);
				return true;
			case android.R.id.home:
				finishAction();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Method receiving the results from the Barcode Scanner app
	 *   if flag is false:
	 *      assign results to first EditText (pallet)
	 *   else if flag is true:
	 *      assign results to second EditText (location)
	 *   else if no scan is done, Toast the user
	 * @param requestCode : Necessary for the receiving of information from BarcodeScanner
	 * @param resultCode : Necessary for the receiving of information from BarcodeScanner
	 * @param intent : Necessary for the receiving of information from BarcodeScanner
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode,
				resultCode, intent);
		if (scanningResult != null) {
			if (flag ==1) {
				pallet_tag.setText(scanningResult.getContents());
			}
			else if (flag ==2) {
				old_location.setText(scanningResult.getContents());
			}
			else if (flag == 3) {
				new_location.setText(scanningResult.getContents());
			}
		}
		else {
			Toast t = Toast.makeText(getApplicationContext(),
					"No scan data received!", Toast.LENGTH_SHORT);
			t.show();
		}

	}

	public void transferButtonAction(View view) {
		// Transfer button clicked
		if (view.getId() == R.id.transferButton) {
			// Get the three input variables from the EditTexts
			final String pallet = pallet_tag.getText().toString();
			final String old_loc = old_location.getText().toString();
			final String new_loc = new_location.getText().toString();

			// if one of the fields is missing, Toast the user and return
			if (pallet.equals("") || old_loc.equals("") || new_loc.equals("")) {
				Toast.makeText(context,
						"One ore more fields are missing! Please, try again.",
						Toast.LENGTH_LONG).show();
				return;
			}

			// if pallet is not in the virtual storage, Toast the user and return
			if (!(virtual_tags.contains(pallet))) {
				Toast.makeText(context,
						"The pallet you are looking for is not on the virtual storage!",
						Toast.LENGTH_LONG).show();
				pallet_tag.setText("");
				return;
			}

			int virtual_position = virtual_tags.indexOf(pallet);
			// Try to do the transfer of pallet at old loc to new loc
			ParseQuery<ParseObject> query = ParseQuery.getQuery("Product");
			query.getInBackground(virtual_ids.get(virtual_position), new GetCallback<ParseObject>() {
				@Override
				public void done(ParseObject object, ParseException e) {
					if (e == null) {
						if (object.getString("location").equals(old_loc)) {
							object.put("location", new_loc);
							object.saveEventually(new SaveCallback() {
								@Override
								public void done(ParseException e) {
									updateList(pallet, new_loc);
									pallet_tag.setText("");
									old_location.setText("");
									new_location.setText("");
								}
							});
						}
						else {
							Toast.makeText(context,
									"Pallet tag is not stored on this location! Please, try again.",
									Toast.LENGTH_LONG).show();
							old_location.setText("");
							return;
						}
					}
				}
			});
		}
		// Finish button clicked
		else if (view.getId() == R.id.transferFinishButton) {
			// Go back to HomeActivity
			finishAction();
		}
	}

	/**
	 * Updates list to show the new information and scrolls to the bottom of it
	 * @param pallet : pallet tag to be added to listView
	 * @param location : location number to be added to the listView
	 */
	public void updateList(String pallet, String location) {
		// Add the new pallet tag and location number to the respective ArrayLists
		pallets.add(pallet);
		locations.add(location);
		// Notify the Adapter to update the view of the listView
		SA.notifyDataSetChanged();

		// Scrolling to bottom of listView
		lv.post(new Runnable() {
			@Override
			public void run() {
				// Select the last row so it will scroll into view...
				lv.setSelection(SA.getCount() - 1);
			}
		});

		pallet_tag.requestFocus();
	}

	/**
	 * Display an AlertDialog to prompt the user if he really wants to
	 * finish the transfer transaction
	 */
	public void finishAction() {
		AlertDialog.Builder adb = new AlertDialog.Builder(context);
		adb.setTitle("One Second...");
		adb.setMessage("Are you sure you want to end this transaction?")
				.setCancelable(false)
				.setPositiveButton("YES", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(getApplicationContext(), HomeActivity.class);
						startActivity(i);
					}
				})
				.setNegativeButton("STAY", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		AlertDialog ad = adb.create();
		ad.show();
	}

	/**
	 * Hides the SoftInputKeyboard
	 * @param v : View of the component pressed
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.transferRelativeLayout||
				v.getId() == R.id.transferInstructionsTextView) {
			InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}
	}
}
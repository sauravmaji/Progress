package com.github.abusalam.android.projectaio;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.AccountDb;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpProvider;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSource;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.OtpSourceException;
import com.github.abusalam.android.projectaio.GoogleAuthenticator.TotpClock;
import com.github.abusalam.android.projectaio.ajax.VolleyAPI;
import com.github.abusalam.android.projectaio.sms.MessageDB;
import com.github.abusalam.android.projectaio.sms.MsgItem;
import com.github.abusalam.android.projectaio.sms.MsgItemAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class GroupSMS extends ActionBarActivity {

  public static final String TAG = GroupSMS.class.getSimpleName();
  /**
   * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
   * generated for an account until the moment the next code can be generated for the account.
   * This is to prevent the user from generating too many HOTP codes in a short period of time.
   */
  private static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;
  /**
   * The maximum amount of time (milliseconds) for which a HOTP code is displayed after it's been
   * generated.
   */
  private static final long HOTP_DISPLAY_TIMEOUT = 2 * 60 * 1000;
  protected PinInfo mUser;
  private MsgItemAdapter lvMsgHistAdapter;
  private Spinner spnrAllGroups;
  private ArrayList<MsgItem> lvMsgContent;
  private JSONArray respJsonArray;
  private MessageDB msgDB;
  private RequestQueue rQueue;
  private AccountDb mAccountDb;
  private OtpSource mOtpProvider;
  private EditText etMsg;
  private int mSelectedItemIndex;

  private void getAllGroups() {

    final JSONObject jsonPost = new JSONObject();

    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.user);
      mUser.hotpCodeGenerationAllowed = false;
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
          + " MDN:" + mUser.user, Toast.LENGTH_SHORT).show();
      return;
    }

    try {
      jsonPost.put("API", "AG");
      jsonPost.put("MDN", mUser.user);
      jsonPost.put("OTP", mUser.pin);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
        DashAIO.API_URL, jsonPost,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Group-SMS " + response.toString());
            Toast.makeText(getApplicationContext(), response.optString(DashAIO.KEY_STATUS), Toast.LENGTH_SHORT).show();
            try {
              respJsonArray = response.getJSONArray("DB");
              ArrayList<String> groupList = new ArrayList<String>();

              for (int i = 0; i < respJsonArray.length(); i++) {
                groupList.add(respJsonArray.getJSONObject(i).optString("GroupName"));
              }
              // Spinner adapter
              spnrAllGroups.setAdapter(new ArrayAdapter<String>(GroupSMS.this,
                  android.R.layout.simple_spinner_dropdown_item,
                  groupList));
            } catch (JSONException e) {
              e.printStackTrace();
              return;
            }

          }
        }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        Log.e(TAG, jsonPost.toString());
      }
    }
    );

    Handler mHandler = new Handler();
    mHandler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            mUser.hotpCodeGenerationAllowed = true;
          }
        },
        HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    Toast.makeText(getApplicationContext(), "Loading All Groups Please Wait...", Toast.LENGTH_LONG).show();
  }

  private void SyncProfile() {
    final JSONObject jsonPost = new JSONObject();

    try {
      mUser.pin = mOtpProvider.getNextCode(mUser.user);
    } catch (OtpSourceException e) {
      Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
          + " MDN:" + mUser.user, Toast.LENGTH_SHORT).show();
      return;
    }

    try {
      jsonPost.put("API", "SP");
      jsonPost.put("MDN", mUser.user);
      jsonPost.put("OTP", mUser.pin);
    } catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
        DashAIO.API_URL, jsonPost,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Group-SMS " + response.toString());
            Toast.makeText(getApplicationContext(), response.optString(DashAIO.KEY_STATUS), Toast.LENGTH_SHORT).show();
            try {
              JSONObject respJson = response.getJSONObject("DB");
              SharedPreferences mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);
              SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
              prefEdit.putString(DashAIO.PREF_KEY_MOBILE, mUser.user);
              prefEdit.putString(DashAIO.PREF_KEY_NAME, respJson.optString("UserName"));
              prefEdit.putString(DashAIO.PREF_KEY_EMAIL, respJson.optString("eMailID"));
              prefEdit.putString(DashAIO.PREF_KEY_POST, respJson.optString("Designation"));
              prefEdit.apply();
            } catch (JSONException e) {
              e.printStackTrace();
              return;
            }

          }
        }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        Log.e(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
    Toast.makeText(getApplicationContext(), "Synchronizing Profile Please Wait...", Toast.LENGTH_LONG).show();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_group_sms);

    SharedPreferences mInSecurePrefs;
    mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);

    mAccountDb = new AccountDb(this);
    mOtpProvider = new OtpProvider(mAccountDb, new TotpClock(this));

    mUser = new PinInfo();
    mUser.user = mInSecurePrefs.getString(DashAIO.PREF_KEY_UserID, null);
    rQueue = VolleyAPI.getInstance(this).getRequestQueue();

    /**
     * Retrieve & Populate List of SMSs Sent by the user using MessageAPI.
     */
    msgDB = new MessageDB(getApplicationContext());
    lvMsgContent = new ArrayList<MsgItem>();
    lvMsgContent.addAll(msgDB.getAllSms());
    msgDB.closeDB();
    ListView lvMsgHist = (ListView) findViewById(R.id.lvMsgHist);
    lvMsgHistAdapter = new MsgItemAdapter(this, R.layout.msg_item, lvMsgContent);
    lvMsgHist.setAdapter(lvMsgHistAdapter);

    registerForContextMenu(lvMsgHist);

    etMsg = (EditText) findViewById(R.id.etMsg);

    spnrAllGroups = (Spinner) findViewById(R.id.spinner);

    /*// Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        R.array.sms_groups, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    spnrAllGroups.setAdapter(adapter);*/

    getAllGroups();

    ImageButton btnSendSMS = (ImageButton) findViewById(R.id.btnSendSMS);
    btnSendSMS.setOnClickListener(new SendSMSClickListener());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.group_sm, menu);
    return true;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    mSelectedItemIndex = info.position;      // Get Index of long-clicked item

    super.onCreateContextMenu(menu, v, menuInfo);
    menu.setHeaderTitle("Choose Action");   // Context-menu title
    menu.add(0, v.getId(), 0, "Delivery Report");
    menu.add(0, v.getId(), 1, "Copy Message");
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (item.getTitle() == "Delivery Report") {
      //Do stuff
    } else if (item.getTitle() == "Copy Message") {
      MsgItem mMsgItem = lvMsgHistAdapter.getItem(mSelectedItemIndex);
      etMsg.setText(mMsgItem.getMsgText());
      Toast.makeText(getApplicationContext(), "Text Copied", Toast.LENGTH_LONG).show();
    } else {
      return false;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mAccountDb.close();
    rQueue.cancelAll(TAG);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.sync_profile) {
      SyncProfile();
      return true;
    }
    if (id == R.id.register_again) {
      SharedPreferences mInSecurePrefs = getSharedPreferences(DashAIO.SECRET_PREF_NAME, MODE_PRIVATE);
      SharedPreferences.Editor prefEdit = mInSecurePrefs.edit();
      prefEdit.clear();
      prefEdit.apply();
      prefEdit.commit();
      Toast.makeText(getApplicationContext(), getString(R.string.register_again_msg), Toast.LENGTH_LONG).show();
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * A tuple of user, OTP value, and type, that represents a particular user.
   *
   * @author adhintz@google.com (Drew Hintz)
   */
  private static class PinInfo {
    private String pin; // calculated OTP, or a placeholder if not calculated
    private String user;
    private boolean isHotp = true; // used to see if button needs to be displayed

    /**
     * HOTP only: Whether code generation is allowed for this account.
     */
    private boolean hotpCodeGenerationAllowed = true;
  }

  private class SendSMSClickListener implements View.OnClickListener {
    private final Handler mHandler = new Handler();

    @Override
    public void onClick(View view) {

      String txtMsg = etMsg.getText().toString();

      if (txtMsg.length() > 0) {
        Toast.makeText(getApplicationContext(), "Message Size: " + txtMsg.length(), Toast.LENGTH_SHORT).show();
        try {
          String oldPin = mUser.pin;
          mUser.pin = mOtpProvider.getNextCode(mUser.user);
          if (mUser.pin.equals(oldPin) || !mUser.hotpCodeGenerationAllowed) {
            Toast.makeText(getApplicationContext(), "Please wait for a while to generate new OTP for"
                + " MDN:" + mUser.user, Toast.LENGTH_LONG).show();
            return;
          }
        } catch (OtpSourceException e) {
          Toast.makeText(getApplicationContext(), "Error: " + e.getMessage()
              + " MDN:" + mUser.user, Toast.LENGTH_SHORT).show();
          return;
        }

        // Temporarily disable code generation for this account
        mUser.hotpCodeGenerationAllowed = false;

        // The delayed operation below will be invoked once code generation is yet again allowed for
        // this account. The delay is in wall clock time (monotonically increasing) and is thus not
        // susceptible to system time jumps.
        mHandler.postDelayed(
            new Runnable() {
              @Override
              public void run() {
                mUser.hotpCodeGenerationAllowed = true;
              }
            },
            HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES
        );

        sendSMS();

      } else {
        Toast.makeText(getApplicationContext(), "Type your Message", Toast.LENGTH_SHORT).show();
      }

    }
  }

  private void sendSMS() {
    final MsgItem newMsgItem = new MsgItem(spnrAllGroups.getSelectedItem().toString(),
        etMsg.getText().toString(), getString(R.string.default_msg_status));
    newMsgItem.setShowPB(true);
    lvMsgContent.add(newMsgItem);
    newMsgItem.setMsgID(msgDB.saveSMS(newMsgItem));
    msgDB.closeDB();
    etMsg.setText("");

    lvMsgHistAdapter.notifyDataSetChanged();


    final JSONObject jsonPost = new JSONObject();

    try {
      jsonPost.put("API", "SM");
      jsonPost.put("MDN", mUser.user);
      jsonPost.put("OTP", mUser.pin);
      jsonPost.put("TXT", newMsgItem.getMsgText());
      jsonPost.put("GRP", newMsgItem.getSentTo());
    } catch (JSONException e) {
      e.printStackTrace();
    }

    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
        DashAIO.API_URL, jsonPost,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Group-SMS " + response.toString());
            Toast.makeText(getApplicationContext(), response.optString(DashAIO.KEY_STATUS), Toast.LENGTH_SHORT).show();

            newMsgItem.setMsgStatus(response.optString(DashAIO.KEY_SENT_ON));
            newMsgItem.setShowPB(false);
            msgDB.updateSMS(newMsgItem);
            lvMsgHistAdapter.notifyDataSetChanged();
          }
        }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        VolleyLog.d(TAG, "Error: " + error.getMessage());
        Log.e(TAG, jsonPost.toString());
      }
    }
    );

    // Adding request to request queue
    jsonObjReq.setTag(TAG);
    rQueue.add(jsonObjReq);
  }

}

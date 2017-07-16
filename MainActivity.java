package com.circlesquare.serviceprovider.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.circlesquare.serviceprovider.R;
import com.circlesquare.serviceprovider.services.AddTechnicianService;
import com.circlesquare.serviceprovider.services.ServiceReceiver;
import com.circlesquare.serviceprovider.util.RegistrationIntentService;
import com.circlesquare.serviceprovider.util.ServiceProvider;
import com.circlesquare.serviceprovider.util.ServiceProviderLocalStore;
import com.circlesquare.serviceprovider.util.TypefaceUtil;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.circlesquare.serviceprovider.util.ServiceProvider.IP;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServiceReceiver.Listener {
    ServiceProviderLocalStore userLocalStore;
    EditText etName,etMobile, etPassword;
    CheckBox checkbox;

    Button regbutton;

    public String name;
    public String mobile;
    public String password;
    public String email;
    public static int serviceProviderId;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    private ProgressDialog statusDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "Roboto-Regular.ttf");
        userLocalStore = new ServiceProviderLocalStore(this);

        setTheme(R.style.AppTheme_NoActionBar);
        networkCall();
        etName=(EditText)findViewById(R.id.name);
        etMobile=(EditText)findViewById(R.id.mobile);
        etPassword=(EditText)findViewById(R.id.password);

        etName.setText(name);
        etMobile.setText(mobile);
        etPassword.setText(password);
        regbutton=(Button)findViewById(R.id.regbutton);
        regbutton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                isAllValid();
                Intent intent=new Intent(MainActivity.this,AddTechnicianService.class);
                startActivity(intent);
            }
        });

    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private boolean authenticate()
    {
        return userLocalStore.getServiceProviderLoggedInStatus();
    }

    public boolean isAllValid(){
        String name = etName.getText().toString();
        if(etName.getVisibility() == View.VISIBLE)
        if( name.length() == 0 )
        {
            etName.setError("Name is required!");
            return false;
        } else if(!name.matches("[a-zA-Z ]*")){
            etName.setError("Enter only alphabets but not numbers and special characters");
            return false;
        }

        String mobile = etMobile.getText().toString();
        if(mobile.length() != 10)
        {
            etMobile.setError("Enter valid 10 digits Mobile number");
            return false;
        }
        String password = etPassword.getText().toString();
        if( password.length() == 0 ){
            etPassword.setError("Enter your password");
            return false;
        }
//        if(!checkbox.isChecked()){
//            checkbox.setError("Read and Accept terms and conditions");
//            return false;
//        }

        return true;
    }

    public static boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "^[_A‐Za‐z0‐9‐\\+]+(\\.[_A‐Za‐z0‐9‐]+)*@"
                + "[A‐Za‐z0‐9‐]+(\\.[A‐Za‐z0‐9]+)*(\\.[A‐Za‐z]{2,})$";
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.regbutton) {
            if (isAllValid()) {
                   name = etName.getText().toString();
                   mobile = etMobile.getText().toString();
                   password=etPassword.getText().toString();
                   verifyCredentials();
            }
        }
        if(v.getId() == R.id.terms){
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Terms & Conditions");
            alertDialog.setMessage("This need to be written");
            alertDialog.setIcon(R.drawable.tick);
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }
    private boolean isSpinnerSet(Spinner spinner, String myString)
    {
        for(int i = 0; i < spinner.getCount(); i++){
            Log.e("item",spinner.getItemAtPosition(i).toString());
            if(spinner.getItemAtPosition(i).toString().equals(myString)){
                return true;
            }
        }
        return false;
    }
    public boolean isConnectedToServer(String url, int timeout) {
        try{
            URL myUrl = new URL(url);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void callAddService(){
        String token = userLocalStore.getAppInstanceId();
        Intent i = new Intent(MainActivity.this, AddTechnicianService.class);
        ServiceReceiver receiver = new ServiceReceiver(new Handler(Looper.getMainLooper()));
        receiver.setListener(MainActivity.this);
        i.putExtra("rec", receiver);
        i.putExtra("mobile", mobile);
        i.putExtra("name", name);
        i.putExtra("city","");
        i.putExtra("pincode",0);
        i.putExtra("gcmtoken",token);
        i.putExtra("serviceCenterId",0);
            startService(i);
    }



    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == 0 || resultCode == 200 ){
            String Id = resultData.getString("User_Id");
            Id = Id.trim();
            Toast.makeText(MainActivity.this, "User added with ID "+Id, Toast.LENGTH_SHORT).show();

        }
    }

    public int sendToken(String token, int userid) {
        int response = -1;
        try{
            URL url = new URL(IP+"/techinician/gcm/"+token+"/"+userid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("charset", "utf-8");
            connection.setReadTimeout(10000 /* milliseconds */);
            connection.setConnectTimeout(15000 /* milliseconds */);
            connection.connect();
            response = connection.getResponseCode();
        }catch(Exception e){
            Log.e("Exception",""+e);
        }
        return response;
    }

    public  class GCMRegistrationToken extends AsyncTask<Void,Void,Void>{
        protected Void doInBackground(Void... params){
            try {
                InstanceID instanceID = InstanceID.getInstance(MainActivity.this);
                final String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                // [END get_token]
//                Log.i(TAG, "GCM Registration Token: " + token);
//                Log.e("APP ID", instanceID.getId());
//           tokenShared = token;
                userLocalStore = new ServiceProviderLocalStore(MainActivity.this);
                userLocalStore.putAppInstanceId(token);
//                Log.e("InstanceID", userLocalStore.getAppInstanceId());
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }


    public void networkCall() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = ServiceProvider.serverURL;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        displayLayoutNavigateToHomeScreenIfSecondTime();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ServiceProvider.displayVolleyErrorMessage(volleyError, MainActivity.this);
            }
        });

        queue.add(stringRequest);
    }
    public void navigateToHomeScreen(){
        Intent intent = new Intent(MainActivity.this, ServiceProviderTabbedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent, ServiceProvider.getLeftToRightAnimation(this));
        finish();
    }
    public void storeLoggedInUser(){
        userLocalStore.setCurrentServiceProviderId(0); //need to update it
        userLocalStore.setServiceProviderLoggedInStatus(true);
    }
    public void verifyCredentials() {

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = IP+"/sp/Login";
        JSONObject jsonObject = new JSONObject();
        String mobile = etMobile.getText().toString();
        String password = etPassword.getText().toString();
        try {
            jsonObject.put("mobile", mobile);
            jsonObject.put("password", password);
        }catch(Exception e){
            e.printStackTrace();
        }

//        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url,jsonObject,
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
                        storeLoggedInUser();
                        navigateToHomeScreen();
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError volleyError) {
//                etMobile.setError("Mobile or Password is wrong");
//            }
//        });
//        queue.add(jsonRequest);
    }


    public void displayLayoutNavigateToHomeScreenIfSecondTime(){
        setContentView(R.layout.activity_main);
        if (authenticate() && userLocalStore.getCurrentServiceProviderId() != -1) {
            navigateToHomeScreen();
        }
        if (checkPlayServices()) {
                try {
                    final Intent intent = new Intent(this, RegistrationIntentService.class);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startService(intent);
                        }
                    });
                    thread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        etName = (EditText) findViewById(R.id.name);
        checkbox = (CheckBox) findViewById(R.id.checkBox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    checkbox.setError(null);
            }
        });
        etMobile = (EditText) findViewById(R.id.mobile);

        etPassword = (EditText) findViewById(R.id.password);

        etPassword.setTypeface(etMobile.getTypeface());
        regbutton = (Button) findViewById(R.id.regbutton);
        regbutton.setOnClickListener(this);
        TextView terms = (TextView) findViewById(R.id.terms);
        terms.setOnClickListener(this);
        terms.setTextColor(ContextCompat.getColor(this,R.color.linkColor));
        TextView iagree = (TextView) findViewById(R.id.iagree);
        iagree.setTextColor(Color.WHITE);

    }

}
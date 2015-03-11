package test.thm.couchbasesample;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;

import com.couchbase.lite.*;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends ActionBarActivity {

    Button btn_add;
    Button btn_sync;

    //neues Logging Tag
    final String TAG = "CouchbaseSample";

    //local Database Name
    public static final String DB_NAME = "testtest";

    Manager mManagerCBL;
    Database mDatabaseCBL;
    Document mCurrentDocument;

    //Sync Gateway Variables
    public static final String CB_SERVER_BUCKET_NAME = "sync_gateway";
    public static final String CB_SERVER = "http://192.168.1.107";
    public static final String CB_SERVER_PORT = "4984";
    public String CB_SYNC_URL = CB_SERVER + ":" + CB_SERVER_PORT + "/" + CB_SERVER_BUCKET_NAME + "/";

    public static final String CB_SYNC_USER = "testuser";
    public static final String CB_SYNC_USER_PW = "1234";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //create Manager and Database Function
        CreateManagerAndDatabase();
        btn_add = (Button) findViewById(R.id.btn_add);
        btn_add.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                EditText inputText = (EditText) findViewById(R.id.text_add);
                Object text;
                text = inputText.getText().toString();
                SetNewDocumentContent(text);
                inputText.setText("");
            }
        });


        btn_sync = (Button) findViewById(R.id.btn_sync);
        btn_sync.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startSync();
            }
        });
    }

    private void startSync() {
        try {
            URL url = new URL(CB_SYNC_URL);
            Log.d(TAG, url.toString());
            Replication push = mDatabaseCBL.createPushReplication(url);
            Replication pull = mDatabaseCBL.createPullReplication(url);
            pull.setContinuous(false);
            push.setContinuous(false);
            Authenticator auth = new BasicAuthenticator(CB_SYNC_USER, CB_SYNC_USER_PW);
            push.setAuthenticator(auth);
            pull.setAuthenticator(auth);

            push.addChangeListener(new Replication.ChangeListener() {
                @Override
                public void changed(Replication.ChangeEvent event) {
                    // will be called back when the push replication status changes
                }
            });
            pull.addChangeListener(new Replication.ChangeListener() {
                @Override
                public void changed(Replication.ChangeEvent event) {
                    // will be called back when the pull replication status changes
                }
            });
            push.start();
            pull.start();


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void CreateManagerAndDatabase(){
        // create a mManagerCBL
        try {
            mManagerCBL = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            Log.d (TAG, "Manager created");
        } catch (IOException e) {
            Log.e(TAG, "Cannot create manager object");
        }


        // create a name for the mDatabaseCBL and make sure the name is legal
        if (!Manager.isValidDatabaseName(DB_NAME)) {
            Log.e(TAG, "Bad database name");
        }

        // create a new mDatabaseCBL
        try {
            mDatabaseCBL = mManagerCBL.getDatabase(DB_NAME);
            Log.d (TAG, "Database created");
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot get database");
        }
    }


    public void SetNewDocumentContent(Object text) {

        // get the current date and time
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());
        // create an object that contains data for a mCurrentDocument
        Map<String, Object> docContent = new HashMap<String, Object>();
        docContent.put("text", text);
        docContent.put("creationDate", currentTimeString);
        // display the data for the new mCurrentDocument
        Log.d(TAG, "docContent=" + String.valueOf(docContent));
        // create an empty mCurrentDocument
        mCurrentDocument = mDatabaseCBL.createDocument();
        // add content to mCurrentDocument and write the mCurrentDocument to the mDatabaseCBL
        try {
            mCurrentDocument.putProperties(docContent);
            Log.d(TAG, "Document written to mDatabaseCBL named " + DB_NAME + " with ID = " + mCurrentDocument.getId());
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot write mCurrentDocument to mDatabaseCBL", e);
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

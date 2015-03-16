package test.thm.couchbasesample;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity {

    //local Database Name
    public static final String DB_NAME = ""; //TODO: Database Namen eintragen

    //Sync Gateway Variables
    public static final String CB_SYNC_USER = ""; //TODO: Username eingeben
    public static final String CB_SYNC_USER_PW = ""; //TODO: Passwort eingeben
    public static final String CB_SERVER_BUCKET_NAME = ""; //TODO: Bucket mit dem das Syncgateway syncronisiert eintragen
    public static final String CB_SERVER = ""; //TODO:IP Adresse des Syncgateways eintragen
    public static final String CB_SERVER_PORT = ""; //TODO: Syncgateway Port

    Button btn_add;
    Button btn_sync;
    Button btn_showAll;
    Manager manager;
    Database database;
    Document currentDoc;



    //neues Logging Tag
    final String TAG = "CouchbaseSample";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //create Manager and Database Function
        try {
            Log.d(TAG, "Call CreateManagerDatabase() to start CouchbaseLite");
            CreateManagerDatabase();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        //Add new document to database
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

        //start the sync
        btn_sync = (Button) findViewById(R.id.btn_sync);
        btn_sync.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startSync();
            }
        });

        btn_showAll = (Button) findViewById(R.id.btn_showAll);
        btn_showAll.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                try {
                    QueryAllDocs();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
        });



    }

    public void CreateManagerDatabase() throws CouchbaseLiteException {
        // create a manager
        try {
            manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            Log.d (TAG, "Manager created");
        } catch (IOException e) {
            Log.e(TAG, "Cannot create manager object");
        }

        // create a name for the database and make sure the name is valid
        if (!Manager.isValidDatabaseName(DB_NAME)) {
            Log.e(TAG, "Bad database name");
        }

        // create a new database
        try {
            database = manager.getDatabase(DB_NAME);
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

        // create an object that contains data for a currentDoc
        Map<String, Object> docContent = new HashMap<String, Object>();
        docContent.put("text", text);
        docContent.put("creationDate", currentTimeString);

        // display the data for the new currentDoc
        Log.d(TAG, "docContent=" + String.valueOf(docContent));

        // create an empty currentDoc
        currentDoc = database.createDocument();

        // add content to currentDoc and write the currentDoc to the database
        try {
            currentDoc.putProperties(docContent);
            Log.d(TAG, "Document written to database named " + DB_NAME + " with ID = " + currentDoc.getId());
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot write currentDoc to database", e);
        }


    }


    public void QueryAllDocs() throws CouchbaseLiteException {
        Query query = database.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        QueryEnumerator result = query.run();

        final ListView lv = (ListView) findViewById(R.id.listView);


        List<String> array_list = new ArrayList<String>();
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            Log.d(TAG, "Doc.id: " + row.getDocumentId() + "Doc.text: " + row.getDocument().getProperty("text"));
            array_list.add(row.getDocumentId() + "\nDoc.text: " +row.getDocument().getProperty("text")  + "\nDoc.CreationDate: " +row.getDocument().getProperty("creationDate") );
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, array_list);

        lv.setAdapter(arrayAdapter);

    }


    private void startSync() {
        URL url = null;
        try {
            // connect to SyncGateway
            url = new URL("http://"+ CB_SERVER + ":" + CB_SERVER_PORT + "/" + CB_SERVER_BUCKET_NAME + "/");
            Log.d(TAG, "Connect to SyncGateway: " + url.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // replication push and pull
        Replication push = database.createPushReplication(url);
        Replication pull = database.createPullReplication(url);

        // Continuous push or pull?
        pull.setContinuous(true);
        push.setContinuous(true);

        // Authenticate at the syncgateway
        Authenticator auth = new BasicAuthenticator(CB_SYNC_USER, CB_SYNC_USER_PW);
        push.setAuthenticator(auth);
        pull.setAuthenticator(auth);

        // Replication change Listener
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
        Log.d(TAG, "Push/Pull sync started");




    }

}

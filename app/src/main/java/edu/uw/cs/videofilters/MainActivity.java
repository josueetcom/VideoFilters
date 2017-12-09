package edu.uw.cs.videofilters;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = MainActivity.class.getSimpleName();
    static final int REQUEST_CODE = 394;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button select = (Button) findViewById(R.id.bSelect);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent  = new Intent()
                        .setType("video/*")
                        .setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CODE);
            }
        });
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                new UploadTask().execute(data.getData());

            } else {

                Toast.makeText(MainActivity.this, "Upload Failed!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Source: https://www.simplifiedcoding.net/android-upload-video-to-server-using-php/
    public String getPath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
        cursor.close();

        return path;
    }

    private class UploadTask extends AsyncTask<Uri, Long, String> {
        private String mHost;
        private String mMessage;
        private int mPort;
        private ProgressBar progressBar;
        private TextView description;
        private long totalBytes = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mHost = ((EditText) findViewById(R.id.etHost)).getText().toString();
            mPort = Integer.valueOf(((EditText)findViewById(R.id.etPort)).getText().toString());
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            progressBar = (ProgressBar) findViewById(R.id.progressbar);
            description = (TextView) findViewById(R.id.tvDescription);
            description.setText("Preparing to upload...");
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
        }


        @Override
        protected String doInBackground(Uri... uris) {
            try {
                // TODO: Modularize all this
                String filename = getPath(uris[0]);
                File file = new File(filename);
                FileInputStream fileInputStream = new FileInputStream(file);
                totalBytes = file.length();

                HttpURLConnection conn = (HttpURLConnection) new URL("http://" + mHost + ":" + mPort + "/").openConnection();
                String boundary = "****************";
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf('/'));
                }
                dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + filename +"\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
                dos.writeBytes(lineEnd);

                Log.e(LOG_TAG,"Headers are written");

                // create a buffer of maximum size
                int bytesAvailable = fileInputStream.available();
                int maxBufferSize = 1024;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[ ] buffer = new byte[bufferSize];

                // read file and write it into form...
                long totalWritten =0;
                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    // TODO: Update Progress Bar
                    dos.write(buffer, 0, bytesRead);
                    totalWritten += bytesRead;
                    publishProgress(totalWritten);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable,maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0,bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // close streams
                fileInputStream.close();
                dos.flush();
                dos.close();

                Log.e(LOG_TAG,"File Sent, Response: "+ String.valueOf(conn.getResponseCode()));

                InputStream is = conn.getInputStream();

                // retrieve the response from server
                int ch;

                StringBuffer b =new StringBuffer();
                while( ( ch = is.read() ) != -1 ){ b.append( (char)ch ); }
                String s=b.toString();
                Log.i("Response",s);

            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            progressBar.setProgress((int) Math.floor(100.0 * values[0] / totalBytes));
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            Toast.makeText(MainActivity.this, "File Uploaded!", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
            description.setVisibility(View.INVISIBLE);
            super.onPostExecute(s);
        }
    }

    private class DownloadTask extends AsyncTask<Uri, Integer, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(uris[0].toString()).openConnection();
                String boundary = "*****";
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "Upload Finished!", Toast.LENGTH_SHORT);
            super.onPostExecute(s);
        }
    }
}

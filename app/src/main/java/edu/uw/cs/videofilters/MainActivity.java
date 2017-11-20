package edu.uw.cs.videofilters;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = MainActivity.class.getSimpleName();
    static final int REQUEST_CODE = 394;

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

    private class UploadTask extends AsyncTask<Uri, Integer, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            try {
                // TODO: Modularize all this
                FileInputStream fileInputStream = new FileInputStream(uris[0].toString());

                HttpURLConnection conn = (HttpURLConnection) new URL("http://attu4.cs.washington.edu:9090/").openConnection();
                String boundary = "*****";
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String Tag="fSnd";
                String Title = "test title";
                String Description = "test description";

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\""+ lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(Title);
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"description\""+ lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(Description);
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + uris[0].toString() +"\"" + lineEnd);
                dos.writeBytes(lineEnd);

                Log.e(Tag,"Headers are written");

                // create a buffer of maximum size
                int bytesAvailable = fileInputStream.available();

                int maxBufferSize = 1024;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[ ] buffer = new byte[bufferSize];

                // read file and write it into form...
                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    // TODO: Update Progress Bar
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable,maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0,bufferSize);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // close streams
                fileInputStream.close();

                dos.flush();

                Log.e(Tag,"File Sent, Response: "+String.valueOf(conn.getResponseCode()));

                InputStream is = conn.getInputStream();

                // retrieve the response from server
                int ch;

                StringBuffer b =new StringBuffer();
                while( ( ch = is.read() ) != -1 ){ b.append( (char)ch ); }
                String s=b.toString();
                Log.i("Response",s);
                dos.close();
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
            Toast.makeText(MainActivity.this, "Upload Finished!", Toast.LENGTH_SHORT).show();
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

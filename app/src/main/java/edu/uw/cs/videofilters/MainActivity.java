package edu.uw.cs.videofilters;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = MainActivity.class.getSimpleName();
    static final int REQUEST_CODE = 394;
    static final String VIDEO_PATH = "VIDEO_PATH";
    SharedPreferences prefs;
    private ProgressBar progressBar;
    private TextView description;
    private GridView gridView;
    private Kernel kernel;

    @RequiresApi(api = Build.VERSION_CODES.N)
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
        gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(new ValueAdapter(this, false));

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        description = (TextView) findViewById(R.id.tvDescription);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        final ArrayAdapter<Kernel.Type> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Kernel.Type.values());
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemClickListener((adapterView, view, i, l) -> {
            String selection = spinner.getSelectedItem().toString();
            ValueAdapter a = (ValueAdapter) gridView.getAdapter();
            a.setEnabled(selection.equals(Kernel.Type.CUSTOM.name()));
            a.notifyDataSetChanged();
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

    private String readInputStream(InputStream is) throws IOException{
        int ch;

        StringBuilder b =new StringBuilder();
        while( ( ch = is.read() ) != -1 ){ b.append( (char)ch ); }
        String response = b.toString();
        Log.i("Response",response);
        return response;
    }

    private class UploadTask extends AsyncTask<Uri, Long, String> {
        private String MASTER_URL;
        private long totalBytes = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String host = ((EditText) findViewById(R.id.etHost)).getText().toString();
            String port = ((EditText)findViewById(R.id.etPort)).getText().toString();
            MASTER_URL = "http://" + host + ":" + port;
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
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

                HttpURLConnection conn = (HttpURLConnection) new URL(MASTER_URL + "/video").openConnection();
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

                while (bytesRead > 0) {
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

                int responseCode = conn.getResponseCode();
                Log.e(LOG_TAG,"File Sent, Response: "+ String.valueOf(responseCode));

                // retrieve the response from server
                InputStream is = conn.getInputStream();
                String resource_id = readInputStream(is).trim();
                Log.i("Response", resource_id);
                is.close();
                if (responseCode == 201) {
                    return resource_id.trim();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
            return null;
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            description.setText(String.format("Uploaded %.2f/%.2f", values[0] / 1024 / 1024.0, totalBytes / 1024 / 1024.0));
            progressBar.setProgress((int) Math.floor(100.0 * values[0] / totalBytes));
        }

        @Override
        protected void onPostExecute(String resource_id) {
            super.onPostExecute(resource_id);
            if (resource_id == null) {
                // Upload failed
                Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Upload succeeded", Toast.LENGTH_SHORT).show();
                new DownloadTask().execute(resource_id);
            }
        }
    }

    private class DownloadTask extends AsyncTask<String, Long, String> {
        private long totalBytes = 0;
        private String MASTER_URL;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String host = ((EditText) findViewById(R.id.etHost)).getText().toString();
            String port = ((EditText)findViewById(R.id.etPort)).getText().toString();
            MASTER_URL = "http://" + host + ":" + port;
            description.setText("Applying Filter...");
            // TODO: Get Kernel and URLEncoder.encode(kernel, "UTF-8")
            progressBar.setIndeterminate(true);
        }

        private void waitForDone(String task_id) throws IOException {
            String done = "false";
            while (done.equals("false")) {
                HttpURLConnection conn = (HttpURLConnection) new URL(MASTER_URL + "/status?task_id=" + task_id).openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.connect();
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                } else {
                    // retrieve the response from server
                    InputStream is = conn.getInputStream();
                    done = readInputStream(is).trim();
                    Log.i("Response", done);
                    is.close();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private final String PATH = "/data/data/videofilters/";

        @Override
        protected String doInBackground(String... resource_id) {
            try {
                // 1. Apply Filter
                HttpURLConnection conn = (HttpURLConnection) new URL(MASTER_URL + "/filter").openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                OutputStream dos = conn.getOutputStream();
                dos.write((String.format("resource_id=%s&kernel=%s", resource_id[0], kernel.toString())).getBytes("UTF-8"));

                // Get response with task id

                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                Log.e(LOG_TAG,"Filter Applied, Response: "+ String.valueOf(responseCode));

                // retrieve the response from server
                InputStream is = conn.getInputStream();
                String task_id = readInputStream(is).trim();
                conn.disconnect();

                // Now let's wait until the task succeeds before downloading
                waitForDone(task_id);

                // Ready for download!
                conn = (HttpURLConnection) new URL(MASTER_URL + "/video").openConnection();
                conn.setDoInput(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "Keep-Alive");
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), task_id + ".mp4");

//                long startTime = System.currentTimeMillis();
                Log.d(LOG_TAG, "Starting download......from " + conn.getURL().toString());
                is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                //Read bytes to the Buffer until there is nothing more to read(-1).
                totalBytes = conn.getContentLength();
                byte[] bytes = new byte[(int)totalBytes];
                int totalRead = 0;
                while (totalRead != totalBytes) {
                    int readSize = bis.read(bytes, totalRead, (int)totalBytes - totalRead);
                    if (readSize == -1) {
                        Log.e(LOG_TAG, "Content-Length did not match number of bytes read. Expected " + totalBytes + ". Actual " + totalRead);
                        break;
                    }
                    totalRead += readSize;
                    publishProgress((long) totalRead);
                }

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
                is.close();
                conn.disconnect();
                return file.getPath();
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            description.setText(String.format("Downloaded %.2f/%.2f", values[0] / 1024 / 1024.0, totalBytes / 1024 / 1024.0));
            progressBar.setIndeterminate(false);
            progressBar.setProgress((int) Math.floor(100.0 * values[0] / totalBytes));
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // TODO: Start VideoPlayerActivity
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            progressBar.setVisibility(View.INVISIBLE);
            description.setVisibility(View.INVISIBLE);
            if (s != null) {

                Intent i = new Intent(MainActivity.this, VideoPlayerActivity.class);
                i.putExtra(VIDEO_PATH, s);
                startActivity(i);
            } else {
                Toast.makeText(MainActivity.this, "Applying/Downloading Failed", Toast.LENGTH_LONG).show();
            }
        }
    }
}

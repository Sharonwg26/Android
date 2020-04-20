package samples.speech.cognitiveservices.microsoft.speech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;

import java.util.concurrent.Future;

import static android.Manifest.permission.*;

public class MainActivity extends AppCompatActivity {
    private EditText et_ip;
    private EditText et_msg;
    private Button tv_send;
    private Button tv_confirm;

    private Socket mSocket;
    private OutputStream mOutStream;
    private InputStream mInStream;
    private StringBuffer sb = new StringBuffer();
    private static String speechSubscriptionKey = "75851669a0a84bb8bd03dd1189288bea";
    // Replace below with your own service region (e.g., "westus").
    private static String serviceRegion = "westus.api.cognitive.microsoft.com/sts/v1.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        initView();
        setListener();
        int requestCode = 5; // unique code for the permission request
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);
    }

    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                Bundle data = msg.getData();
                sb.append(data.getString("msg"));
                sb.append("\n");
                tv_msg.setText(sb.toString());
            }
        }
    };
    private TextView tv_msg;
    private void initView() {
        et_ip = (EditText) findViewById(R.id.et_ip);
        et_msg = (EditText) findViewById(R.id.et_msg);
        tv_send = (Button) findViewById(R.id.tv_send);
        tv_confirm = (Button) findViewById(R.id.confirm);
        tv_msg = (TextView) findViewById(R.id.tv_msg);
    }

    private void setListener() {
        tv_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(et_msg.getText().toString());
            }
        });
        tv_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.e("info", "run: OPEN");
                    InetAddress ip = InetAddress.getByName(et_ip.getText().toString());
                    mSocket = new Socket("192.168.43.116", 3000);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("info", "run: Eorr");
                }
                tv_msg.setText("connect success========================================");
                startReader(mSocket);
            }
        });
    }

    public void send(final String str) {
        if (str.length() == 0){
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())) ;
                    bw.write(str);
                    bw.flush();
                    System.out.println("發送");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    private void startReader(final Socket socket) {

        new Thread(){
            @Override
            public void run() {
                try {
                    BufferedReader br= new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (true) {
                        System.out.println("*Wait input message*");
                        String msg = br.readLine();
                        System.out.println("the message is：" + msg);
                        Message message = new Message();
                        message.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("msg", msg);
                        message.setData(bundle);
                        handler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void onSpeechButtonClicked(View v) {
        try {
            SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
            assert(config != null);

            SpeechRecognizer reco = new SpeechRecognizer(config);
            assert(reco != null);

            Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
            assert(task != null);

            // Note: this will block the UI thread, so eventually, you want to
            //        register for the event (see full samples)
            SpeechRecognitionResult result = task.get();
            assert(result != null);

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                et_msg.setText(result.toString());
            }
            else {
                tv_msg.setText("Error recognizing. Did you update the subscription info?" + System.lineSeparator() + result.toString());
            }

            reco.close();
        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
            assert(false);
        }
    }

}

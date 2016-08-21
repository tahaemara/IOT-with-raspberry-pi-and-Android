package emaraic.com.raspberrypiclient;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Taha Emara
 *         Website: http://www.emaraic.com
 *         Email : taha@emaraic.com
 *         Created on: August 16, 2016
 */
public class MainActivity extends AppCompatActivity {
    private Switch led1;
    private Switch led2;
    private Switch led3;
    private EditText ip, port;
    private Button connect;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    private String ipaddress;
    private int portnum;
    private TextView link;
    private Pattern pattern;
    private Matcher matcher;
    private Handler handler;
    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pattern = Pattern.compile(IPADDRESS_PATTERN);
        handler = new Handler();
        setContentView(R.layout.activity_main);

        led1 = (Switch) findViewById(R.id.led1);
        led2 = (Switch) findViewById(R.id.led2);
        led3 = (Switch) findViewById(R.id.led3);
        ip = (EditText) findViewById(R.id.ip);
        port = (EditText) findViewById(R.id.port);
        connect = (Button) findViewById(R.id.connect);
        link = (TextView) findViewById(R.id.link);

        changeSwitchesSatte(false);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connect.getText().toString().equalsIgnoreCase("Connect")) {
                    try {
                        ipaddress = ip.getText().toString();
                        if (!checkIP(ipaddress))
                            throw new UnknownHostException(port + "is not a valid IP address");
                        portnum = Integer.parseInt(port.getText().toString());
                        if (portnum > 65535 && portnum < 0)
                            throw new UnknownHostException(port + "is not a valid port number ");
                        Client client = new Client(ipaddress, portnum);
                        client.start();


                    } catch (UnknownHostException e) {
                        showErrorsMessages("Please enter a valid IP !! ");
                    } catch (NumberFormatException e) {
                        showErrorsMessages("Please enter valid port number !! ");
                    }
                } else {
                    connect.setText("Connect");
                    changeSwitchesSatte(false);
                    closeConnection();
                }
            }
        });
        led1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    lightOn(1);
                } else {
                    lightOff(1);
                }
            }
        });

        led2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    lightOn(2);
                } else {
                    lightOff(2);
                }
            }
        });

        led3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    lightOn(3);
                } else {
                    lightOff(3);
                }
            }
        });

        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.emaraic.com"));
                startActivity(browserIntent);
            }
        });

    }//end of oncreate

    private void closeConnection() {
        try {
            out.writeObject("close");
            out.close();
            in.close();
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            showErrorsMessages(ex.getMessage());
        }
    }//end of closeConnection

    @Override
    protected void onStop() {
        super.onStop();
        closeConnection();
    }

    //////////////switches related methods ///////////////////
    void checkSwitchStatus() {
        if (led1.isChecked()) {
            lightOn(1);
        } else {
            lightOff(1);
        }
        if (led2.isChecked()) {
            lightOn(2);
        } else {
            lightOff(2);
        }
        if (led3.isChecked()) {
            lightOn(3);
        } else {
            lightOff(3);
        }
    }

    void changeSwitchesSatte(boolean state) {
        led1.setEnabled(state);
        led2.setEnabled(state);
        led3.setEnabled(state);
    }

    ////////////////////// light related methods /////////////
    void lightOn(int lednum) {
        try {
            out.writeObject(lednum + "1");
            out.flush();
            out.writeObject("end");
        } catch (IOException e) {
            e.printStackTrace();
            showErrorsMessages("Error while sending command!!");
        }
    }

    void lightOff(int lednum) {
        try {
            out.writeObject(lednum + "0");
            out.flush();
            out.writeObject("end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void showErrorsMessages(String error) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Error!! ").setMessage(error).setNeutralButton("OK", null).create().show();
    }

    public boolean checkIP(final String ip) {
        matcher = pattern.matcher(ip);
        return matcher.matches();
    }


    /////////////// client thread ////////////////////////////
    private class Client extends Thread {
        private String ipaddress;
        private int portnum;

        public Client(String ipaddress, int portnum) {
            this.ipaddress = ipaddress;
            this.portnum = portnum;
        }

        @Override
        public void run() {
            super.run();
            connectToServer(ipaddress, portnum);

        }


        public void connectToServer(String ip, int port) {

            try {
                socket = new Socket(InetAddress.getByName(ip), port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                for (int i = 0; i < 1; i++) {
                    System.out.println((String) in.readObject() + "\n");
                }
                checkSwitchStatus();
                handler.post(new Runnable() {
                    public void run() {
                        connect.setText("Close");
                        changeSwitchesSatte(true);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    public void run() {
                        showErrorsMessages("Unkown host!!");
                    }
                });
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

    }//end of client class

}

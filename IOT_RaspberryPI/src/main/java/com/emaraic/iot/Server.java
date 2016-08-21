package com.emaraic.iot;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Taha Emara 
 * Website: http://www.emaraic.com 
 * Email : taha@emaraic.com
 * Created on: August 16, 2016
 *
 */
public class Server {

    private ServerSocket server;
    private GpioPinDigitalOutput led1;
    private GpioPinDigitalOutput led2;
    private GpioPinDigitalOutput led3;
    final GpioController gpio = GpioFactory.getInstance();

    public Server() {

        led1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW);
        led2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);
        led3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);
    }

    public static void main(String[] args) {

        Server server = new Server();
        server.runServer();
    }

    public void runServer() {

        try {
            server = new ServerSocket(12345, 100);
            System.out.println("Server is running");
            while (true) {
                new Controller(server.accept()).start();
                System.out.println("hello client");
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    private class Controller extends Thread {

        private Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private String in;

        public Controller(Socket socket) {
            this.socket = socket;
            System.out.println("New client at " + socket.getRemoteSocketAddress());

        }

        @Override
        public void run() {
            try {
                getStreams();
                output.writeObject("Hello, Welocme to Raspberry PI");
                output.flush();
                while (!(in = (String) input.readObject()).equals("close")) {
                    if (!in.equals("end")) {//end here as a marker for ending of stream
                        //System.out.println("input   " + in);
                        switch (in) {
                            case "10"://led1 off
                                led1.low();
                                break;
                            case "11"://led1 on
                                led1.high();
                                break;
                            case "20":
                                led2.low();
                                break;
                            case "21":
                                led2.high();
                                break;
                            case "30":
                                led3.low();
                                break;
                            case "31":
                                led3.high();
                                break;
                        }
                        output.writeObject("command"+in.toUpperCase());
                    }else{
                    
                    }
                }//end of first while
            } catch (IOException e) {//catch output object exception
                e.printStackTrace();
                System.out.println("Error handling client @ " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } finally {
                closeConnection();
                System.out.println("Connection with client @ " + socket.getRemoteSocketAddress() + " closed");
            }
        }//end of run

        private void getStreams() throws IOException {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
        }//end of getStreams

        private void closeConnection() {
            try {
                output.close();
                input.close();
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }//end of closeConnection
    }
}

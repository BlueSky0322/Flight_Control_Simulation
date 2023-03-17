///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package Plane.Sensors;
//
//import Plane.Connections.ConnectionManager;
//import Plane.FC.FlightController;
//import Plane.Main.Plane;
//import com.rabbitmq.client.Channel;
//import java.io.IOException;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// *
// * @author ryann
// */
//public class ZSample implements Runnable {
//    Plane plane;
//    private volatile boolean pause = false;
//
//    public void pauseEvent() {
//        pause = true;
//        //ac.pauseEvent();
//        System.out.println("[FC] EMERGENCY PROTOCOL ENABLED! Pausing Sensors.");
//    }
//
//    public void unpauseEvent() {
//        pause = false;
//        //ac.unpauseEvent();
//        System.out.println("[FC] EMERGENCY PROTOCOL DISABLED! Resuming Sensors.");
//    }
//
//    public ZSample(Plane plane) {
//        this.plane = plane;
//        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//        executor.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
//    }
//
//    @Override
//    public void run() {
//        if (!pause) {
//            publishMessage();
//        }
//    }
//
//    public void publishMessage() {
//        ConnectionManager.declareExchange("my-sensor-exchange");
//        ConnectionManager.declareActuatorQueues();
//        ConnectionManager.declareActuatorBindings();
//        try (Channel channel = ConnectionManager.getConnection().createChannel()) {
//            String message = "GPS SENSOR message - Turn on the Suck dick ";
//            channel.basicPublish("my-sensor-exchange", "flight-controller", null, message.getBytes());
//            System.out.println("[GPS] " + message);
//        } catch (IOException | TimeoutException ex) {
//            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//}

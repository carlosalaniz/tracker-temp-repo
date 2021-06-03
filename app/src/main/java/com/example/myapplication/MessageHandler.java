package com.example.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import androidx.room.Room;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.Message;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MessageHandler {
    private static MessageHandler instance = null;
    private Mqtt5BlockingClient client;
    private boolean draining;

    MessageHandler() {
        this.client = MqttClient.builder()
                .identifier("clientId-db172a4c")
                .serverHost("broker.hivemq.com")
                .useMqttVersion5()
                .buildBlocking();
    }

    void tryDrainQueue() {
        this.draining = true;
        try {
            this.client.connectWith().cleanStart(false).send();
            Message message = InstallationConfiguration.appDatabase.messageDao().getOne();
            while (message != null) {
                this.client.publishWith()
                        .topic(message.topic)
                        .qos(MqttQos.EXACTLY_ONCE)
                        .payload(message.message.getBytes()).send();
                InstallationConfiguration.appDatabase.messageDao().deleteById(message.uid);
                message = InstallationConfiguration.appDatabase.messageDao().getOne();
            }
            this.client.disconnect();
        }catch (Exception e){
            Log.d("ERRORAPP", e.getMessage() + e.getCause());
        }
        this.draining = false;
    }

    void tryPublishData(String topicName, String data) {
        AsyncTask.execute(() -> {
            InstallationConfiguration.appDatabase.messageDao()
                    .insertAll(
                            new Message(
                                    data,
                                    "Android/" + InstallationConfiguration.DeviceID + "/" + topicName)
                    );
        });
        if (!this.draining)
            AsyncTask.execute(() -> {
                this.tryDrainQueue();
            });
    }


    static MessageHandler getMessageHandlerInstance() {
        if (MessageHandler.instance == null) {
            MessageHandler.instance = new MessageHandler();
        }
        return MessageHandler.instance;
    }
}

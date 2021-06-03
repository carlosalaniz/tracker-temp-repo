package com.example.myapplication.database;

import android.icu.util.Calendar;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;


@Entity
public class Message {
    @PrimaryKey(autoGenerate=true)
    public long uid;

    @ColumnInfo(name = "message")
    public String message;

    @ColumnInfo(name = "createdAt")
    public long createdAt;

    @ColumnInfo(name = "topic")
    public String topic;

    public Message(String message, String topic){
        this.message = message;
        this.topic = topic;
        this.createdAt = Calendar.getInstance().getTime().toInstant().toEpochMilli();
    }
}

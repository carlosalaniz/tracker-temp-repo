package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM message ORDER BY createdAt desc")
    List<Message> getAll();

    @Query("SELECT * FROM message WHERE uid=:id LIMIT 1")
    Message findById(int id);

    @Insert
    void insertAll(Message... users);

    @Delete
    void delete(Message message);

    @Query("DELETE FROM message WHERE uid = :id")
    void deleteById(long id);

    @Query("SELECT * FROM message LIMIT 1")
    Message getOne();
}

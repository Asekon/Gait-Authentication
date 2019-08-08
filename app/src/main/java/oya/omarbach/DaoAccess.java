package oya.omarbach;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

/**
 * Created by Omar on 5/14/2018.
 */
@Dao
public interface DaoAccess {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGait (Gait gait);
    @Query("SELECT * FROM gait WHERE name = :name")
    Gait fetchByName (String name);
    @Update
    void updateGait (Gait Gait);
    @Delete
    void deleteGait (Gait Gait);
}

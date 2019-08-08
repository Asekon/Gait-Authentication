package oya.omarbach;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by Omar on 5/14/2018.
 */
@Database (entities = {Gait.class}, version = 1, exportSchema = false)
public abstract class GaitDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "gaitDatabase";

    private static GaitDatabase INSTANCE;
    public abstract DaoAccess daoAccess() ;

    public static GaitDatabase getDatabase(Context context){
        if (INSTANCE ==  null){
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), GaitDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries() //VERY BAD
                    .build();

        }
        return INSTANCE;
    }
}

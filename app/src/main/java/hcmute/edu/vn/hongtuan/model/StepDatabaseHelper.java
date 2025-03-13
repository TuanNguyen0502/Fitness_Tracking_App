package hcmute.edu.vn.hongtuan.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class StepDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "step_database.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "steps";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_STEPS = "steps";
    private static final String COLUMN_DISTANCE = "distance";
    private static final String COLUMN_CALORIES = "calories";
    private static final String COLUMN_GOAL = "goal";

    public StepDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_DATE + " TEXT PRIMARY KEY, " +
                COLUMN_STEPS + " INTEGER DEFAULT 0, " +
                COLUMN_DISTANCE + " REAL DEFAULT 0, " +
                COLUMN_CALORIES + " REAL DEFAULT 0, " +
                COLUMN_GOAL + " INTEGER DEFAULT 500)";
        sqLiteDatabase.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public void updateGoal(int goal) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_GOAL + "=?", new Object[]{goal});
    }

    public int getGoal(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_GOAL + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
            int goal = cursor.getInt(0);
            return goal;
        }
        cursor.close();
        return 0;
    }

    public void updateCalories(String date, float calories) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_CALORIES + "=? WHERE " + COLUMN_DATE + "=?", new Object[]{calories, date});
        } else {
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COLUMN_DATE + ", " + COLUMN_CALORIES + ") VALUES (?, ?)", new Object[]{date, calories});
        }
        cursor.close();
    }

    public float getCalories(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_CALORIES + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
            float calories = cursor.getFloat(0);
            return calories;
        }
        cursor.close();
        return 0;
    }

    public void updateDistance(String date, float distance) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_DISTANCE + "=? WHERE " + COLUMN_DATE + "=?", new Object[]{distance, date});
        } else {
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COLUMN_DATE + ", " + COLUMN_DISTANCE + ") VALUES (?, ?)", new Object[]{date, distance});
        }
        cursor.close();
    }

    public float getDistance(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_DISTANCE + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
            float distance = cursor.getFloat(0);
            return distance;
        }
        cursor.close();
        return 0;
    }

    public void updateSteps(String date, int steps) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
             db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STEPS + "=? WHERE " + COLUMN_DATE + "=?", new Object[]{steps, date});
        } else {
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COLUMN_DATE + ", " + COLUMN_STEPS + ") VALUES (?, ?)", new Object[]{date, steps});
        }
        cursor.close();
    }

    public int getSteps(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Cursor is used to read data from database and it is used to iterate over the rows of the result set
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_STEPS + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?", new String[]{date});
        if (cursor.moveToFirst()) {
            int steps = cursor.getInt(0);
            return steps;
        }
        cursor.close();
        return 0;
    }
}

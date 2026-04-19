package com.example.studentfood.data.local.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Student;

public class StudentDAO {
    private final SQLiteDatabase db;

    public StudentDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public boolean insertStudent(Student student) {
        android.content.ContentValues v = new android.content.ContentValues();
        v.put(DBHelper.COL_STUDENT_ID, student.getUserId());
        v.put(DBHelper.COL_STUDENT_UNI, student.getUniversityName());
        v.put(DBHelper.COL_STUDENT_POINTS, student.getRewardPoints());
        
        return db.insertWithOnConflict(DBHelper.TABLE_STUDENT, null, v, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public int getStudentCount() {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_STUDENT, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    public boolean insertStudentOnly(Student student) {
        return insertStudent(student);
    }

    public boolean updateRewardPoints(String userId, float points) {
        android.content.ContentValues v = new android.content.ContentValues();
        v.put(DBHelper.COL_STUDENT_POINTS, points);
        return db.update(DBHelper.TABLE_STUDENT, v, DBHelper.COL_STUDENT_ID + " = ?", new String[]{userId}) > 0;
    }

    public java.util.List<Student> getTopStudents(int limit) {
        java.util.List<Student> students = new java.util.ArrayList<>();
        String query = "SELECT u.*, s." + DBHelper.COL_STUDENT_UNI + ", s." + DBHelper.COL_STUDENT_POINTS
                     + " FROM " + DBHelper.TABLE_USER + " u "
                     + " JOIN " + DBHelper.TABLE_STUDENT + " s ON u." + DBHelper.COL_USER_ID + " = s." + DBHelper.COL_STUDENT_ID
                     + " ORDER BY s." + DBHelper.COL_STUDENT_POINTS + " DESC LIMIT ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)})) {
            while (cursor != null && cursor.moveToNext()) {
                Student s = new Student();
                UserDAOMapper.mapBaseUserFields(s, cursor);
                s.setUniversityName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_UNI)));
                s.setRewardPoints(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_POINTS)));
                students.add(s);
            }
        }
        return students;
    }

    public Student getStudentById(String userId) {
        return getStudentFullInfo(userId);
    }

    public Student getStudentFullInfo(String userId) {
        String query = "SELECT u.*, s." + DBHelper.COL_STUDENT_UNI + ", s." + DBHelper.COL_STUDENT_POINTS
                     + " FROM " + DBHelper.TABLE_USER + " u "
                     + " JOIN " + DBHelper.TABLE_STUDENT + " s ON u." + DBHelper.COL_USER_ID + " = s." + DBHelper.COL_STUDENT_ID
                     + " WHERE u." + DBHelper.COL_USER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                Student s = new Student();
                UserDAOMapper.mapBaseUserFields(s, cursor);
                s.setUniversityName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_UNI)));
                s.setRewardPoints(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_POINTS)));
                return s;
            }
        }
        return null;
    }
}

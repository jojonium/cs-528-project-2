package com.bignerdranch.android.criminalintent.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.bignerdranch.android.criminalintent.Crime;

import java.util.Date;
import java.util.UUID;

import com.bignerdranch.android.criminalintent.database.CrimeDbSchema.CrimeTable;

public class CrimeCursorWrapper extends CursorWrapper {
    public CrimeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Crime getCrime() {
        String uuidString = getString(getColumnIndex(CrimeTable.Cols.UUID));
        String title = getString(getColumnIndex(CrimeTable.Cols.TITLE));
        long date = getLong(getColumnIndex(CrimeTable.Cols.DATE));
        int isSolved = getInt(getColumnIndex(CrimeTable.Cols.SOLVED));
        String suspect = getString(getColumnIndex(CrimeTable.Cols.SUSPECT));
        int photoNum = getInt(getColumnIndex(CrimeTable.Cols.PHOTO_NUM));
        int faceChecked = getInt(getColumnIndex(CrimeTable.Cols.FACE_CHECKED));
        int ocrChecked = getInt(getColumnIndex(CrimeTable.Cols.OCR_CHECKED));

        Crime crime = new Crime(UUID.fromString(uuidString));
        crime.setTitle(title);
        crime.setDate(new Date(date));
        crime.setSolved(isSolved != 0);
        crime.setSuspect(suspect);
        crime.setPhotoNum(photoNum);
        crime.setFaceChecked(faceChecked != 0);
        crime.setOcrChecked(ocrChecked != 0);

        return crime;
    }
}

package com.bignerdranch.android.criminalintent.database;

public class CrimeDbSchema {
    public static final class CrimeTable {
        public static final String NAME = "crimes";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String DATE = "date";
            public static final String SOLVED = "solved";
            public static final String SUSPECT = "suspect";
            public static final String PHOTO_NUM = "photo_num";
            public static final String FACE_CHECKED = "face_checked";
            public static final String OCR_CHECKED = "ocr_checked";
        }
    }
}

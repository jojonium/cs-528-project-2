package com.bignerdranch.android.criminalintent;

import java.util.Date;
import java.util.UUID;

public class Crime {

    private UUID mId;
    private String mTitle;
    private Date mDate;
    private boolean mSolved;
    private String mSuspect;
    private int photoNum;
    private boolean ocrChecked;
    private boolean faceChecked;

    public boolean isOcrChecked() {
        return ocrChecked;
    }

    public void setOcrChecked(boolean ocrChecked) {
        this.ocrChecked = ocrChecked;
    }

    public boolean isFaceChecked() {
        return faceChecked;
    }

    public void setFaceChecked(boolean faceChecked) {
        this.faceChecked = faceChecked;
    }

    public int getPhotoNum() {
        return photoNum;
    }

    public void setPhotoNum(int photoNum) {
        this.photoNum = photoNum;
    }

    public Crime() {
        this(UUID.randomUUID());
    }

    public Crime(UUID id) {
        mId = id;
        mDate = new Date();
    }
    public UUID getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public void setSolved(boolean solved) {
        mSolved = solved;
    }

    public String getSuspect() {
        return mSuspect;
    }

    public void setSuspect(String suspect) {
        mSuspect = suspect;
    }

    public String getPhotoFilename(int photoIndex) {
        return "IMG_" + getId().toString() + "_" + photoIndex + ".jpg";
    }
}

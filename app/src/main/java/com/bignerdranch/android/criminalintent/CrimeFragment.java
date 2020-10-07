package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String AUTHORITY = "com.bignerdranch.android.criminalintent.GenericFileProvider";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;

    private Crime mCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;
    private CheckBox mFaceCheckbox;
    private CheckBox mOCRCheckbox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;

    private View v;

    private FaceDetector mFaceDetector;
    private TextRecognizer mTextRecognizer;

    //private File mPhotoFile;
    private int maxPhotoFiles = 4;
    private ArrayList<File> mPhotoFiles = new ArrayList<File>(maxPhotoFiles);
    //private ImageView mPhotoView;
    private ArrayList<ImageView> mPhotoViews = new ArrayList<ImageView>(maxPhotoFiles);

    Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        for (int i = 0; i < this.maxPhotoFiles; ++i) {
            this.mPhotoFiles.add(CrimeLab.get(getActivity()).getPhotoFile(mCrime, i));
        }
        this.mFaceDetector = new FaceDetector.Builder(this.getActivity().getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        mTextRecognizer = new TextRecognizer.Builder(getActivity().getApplicationContext()).build();

        }


    @Override
    public void onDestroy() {
        this.mFaceDetector.release();
        mTextRecognizer.release();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckbox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckbox.setChecked(mCrime.isSolved());
        mSolvedCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        mFaceCheckbox = v.findViewById(R.id.face_detection_checkbox);
        mFaceCheckbox.setChecked(mCrime.isFaceChecked());
        mFaceCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setFaceChecked(isChecked);
                if (isChecked) {
                    int photoNum = (mCrime.getPhotoNum() + (maxPhotoFiles - 1)) % maxPhotoFiles;
                    if (mPhotoFiles.get(photoNum) == null || !mPhotoFiles.get(photoNum).exists())
                        return;
                    Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFiles.get(photoNum).getPath(), getActivity());
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<Face> faces = mFaceDetector.detect(frame);
                    String facesDetectedText = getResources().getQuantityString(R.plurals.faces, faces.size(), faces.size());
                    ((TextView) getView().findViewById(R.id.faces_detected)).setText(facesDetectedText);
                } else {
                    ((TextView) getView().findViewById(R.id.faces_detected)).setText("");
                }
                updatePhotoView(mCrime.getId());
            }
        });

        mOCRCheckbox = v.findViewById(R.id.ocr_checkbox);
        if (!mTextRecognizer.isOperational()) {
            mOCRCheckbox.setEnabled(false);
            mCrime.setOcrChecked(false);
        } else {
            mOCRCheckbox.setChecked(mCrime.isOcrChecked());
        }

        // Update text fields
        int lastPhoto = (mCrime.getPhotoNum() + (maxPhotoFiles - 1)) % maxPhotoFiles;
        if (mPhotoFiles.get(lastPhoto) != null && mPhotoFiles.get(lastPhoto).exists()) {
            setDetectedText(mOCRCheckbox.isChecked(), lastPhoto);

            if (mFaceCheckbox.isChecked()) {
                Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFiles.get(lastPhoto).getPath(), getActivity());
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Face> faces = mFaceDetector.detect(frame);
                String facesDetectedText = getResources().getQuantityString(R.plurals.faces, faces.size(), faces.size());
                ((TextView) v.findViewById(R.id.faces_detected)).setText(facesDetectedText);
            }
        }

        mOCRCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setOcrChecked(isChecked);
                int photoNum = (mCrime.getPhotoNum() + (maxPhotoFiles - 1)) % maxPhotoFiles;
                setDetectedText(isChecked, photoNum);
                updatePhotoView(mCrime.getId());
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);

        boolean canTakePhoto = !mPhotoFiles.contains(null) &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            captureImage.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(getActivity(), AUTHORITY, mPhotoFiles.get(mCrime.getPhotoNum()));
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoViews.add((ImageView) v.findViewById(R.id.crime_photo_0));
        mPhotoViews.add((ImageView) v.findViewById(R.id.crime_photo_1));
        mPhotoViews.add((ImageView) v.findViewById(R.id.crime_photo_2));
        mPhotoViews.add((ImageView) v.findViewById(R.id.crime_photo_3));
        updatePhotoView(mCrime.getId());

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[]{
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            // perform face detection if enabled
            if (mFaceCheckbox.isChecked()) {
                Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFiles.get(mCrime.getPhotoNum()).getPath(), getActivity());
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Face> faces = this.mFaceDetector.detect(frame);
                String facesDetectedText = getResources().getQuantityString(R.plurals.faces, faces.size(), faces.size());
                ((TextView) this.getView().findViewById(R.id.faces_detected)).setText(facesDetectedText);
            }
            setDetectedText(mOCRCheckbox.isChecked(), mCrime.getPhotoNum());

            mCrime.setPhotoNum((mCrime.getPhotoNum() + 1) % maxPhotoFiles);
            this.captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            captureImage.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(getActivity(), AUTHORITY, mPhotoFiles.get(mCrime.getPhotoNum()));
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            UUID uuid_fk = mCrime.getId();
            updatePhotoView(uuid_fk);
        }
    }

    private void setDetectedText(boolean isChecked, int photoNum){
        if (isChecked) {
            if (mPhotoFiles.get(photoNum) == null || !mPhotoFiles.get(photoNum).exists())
                return;
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFiles.get(photoNum).getPath(), getActivity());
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlock = mTextRecognizer.detect(frame);
            if (textBlock.size() > 0) {
                ((TextView) v.findViewById(R.id.text_detected)).setText(textBlock.get(0).getValue());
            }
        } else {
            ((TextView) v.findViewById(R.id.text_detected)).setText("");
        }

    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView(UUID uuid_fk) {
        for (int i = 0; i < maxPhotoFiles; ++i) {
            if (mPhotoFiles.get(i) == null || !mPhotoFiles.get(i).exists()) {
                mPhotoViews.get(i).setImageDrawable(null);
            } else {
                Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFiles.get(i).getPath(), getActivity());
                if (mFaceCheckbox.isChecked()) {
                    Bitmap destinationBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(destinationBitmap);
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    Paint paint = new Paint();
                    paint.setStrokeWidth(15);
                    paint.setColor(Color.GRAY);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    SparseArray<Face> faces = mFaceDetector.detect(frame);
                    for (int f = 0; f < faces.size(); ++f) {
                        Face face = faces.valueAt(f);
                        float x1 = face.getPosition().x;
                        float y1 = face.getPosition().y;
                        float x2 = x1 + face.getWidth();
                        float y2 = y1 + face.getHeight();
                        canvas.drawRect(new RectF(x1, y1, x2, y2), paint);
                    }
                    mPhotoViews.get(i).setImageBitmap(destinationBitmap);
                } else {
                    mPhotoViews.get(i).setImageBitmap(bitmap);
                }
            }
        }
    }
}

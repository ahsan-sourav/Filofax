package com.example.filofax.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import com.example.filofax.R;
import com.example.filofax.database.NotesDatabase;
import com.example.filofax.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle,inputNoteText;
    private TextView textDateTime;
    private ImageView imageNote;
    private String selectedImagePath;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        imageNote = findViewById(R.id.imageNote);

        textDateTime.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date()));

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNote();
            }
        });

        selectedImagePath = "";
        initOptions();

    }

    private void saveNote(){
        if(inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note title can't be empty",Toast.LENGTH_SHORT).show();
            return;
        }/*else if(inputNoteText.getText().toString().isEmpty()){
            Toast.makeText(this,"Note can't be empty",Toast.LENGTH_SHORT).show();
            return;
        }*/
        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setImagePath(selectedImagePath);

        // Need an async task for saving note because Room database doesn't allow operation on main thread
        @SuppressLint("StaticFieldLeak")
        class saveNoteTask extends AsyncTask<Void,Void,Void>{

            @Override
            protected Void doInBackground(Void... voids){
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected  void onPostExecute(Void aVoid){
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }
        new saveNoteTask().execute();
    }
    private void initOptions(){
        final LinearLayout layoutOptions = findViewById(R.id.layoutOptions);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutOptions);
        layoutOptions.findViewById(R.id.textOptions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(bottomSheetBehavior.STATE_EXPANDED);
                }else{
                    bottomSheetBehavior.setState(bottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        layoutOptions.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if(ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(CreateNoteActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                }else{
                    selectImage();
                }
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent,REQUEST_CODE_SELECT_IMAGE);
        }
    }

    // Handling permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else{
                Toast.makeText(this,"Permission Denied!",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data != null){
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null){
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(selectedImageUri);

                    }catch (Exception exception){
                        Toast.makeText(this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    private String getPathFromUri(Uri contentUri){
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);

        if(cursor == null){
            filePath = contentUri.getPath();
        }else{
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }
}
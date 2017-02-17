package paweltypiak.checkerboard_imu_comparator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private final static int DEFAULT_NECESSARY_IMAGES_NUMBER=15;
    private final static int DEFAULT_CHECKERBOARD_WIDTH =9;
    private final static int DEFAULT_CHECKERBOARD_HEIGHT =6;
    private EditText necessaryImagesNumberEditText;
    private EditText checkerboardWidthEditText;
    private EditText checkerboardHeightEditText;
    private Intent intent;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initializeSharedPreferences();
        initializeEditTexts();
        initializeSaveButton();
        initializeRestoreButton();
        initializeIntent();
        setResult(0, intent);
    }

    private void initializeIntent(){
        intent = new Intent();
        intent.putExtra(getString(R.string.settings_is_restore_clicked_key), false);
    }

    private void initializeSharedPreferences(){
        sharedPreferences = getSharedPreferences(getString(R.string.sharedprefrences_key), Context.MODE_PRIVATE);
    }

    private void initializeEditTexts(){
        necessaryImagesNumberEditText = (EditText) findViewById(R.id.necessary_images_number_edit_text);
        necessaryImagesNumberEditText.setText(
                Integer.toString(sharedPreferences.getInt(
                        getString(R.string.sharedpreferences_necessary_images_number_key),
                        DEFAULT_NECESSARY_IMAGES_NUMBER)));
        checkerboardWidthEditText = (EditText) findViewById(R.id.checkerboard_width_edit_text);
        checkerboardWidthEditText.setText(
                Integer.toString(sharedPreferences.getInt(
                        getString(R.string.sharedpreferences_checkerboard_width_key),
                        DEFAULT_CHECKERBOARD_WIDTH)));
        checkerboardHeightEditText = (EditText) findViewById(R.id.checkerboard_height_edit_text);
        checkerboardHeightEditText.setText(
                Integer.toString(sharedPreferences.getInt(
                        getString(R.string.sharedpreferences_checkerboard_height_key),
                        DEFAULT_CHECKERBOARD_HEIGHT)));
    }

    private void initializeSaveButton() {
        Button saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Integer.parseInt(necessaryImagesNumberEditText.getText().toString()) > 10 &&
                        Integer.parseInt(necessaryImagesNumberEditText.getText().toString()) < 20 &&
                        Integer.parseInt(checkerboardWidthEditText.getText().toString()) > 1 &&
                        Integer.parseInt(checkerboardHeightEditText.getText().toString()) > 1) {
                    saveToSharedPreferences();
                    Toast.makeText(
                            SettingsActivity.this.getApplicationContext(),
                            getString(R.string.changes_saved_toast_message),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            SettingsActivity.this.getApplicationContext(),
                            getString(R.string.incorrect_values_toast_message),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveToSharedPreferences(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(
                getString(R.string.sharedpreferences_necessary_images_number_key),
                Integer.parseInt(necessaryImagesNumberEditText.getText().toString()));
        editor.putInt(
                getString(R.string.sharedpreferences_checkerboard_width_key),
                Integer.parseInt(checkerboardWidthEditText.getText().toString()));
        editor.putInt(
                getString(R.string.sharedpreferences_checkerboard_height_key),
                Integer.parseInt(checkerboardHeightEditText.getText().toString()));
        editor.apply();
    }

    private void initializeRestoreButton() {
        Button restoreButton = (Button) findViewById(R.id.restore_button);
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(getString(R.string.settings_is_restore_clicked_key), true);
                setResult(RESULT_OK, intent);
                restoreEditTexts();
                restoreSharedPreferences();
                Toast.makeText(
                        SettingsActivity.this.getApplicationContext(),
                        getString(R.string.settings_restored_toast_message),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restoreEditTexts(){
        necessaryImagesNumberEditText.setText(Integer.toString(DEFAULT_NECESSARY_IMAGES_NUMBER));
        checkerboardWidthEditText.setText(Integer.toString(DEFAULT_CHECKERBOARD_WIDTH));
        checkerboardHeightEditText.setText(Integer.toString(DEFAULT_CHECKERBOARD_HEIGHT));
    }

    private void restoreSharedPreferences(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.sharedpreferences_necessary_images_number_key), DEFAULT_NECESSARY_IMAGES_NUMBER);
        editor.putInt(getString(R.string.sharedpreferences_checkerboard_width_key), DEFAULT_CHECKERBOARD_WIDTH);
        editor.putInt(getString(R.string.sharedpreferences_checkerboard_height_key), DEFAULT_CHECKERBOARD_HEIGHT);
        editor.apply();
    }
}

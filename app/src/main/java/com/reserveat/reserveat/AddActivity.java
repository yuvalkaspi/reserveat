package com.reserveat.reserveat;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.reserveat.reserveat.common.utils.DBUtils;
import com.reserveat.reserveat.common.utils.DateUtils;
import com.reserveat.reserveat.common.dbObjects.Reservation;
import com.reserveat.reserveat.common.utils.ValidationUtils;

import static com.google.android.gms.location.places.Place.TYPE_RESTAURANT;

public class AddActivity extends BaseActivity {

    private DatabaseReference mDatabase;
    EditText branchEditText;
    EditText dateEditText;
    EditText hourEditText;
    EditText numOfPeopleEditText;
    EditText reservationNameEditText;
    private Switch isReservationOnMyName;
    FirebaseUser currentUser;
    private static final String TAG = "AddActivity";
    private String restaurant;
    private String placeID;
    private Spinner dropdown;
    private String sittingPlace;
    Calendar current = Calendar.getInstance();
    final DateUtils.Day[] reservationDay = new DateUtils.Day[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);


        //Calendar myCalendar = Calendar.getInstance();

        branchEditText = findViewById(R.id.branch);
        dateEditText = findViewById(R.id.date);
        hourEditText = findViewById(R.id.hour);
        numOfPeopleEditText = findViewById(R.id.numOfPeople);
        reservationNameEditText = findViewById(R.id.reservationName);
        dropdown = findViewById(R.id.spinner);

        Button addButton = findViewById(R.id.add);

        final PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(TYPE_RESTAURANT).setCountry("IL")
                .build();

        autocompleteFragment.setFilter(typeFilter);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());
                restaurant = place.getName().toString();
                placeID = place.getId();
                branchEditText.setText(place.getAddress());
                branchEditText.setVisibility(View.VISIBLE);
                DBUtils.addingPlaceToDB(place, TAG);
                autocompleteFragment.setMenuVisibility(false);
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        dateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = current.get(Calendar.YEAR);
                int month = current.get(Calendar.MONTH);
                int day = current.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(AddActivity.this, R.style.DialogTheme, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                        DateFormat dateFormat = new SimpleDateFormat(DateUtils.dateFormatUser, Locale.getDefault());
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DATE, dayOfMonth);
                        reservationDay[0] = DateUtils.getDaybyDate(calendar);
                        Date dateObj = calendar.getTime();
                        dateEditText.setText(dateFormat.format(dateObj));
                    }
                },year,month,day);
                dpd.show();
            }
        });

        hourEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour = current.get(Calendar.HOUR_OF_DAY);
                int minutes = current.get(Calendar.MINUTE);
                TimePickerDialog tpd = new TimePickerDialog(AddActivity.this, R.style.DialogTheme, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minutes) {
                        DateFormat dateFormat = new SimpleDateFormat(DateUtils.hourFormat, Locale.getDefault());
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minutes);
                        Date dateObj = calendar.getTime();
                        hourEditText.setText(dateFormat.format(dateObj));
                    }
                },hour,minutes,false);
                tpd.show();
            }
        });

        isReservationOnMyName = findViewById(R.id.isReservationOnMyName);
        isReservationOnMyName.setText(R.string.is_reservation_on_my_name);
        isReservationOnMyName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    reservationNameEditText.setEnabled(false);
                    reservationNameEditText.setText("");//clear
                }
                else{
                    reservationNameEditText.setEnabled(true);
                }
            }
        });

        String[] items = new String[]{"Inside", "Outside", "Bar"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                sittingPlace = position == 0 ? "Inside" : position == 1 ? "Outside" : "Bar";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        mDatabase = FirebaseDatabase.getInstance().getReference();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAddReservation();
            }
        });
    }




    @Override
    public void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null){
            Intent intent = new Intent(AddActivity.this, LoginActivity.class );
            startActivity(intent);
        }
    }


    private void attemptAddReservation() {

        //String restaurant = restaurantEditText.getText().toString().trim();
        String branch = branchEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String hour = hourEditText.getText().toString().trim();
        String numOfPeople = numOfPeopleEditText.getText().toString();
        String reservationName = reservationNameEditText.getText().toString().trim();

        String[] mandatoryFieldsValues = {reservationName, numOfPeople, hour, date, branch};

        TextView[] formTextViewArr = {reservationNameEditText, numOfPeopleEditText,
                hourEditText, dateEditText, branchEditText};//order desc

        int[] formTextViewErrCodeArr = new int[formTextViewArr.length];

        View focusView = null;

        for (int i = 0 ; i < formTextViewErrCodeArr.length ; i ++ ){
            formTextViewErrCodeArr[i] = ValidationUtils.isEmptyTextField(mandatoryFieldsValues[i]);
        }

        if (isReservationOnMyName.isChecked()){//reservation on user's name
            formTextViewErrCodeArr[0] = 0;
            reservationNameEditText.setError(null);
        }

        for (int i = 0; i < formTextViewArr.length; i ++){
            int res = formTextViewErrCodeArr[i];
            TextView textView = formTextViewArr[i];
            if(res != 0){//error
                textView.setError(getString(res));
                focusView = textView;
            }else{
                textView.setError(null);// Reset error.
            }
        }
        if (focusView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            Log.w(TAG, "fields verification error: field was entered incorrect");
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            Log.i(TAG, "fields verification: success");
            addReservationToDB(restaurant, branch, date, hour, Integer.valueOf(numOfPeople), reservationName);

        }
    }

    private void addReservationToDB(String restaurant, String branch, String date, String hour, int numOfPeople, String reservationName) {

        Log.i(TAG, "adding a new reservation to DB");
        String key = mDatabase.child("reservations").push().getKey();
        try{
            String dateNewFormat = DateUtils.switchDateFormat(date, DateUtils.dateFormatUser, DateUtils.dateFormatDB);
            String newFullDateString = dateNewFormat + " " + hour;
            if(reservationName.equals("")){
                reservationName = currentUser.getDisplayName();
            }
            DateUtils.TimeOfDay timeInDay = DateUtils.getTimeOfDay(hour);
            Reservation reservation = new Reservation(currentUser.getUid(), restaurant, branch, placeID, newFullDateString, numOfPeople, reservationName, 0, reservationDay[0], timeInDay, sittingPlace);
            Map<String, Object> reservationValues = reservation.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/reservations/" + key, reservationValues);
            childUpdates.put("/users/" + currentUser.getUid() + "/reservations/" + key, reservationValues);

            mDatabase.updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "add new reservation:success", task.getException());
                        Intent intent = new Intent(AddActivity.this, MainActivity.class );
                        startActivity(intent);
                    } else {
                        Log.w(TAG, "add new reservation:failure", task.getException());
                        Toast.makeText(AddActivity.this, "Error!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }catch (ParseException e){
            //todo
            Toast.makeText(AddActivity.this, "Error!", Toast.LENGTH_LONG).show();
        }





    }
}


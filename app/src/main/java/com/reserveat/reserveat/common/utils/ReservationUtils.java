package com.reserveat.reserveat.common.utils;

import android.Manifest;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.reserveat.reserveat.AddActivity;
import com.reserveat.reserveat.MainActivity;
import com.reserveat.reserveat.R;
import com.reserveat.reserveat.common.dbObjects.Reservation;
import com.reserveat.reserveat.common.dbObjects.ReservationHolder;
import com.reserveat.reserveat.common.dbObjects.Restaurant;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;


public class ReservationUtils {

    public static int numOfStarsPerPick = 2;

    public static void myPopulateViewHolder(ReservationHolder viewHolder, Reservation model) throws ParseException {
        String date = model.getDate();
        int indexOfSpace = date.indexOf(" ");
        String dateOldFormat = date.substring(0, indexOfSpace);
        String hour = date.substring(indexOfSpace + 1);
        String dateNewFormat = DateUtils.switchDateFormat(dateOldFormat, DateUtils.dateFormatDB, DateUtils.dateFormatUser);
        viewHolder.setRestaurant(model.getRestaurant());
        viewHolder.setBranch(model.getBranch());
        viewHolder.setDate(dateNewFormat);
        viewHolder.setHour(hour);
        viewHolder.setNumOfPeople(model.getNumOfPeople());
    }

    public static void popUpWindowCreate(final PopupWindow mPopupWindow, final View customView, Reservation reservation, final Activity activity) {
        mPopupWindow.setElevation(5.0f);

        TextView restaurantTextView = (TextView) customView.findViewById(R.id.popup_resturant_name);
        restaurantTextView.setText(reservation.getRestaurant());

        TextView branchTextView = (TextView) customView.findViewById(R.id.popup_branch);
        branchTextView.setText(reservation.getBranch());

        String date = reservation.getDate();
        int indexOfSpace = date.indexOf(" ");
        String dateOldFormat = date.substring(0, indexOfSpace);
        String hour = date.substring(indexOfSpace + 1);

        try {
            TextView dateTextView = (TextView) customView.findViewById(R.id.popup_date);
            String dateNewFormat = DateUtils.switchDateFormat(dateOldFormat, DateUtils.dateFormatDB, DateUtils.dateFormatUser);
            dateTextView.setText(dateNewFormat);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView hourTextView = (TextView) customView.findViewById(R.id.popup_hour);
        hourTextView.setText(hour);

        TextView numOfPeopleTextView = (TextView) customView.findViewById(R.id.popup_num_of_people);
        numOfPeopleTextView.setText(Integer.toString(reservation.getNumOfPeople()));

        ImageButton closeButton = (ImageButton) customView.findViewById(R.id.ib_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss the popup window
                mPopupWindow.dismiss();
            }
        });

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("restaurants").child(reservation.getPlaceId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.w(TAG, "get restaurant:success");
                final Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                TextView phoneTextView = (TextView) customView.findViewById(R.id.popup_phone);
                phoneTextView.setText(restaurant.getPhoneNumber());
                phoneTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + restaurant.getPhoneNumber()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //request call permission if necessary
                        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, 1);
                            return;
                        }
                        activity.getApplication().getApplicationContext().startActivity(intent);
                    }
                });
//                TextView phoneTextView = (TextView) customView.findViewById(R.id.popup_phone);
//                phoneTextView.setMovementMethod(LinkMovementMethod.getInstance());
//                android:linksClickable="true"
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "get restaurant:failure");
            }
        });
    }




    public static void popUpPickClick(final Reservation reservation, View customView, String key, String userId, final Context context) {
        final Button pickButton = (Button) customView.findViewById(R.id.pick_Button);
        final TextView nameFormTextView = (TextView) customView.findViewById(R.id.popup_name_form);
        final TextView nameTextView = (TextView) customView.findViewById(R.id.popup_name);
        final TextView noteTextView = (TextView) customView.findViewById(R.id.popup_note);

        reservation.setPickedByUid(userId);
        Map<String, Object> reservationValues = reservation.toMap();
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/users/" + userId + "/pickedReservations/" + key, reservationValues);
        childUpdates.put("/users/" + reservation.getUid() + "/reservations/" + key, reservationValues);
        childUpdates.put("/historyReservations/" + key, reservationValues);
        childUpdates.put("/reservations/" + key, null);

        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "pick reservation:success", task.getException());
                    pickButton.setVisibility(View.GONE);
                    nameFormTextView.setVisibility(View.VISIBLE);
                    nameFormTextView.setText("Reservation name is : ");
                    nameTextView.setVisibility(View.VISIBLE);
                    nameTextView.setText(reservation.getReservationName());
                    noteTextView.setVisibility(View.VISIBLE);
                    noteTextView.setText(" it is your responsibility to validate the reservation");
                    DBUtils.updateStarsToUser(numOfStarsPerPick);

                } else {
                    Log.w(TAG, "pick reservation:failure", task.getException());
                    Toast.makeText(context , "Error!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

}


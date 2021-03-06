package com.example.arview.utils;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.arview.R;
import com.example.arview.databaseClasses.chatMessage;
import com.example.arview.databaseClasses.comment;
import com.example.arview.databaseClasses.followers;
import com.example.arview.databaseClasses.following;
import com.example.arview.databaseClasses.nearPost;
import com.example.arview.databaseClasses.post;
import com.example.arview.databaseClasses.profile;
import com.example.arview.databaseClasses.userChat;
import com.example.arview.databaseClasses.users;
import com.example.arview.friend.FriendsPostRecyclerViewAdapter;
import com.example.arview.location.RecyclerViewAdapter;
import com.example.arview.post.CommentsRecyclerViewAdapter;
import com.example.arview.post.PostRecyclerViewAdapter;
import com.example.arview.setting.SettingActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class FirebaseMethods {

    private static final String TAG = "FirebaseMethods";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private StorageReference mStorageReference;
    private UploadTask uploadTask;
    private String userID;
    private ChildEventListener mChildEventListener;
    private DatabaseReference mMessagesDatabaseReference;



    //vars
    private Context mContext;
    private String append = "";
    private String defaultProfilePhoto = "https://firebasestorage.googleapis.com/v0/b/arview-b5eb3.appspot.com/o/profile_photo.png?alt=media&token=edd70c89-7133-49fb-928e-4a3c579bbcec";



    public FirebaseMethods(Context context) {
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();

        mContext = context;

        if(mAuth.getCurrentUser() != null){
            userID = mAuth.getCurrentUser().getUid();
        }
    }



    public void registerNewEmail(final String email, String password, final String username){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the users. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in users can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(mContext, R.string.auth_failed , Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        }
                        else if(task.isSuccessful()){

                            sendVerificationEmail();

                            userID = mAuth.getCurrentUser().getUid();

                            Log.d(TAG, "onComplete: Authstate changed: " + userID);
                        }

                    }
                });
    }

    public void sendVerificationEmail(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null){
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(mContext, "Send verification email.", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "onComplete: sendEmailVerification: " + userID);

                            }else{
                                Toast.makeText(mContext, "couldn't send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    public String formatingUsername ( String username){
        String UserName = username.toLowerCase().replaceAll("\\s+|\\W","");
        return UserName;
    }

    public void checkIfUsernameExists(final String username) {
        Log.d(TAG, "checkIfUsernameExists: Checking if  " + username + " already exists.");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        final FirebaseUser user = mAuth.getCurrentUser();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child("users").orderByChild("userName").equalTo(username + ".");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                    if (singleSnapshot.exists()){
                        //TODO : random num for append
                        //TODO : check again after append add

                        Log.d(TAG, "checkIfUsernameExists: FOUND A MATCH: " + singleSnapshot.getValue(users.class).getUserName());
                        append = String.valueOf(myRef.push().getKey().substring(3,10));
                        Log.d(TAG, "onDataChange: username already exists. Appending random string to name: " + append);
                    }
                }

                String mUsername = "";

                mUsername = formatingUsername(username) +"." + formatingUsername(append);


                //add new users classes to the database
                addNewUser(user.getEmail(), mUsername, username);

                Toast.makeText(mContext, "Signup successful. Sending verification email.", Toast.LENGTH_SHORT).show();

                mAuth.signOut();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void addNewUser(String email, String username , String name){

        userID = mAuth.getCurrentUser().getUid();


        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date createdAt = new Date();
        String StrCreatedAt = dateFormat.format(createdAt);

        users users = new users(username,  email, StrCreatedAt, StrCreatedAt , 0 );

        myRef.child("users")
                .child(userID)
                .setValue(users);


        profile profile = new profile(username, name, "", defaultProfilePhoto, "", null, null, null);

        myRef.child("profile")
                .child(userID)
                .setValue(profile);

        followers followers = new followers();

        myRef.child("followers")
                .child(userID)
                .setValue(followers);

        following following = new following();

        myRef.child("following")
                .child(userID)
                .setValue(following);



    }


    /*************************************************************************************/

    public Uri getProfilePhoto(DataSnapshot dataSnapshot){

        Uri uri = null;

        for(DataSnapshot ds: dataSnapshot.getChildren()){

            if(ds.getKey().equals("profile")) {

                uri = Uri.parse(dataSnapshot.child("profilePhoto").getValue(String.class));

            }
        }
        return uri;
    }

    public profile getProfile(DataSnapshot dataSnapshot) throws IOException {
        Log.d(TAG, "getProfile: retrieving profile from firebase.");

        final profile profile  = new profile();

        for(DataSnapshot ds: dataSnapshot.getChildren()){

            // profile node
            if(ds.getKey().equals("profile")) {
                Log.d(TAG, "getProfile: user profile node datasnapshot: " + ds);

                try {

                    profile.setUserName(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getUserName()
                    );
                    profile.setName(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getName()
                    );
                    profile.setUserLocation(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getUserLocation()
                    );
                    profile.setProfilePhoto(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getProfilePhoto()
                    );
                    profile.setProfileDescription(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getProfileDescription()
                    );

                    profile.setFollowers(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getFollowers()
                    );

                    profile.setFollowing(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getFollowing()
                    );
                    profile.setPost(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getPost()
                    );


                } catch (NullPointerException e) {
                    Log.e(TAG, "getprofile: NullPointerException: " + e.getMessage());
                }

            }


        }

        Log.d(TAG, "geProfile: retrieved profile information: " + profile.toString());

        return profile;

    }

    public profile getProfile(DataSnapshot dataSnapshot , String userID) throws IOException {
        Log.d(TAG, "getProfile: retrieving profile from firebase.");

        final profile profile  = new profile();

        for(DataSnapshot ds: dataSnapshot.getChildren()){

            // profile node
            if(ds.getKey().equals("profile")) {
                Log.d(TAG, "getProfile: user profile node datasnapshot: " + ds);

                try {

                    profile.setUserName(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getUserName()
                    );
                    profile.setName(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getName()
                    );
                    profile.setUserLocation(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getUserLocation()
                    );
                    profile.setProfilePhoto(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getProfilePhoto()
                    );
                    profile.setProfileDescription(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getProfileDescription()
                    );

                    profile.setFollowers(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getFollowers()
                    );

                    profile.setFollowing(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getFollowing()
                    );
                    profile.setPost(
                            ds.child(userID)
                                    .getValue(profile.class)
                                    .getPost()
                    );


                } catch (NullPointerException e) {
                    Log.e(TAG, "getprofile: NullPointerException: " + e.getMessage());
                }

            }


        }

        Log.d(TAG, "geProfile: retrieved profile information: " + profile.toString());

        return profile;

    }


    /*
    test
     */

    public String substringUsername(String username){
        String UserName = username.toLowerCase();
        int index = username.lastIndexOf('.');
        if (index != -1) {
            UserName = username.substring(0, index);
        }
        return UserName;

    }


    /************************************* post ************************************/
    public void addPost(String PostImage, String postName, String postDesc, Location postLocation, String postEndTime, boolean visibilty, boolean personal ){

        String postID = String.valueOf(myRef.push().getKey());


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        String date = sdf.format(new Date());


        post post = new post(PostImage, postID, userID, postName, postDesc, date, postEndTime, visibilty, personal );

        myRef.child("profile").child(userID).child("post").child(postID).setValue(personal + "" +visibilty);


        if (personal){

            myRef.child("profile")
                    .child(userID)
                    .child("personalPosts")
                    .child(postID)
                    .setValue(post);

            //use GeoFirebase
            DatabaseReference GRef = mFirebaseDatabase.getInstance().getReference().child("profile").child(userID).child("personalPostsLocations");

            GeoFire geoFire = new GeoFire(GRef);

            geoFire.setLocation(postID, new GeoLocation(postLocation.getLatitude(), postLocation.getLongitude()),new
                    GeoFire.CompletionListener(){
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.e(TAG, "There was an error saving the location to GeoFire: " + error);
                            } else {
                                Log.e(TAG, "Location saved on server successfully!");
                            }                        }
                    });


        }else {

            if (visibilty){

                myRef.child("posts")
                        .child("public")
                        .child(postID)
                        .setValue(post);

                Log.e(TAG, "add post location" + postLocation);

                //use GeoFirebase
                DatabaseReference GRefP = mFirebaseDatabase.getInstance().getReference().child("postsLocations").child("public");

                GeoFire geoFire = new GeoFire(GRefP);

                geoFire.setLocation(postID, new GeoLocation(postLocation.getLatitude(), postLocation.getLongitude()),new
                        GeoFire.CompletionListener(){
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Log.e(TAG, "There was an error saving the location to GeoFire: " + error);
                                } else {
                                    Log.e(TAG, "Location saved on server successfully!");
                                }
                            }
                        });

            }else {

                myRef.child("posts")
                        .child("private")
                        .child(postID)
                        .setValue(post);

                //use GeoFirebase
                DatabaseReference GRefPr = mFirebaseDatabase.getInstance().getReference().child("postsLocations").child("private");

                GeoFire geoFire = new GeoFire(GRefPr);

                geoFire.setLocation(postID, new GeoLocation(postLocation.getLatitude(), postLocation.getLongitude()),new
                        GeoFire.CompletionListener(){
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    Log.e(TAG, "There was an error saving the location to GeoFire: " + error);
                                } else {
                                    Log.e(TAG, "Location saved on server successfully!");
                                }                            }
                        });


            }
        }


    }

    public void deletePost(String postID, String UserID, boolean v, boolean p ){

        myRef.child("profile")
                .child(UserID)
                .child("post")
                .child(postID)
                .removeValue();

        if (p){
            myRef.child("profile")
                    .child(UserID)
                    .child("personalPosts")
                    .child(postID)
                    .removeValue();

            myRef.child("profile")
                    .child(UserID)
                    .child("personalPostsLocations")
                    .child(postID)
                    .removeValue();

        }else {
            if (v){

                myRef.child("posts")
                        .child("public")
                        .child(postID)
                        .removeValue();

                myRef.child("postsLocations")
                        .child("public")
                        .child(postID)
                        .removeValue();

            }else {
                myRef.child("posts")
                        .child("private")
                        .child(postID)
                        .removeValue();

                myRef.child("postsLocations")
                        .child("private")
                        .child(postID)
                        .removeValue();

            }
        }
    }

    public boolean liked = false;
    public Boolean isLiked(final post p, final PostRecyclerViewAdapter.ViewHolder holder){

        myRef = FirebaseDatabase.getInstance().getReference();

        if (p.isPersonal()){
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("profile").child(p.getOwnerId()).child("personalPosts").child(p.getPostId()).child("likes").child(userID).exists()){
                        holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                        liked = true;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

        }else {
            if (p.isVisibilty()){
                myRef.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("posts").child("public").child(p.getPostId()).child("likes").child(userID).exists()){
                            holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            liked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

            }else {
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("posts").child("private").child(p.getPostId()).child("likes").child(userID).exists()){
                            holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            liked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }

        return liked;
    }

    public boolean Fliked = false;
    public Boolean isFLiked(final post p, final FriendsPostRecyclerViewAdapter.ViewHolder holder){

        myRef = FirebaseDatabase.getInstance().getReference();

        if (p.isPersonal()){
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("profile").child(p.getOwnerId()).child("personalPosts").child(p.getPostId()).child("likes").child(userID).exists()){
                        holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                        Fliked = true;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

        }else {
            if (p.isVisibilty()){
                myRef.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("posts").child("public").child(p.getPostId()).child("likes").child(userID).exists()){
                            holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            Fliked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

            }else {
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("posts").child("private").child(p.getPostId()).child("likes").child(userID).exists()){
                            holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            Fliked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }

        return Fliked;
    }

    public boolean Nliked = false;
    public Boolean isNLiked(final nearPost p, final RecyclerViewAdapter.ViewHolder holder){

        myRef = FirebaseDatabase.getInstance().getReference();

        if (p.getPost().isPersonal()){
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("profile").child(p.getPost().getOwnerId()).child("personalPosts").child(p.getPost().getPostId()).child("likes").child(userID).exists()){
                        holder.heart.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                        Nliked = true;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

        }else {
            if (p.getPost().isVisibilty()){
                myRef.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("posts").child("public").child(p.getPost().getPostId()).child("likes").child(userID).exists()){
                            holder.heart.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            Nliked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

            }else {
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("posts").child("private").child(p.getPost().getPostId()).child("likes").child(userID).exists()){
                            holder.heart.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            Nliked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }

        return Nliked;
    }

    public void addLike(post p, String userID){

        if (p.isPersonal()){
            myRef.child("profile").child(p.getOwnerId()).child("personalPosts").child(p.getPostId()).child("likes").child(userID).setValue("true");

        }else {
            if (p.isVisibilty()){
                myRef.child("posts").child("public").child(p.getPostId()).child("likes").child(userID).setValue("true");

            }else {
                myRef.child("posts").child("private").child(p.getPostId()).child("likes").child(userID).setValue("true");

            }
        }

    }

    public void unLike(post p, String userID){

        if (p.isPersonal()){
            myRef.child("profile").child(p.getOwnerId()).child("personalPosts").child(p.getPostId()).child("likes").child(userID).removeValue();

        }else {
            if (p.isVisibilty()){
                myRef.child("posts").child("public").child(p.getPostId()).child("likes").child(userID).removeValue();

            }else {
                myRef.child("posts").child("private").child(p.getPostId()).child("likes").child(userID).removeValue();

            }
        }

    }

    /************************************* post ************************************/

    /************************************* Comment ************************************/
    public void addComment(String PostID, String PostPath, comment comment){

        String CommentId = String.valueOf(myRef.push().getKey());

        if(PostPath.startsWith("true")){
            //post is personal
            myRef.child("profile").child("personalPosts").child(PostID).child("comments").child(CommentId).setValue(comment);

        } else {

            if (PostPath.endsWith("true")) {
                //post is public
                myRef.child("posts").child("public").child(PostID).child("comments").child(CommentId).setValue(comment);

            }
            if (PostPath.endsWith("false")) {
                //post is private
                myRef.child("posts").child("private").child(PostID).child("comments").child(CommentId).setValue(comment);

            }
        }
    }

    public boolean Cliked = false;
    public Boolean isCLiked(final String CommentID, final String PostID, String PostPath, final CommentsRecyclerViewAdapter.ViewHolder holder){

        if(PostPath.startsWith("true")){
            //post is personal
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if ( dataSnapshot.child("profile").child("personalPosts").child(PostID).child("comments").child(CommentID).child("likes").child(userID).exists()){
                        holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                        Cliked = true;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        } else {

            if (PostPath.endsWith("true")) {
                //post is public
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if ( dataSnapshot.child("posts").child("public").child(PostID).child("comments").child(CommentID).child("likes").child(userID).exists()){
                            holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            Cliked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
            if (PostPath.endsWith("false")) {
                //post is private
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if ( dataSnapshot.child("posts").child("private").child(PostID).child("comments").child(CommentID).child("likes").child(userID).exists()){
                            holder.like.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_red_heart));
                            Cliked = true;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }

        return Cliked;
    }

    public void addCLike(String CommentID, String PostID, String PostPath, String userID){

        if(PostPath.startsWith("true")){
            //post is personal
            myRef.child("profile").child("personalPosts").child(PostID).child("comments").child(CommentID).child("likes").child(userID).setValue("true");
        } else {
            if (PostPath.endsWith("true")) {
                //post is public
                myRef.child("posts").child("public").child(PostID).child("comments").child(CommentID).child("likes").child(userID).setValue("true");
            }
            if (PostPath.endsWith("false")) {
                //post is private
                myRef.child("posts").child("private").child(PostID).child("comments").child(CommentID).child("likes").child(userID).setValue("true");
            }
        }

    }

    public void unCLike(String CommentID, String PostID, String PostPath, String userID){

        if(PostPath.startsWith("true")){
            //post is personal
            myRef.child("profile").child("personalPosts").child(PostID).child("comments").child(CommentID).child("likes").child(userID).removeValue();
        } else {
            if (PostPath.endsWith("true")) {
                //post is public
                myRef.child("posts").child("public").child(PostID).child("comments").child(CommentID).child("likes").child(userID).removeValue();
            }
            if (PostPath.endsWith("false")) {
                //post is private
                myRef.child("posts").child("private").child(PostID).child("comments").child(CommentID).child("likes").child(userID).removeValue();
            }
        }
    }


    /************************************* Comment ************************************/
        /*
    test
     */

    public void setPhoneNumber(String PhoneNumber){

        myRef.child("users")
                .child(userID)
                .child("phoneNumber").setValue(PhoneNumber);

    }

    public void updateEmail(String Email){
        FirebaseUser user = mAuth.getCurrentUser();

        user.updateEmail(Email);

        myRef.child("users")
                .child(userID)
                .child("email").setValue(Email);

    }



    /*

    PERFECT

     */
    public void updatePassword(String oldPass,final String newPass){
        final FirebaseUser user = mAuth.getCurrentUser();


        // Get auth credentials from the user for re-authentication. The example below shows
        // email and password credentials but there are multiple possible providers,
        // such as GoogleAuthProvider or FacebookAuthProvider.
        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), oldPass);

        // Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPass).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Password updated");
                                        Toast.makeText(mContext, "Password Updated .", Toast.LENGTH_SHORT).show();

                                    } else {
                                        Log.d(TAG, "Error password not updated");
                                        Toast.makeText(mContext, "Error password not updated .", Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });
                        } else {
                            Log.d(TAG, "Error auth failed");
                            Toast.makeText(mContext, "Error auth failed .", Toast.LENGTH_SHORT).show();

                        }
                    }
                });

    }

    public void deleteProfilePhoto(){

        // delete from Storage
        StorageReference delRef =  mStorageReference.child(userID).child("profilePhoto").child("profile_photo.png" );

        delRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // File deleted successfully

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });

        //delete download url from profile
        myRef.child("profile")
                .child(userID)
                .child("profilePhoto")
                .setValue(defaultProfilePhoto);

    }

    public void uploadPhoto( Uri selectedImageUri){

        // Get a reference to store file
        final StorageReference upRef =  mStorageReference.child(userID).child("profilePhoto").child("profile_photo.png" );

        // Upload file to Firebase Storage
        uploadTask = upRef.putFile(selectedImageUri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });

        // When the image has successfully uploaded, we get its download URL
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return upRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String downloadURL = String.valueOf(downloadUri);
                    // set download uri on profile
                    myRef.child("profile")
                            .child(userID)
                            .child("profilePhoto")
                            .setValue(downloadURL);

                } else {
                    // Handle failures
                    // ...
                }
            }
        });

    }

    public void uploadChatPhoto(Uri selectedImageUri, final String chatId){

        // Get a reference to store file
        final StorageReference upRef =  mStorageReference.child(userID).child("Chats").child(chatId).child(selectedImageUri.getLastPathSegment());

        // Upload file to Firebase Storage
        uploadTask = upRef.putFile(selectedImageUri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });

        // When the image has successfully uploaded, we get its download URL
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return upRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String downloadURL = String.valueOf(downloadUri);
                    // set download uri on profile

                    chatMessage chatPhoto = new chatMessage(null, userID, downloadURL);

                    sendMessage(chatId, chatPhoto);


                } else {
                    // Handle failures
                    // ...
                }
            }
        });

    }

    public void editProfile(String name , String desc){
        myRef.child("profile")
                .child(userID)
                .child("name")
                .setValue(name);

        myRef.child("profile")
                .child(userID)
                .child("profileDescription")
                .setValue(desc);

    }


    public String addChat(final String OthertUserID){

        String chatID = String.valueOf(myRef.push().getKey());


        final userChat chatUser = new userChat(chatID, OthertUserID );
        final userChat chatUser1 = new userChat(chatID, userID );

        final DatabaseReference chat =myRef.child("userChat").child(userID);

        chat.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.hasChild(OthertUserID)) {

                    chat.child(OthertUserID)
                            .setValue(chatUser);

                    myRef.child("userChat")
                            .child(OthertUserID)
                            .child(userID)
                            .setValue(chatUser1);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return chatID;

    }


    public void sendMessage(String chatId, chatMessage chatMessge){

        String messageID = String.valueOf(myRef.push().getKey());

        myRef.child("Chats")
                .child(chatId)
                .child(messageID)
                .setValue(chatMessge);


    }


}













































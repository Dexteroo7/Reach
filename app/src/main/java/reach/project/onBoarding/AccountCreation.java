package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachAlbumProvider;
import reach.project.database.contentProvider.ReachArtistProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.contentProvider.ReachPlayListProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.MusicScanner;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.viewHelpers.CircleTransform;

public class AccountCreation extends Fragment {

    public AccountCreation() {
    }

    public static Fragment newInstance() {
        return new AccountCreation();
    }

    private String oldImageId;
    private SuperInterface mListener;
    private FrameLayout imageView;
    private Bitmap bitmap;
    private TextView progress,tele,uploadText;
    private LinearLayout bottomPart2,next;
    private RelativeLayout bottomPart3;
    private boolean dpDisabled = false;
    private final int IMAGE_PICKER_SELECT = 999;
    private IncomingHandler incomingHandler;
    private static WeakReference<AccountCreation> accountCreationWeakReference;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        Log.i("Ayush", "inflating contacts view");
        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText firstName = (EditText) rootView.findViewById(R.id.firstName);
        final LinearLayout importMusic = (LinearLayout) rootView.findViewById(R.id.importMusic);
        progress = (TextView) rootView.findViewById(R.id.syncStatus);
        bottomPart2 = (LinearLayout) rootView.findViewById(R.id.bottomPart2);
        bottomPart3 = (RelativeLayout) rootView.findViewById(R.id.bottomPart3);
        tele = (TextView) rootView.findViewById(R.id.telephoneNumber);
        next = (LinearLayout) rootView.findViewById(R.id.nextBtn);
        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);

        imageView = (FrameLayout) rootView.findViewById(R.id.profilePhoto);

        final String oldFirstName = SharedPrefUtils.getOldFirstName(sharedPreferences);
        final String oldLastName = SharedPrefUtils.getOldLastName(sharedPreferences);
        oldImageId = SharedPrefUtils.getOldImageId(sharedPreferences);

        if(!TextUtils.isEmpty(oldFirstName)) {

            if(!TextUtils.isEmpty(oldLastName))
                firstName.setText(oldFirstName + " " + oldLastName);
            else
                firstName.setText(oldFirstName);
        }

        if(!TextUtils.isEmpty(oldImageId))
            Picasso.with(getActivity())
                    .load(StaticData.cloudStorageImageBaseUrl + oldImageId)
                    .resize(350, 350)
                    .centerCrop()
                    .transform(new CircleTransform())
                    .into((ImageView) imageView.findViewById(R.id.displayPic));

        firstName.requestFocus();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!dpDisabled) {
                    final Intent intent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    // intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Photo"),
                            IMAGE_PICKER_SELECT);
                }
            }
        });

        accountCreationWeakReference = new WeakReference<>(this);

        importMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (firstName.length() == 0 ) {

                    Toast.makeText(getActivity(), "Please enter your name", Toast.LENGTH_SHORT).show();
                } else {

                    final InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    final String phoneNumber = SharedPrefUtils.getUserNumber(sharedPreferences);
                    inputMethodManager.hideSoftInputFromWindow(firstName.getWindowToken(), 0);

                    if (TextUtils.isEmpty(phoneNumber)) {
                        Log.i("Downloader", "Account creation could not find number");
                        mListener.startNumberVerification();
                        return;
                    }
                    importMusic.setEnabled(false);

                    if(isRemoving() || isDetached())
                        return;
                    /*try {
                        importDialog.show(getChildFragmentManager(), "import_dialog");
                    } catch (IllegalStateException ignored) {}*/
                    //reset the whole databases
                    SharedPrefUtils.purgeReachUser(sharedPreferences);
                    resetDatabases();
                    Log.i("Ayush", "Cleared everything : AccountCreation underway");
                    dpDisabled = true;
                    uploadText.setVisibility(View.GONE);
                    ((TextView)rootView.findViewById(R.id.tourText)).setText(firstName.getText().toString().trim());
                    rootView.findViewById(R.id.bottomPart1).setVisibility(View.INVISIBLE);
                    rootView.findViewById(R.id.bottomPart2).setVisibility(View.VISIBLE);
                    new SaveUserData().executeOnExecutor(
                            StaticData.threadPool,
                            firstName.getText().toString().trim(),
                            phoneNumber);
                }
            }
        });
        return rootView;
    }

    private void resetDatabases() {

        try {
            getActivity().getContentResolver().delete(ReachFriendsProvider.CONTENT_URI, 1 + "", null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        try {
            getActivity().getContentResolver().delete(ReachSongProvider.CONTENT_URI, 1 + "", null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        try {
            getActivity().getContentResolver().delete(ReachAlbumProvider.CONTENT_URI, 1 + "", null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        try {
            getActivity().getContentResolver().delete(ReachArtistProvider.CONTENT_URI, 1 + "", null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        try {
            getActivity().getContentResolver().delete(ReachPlayListProvider.CONTENT_URI, 1 + "", null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == IMAGE_PICKER_SELECT  && resultCode == Activity.RESULT_OK) {
            final Uri mImageUri = data.getData();
            if(mImageUri != null)
                new ImageBitmapExtractor().executeOnExecutor(StaticData.threadPool, data.getData());
        }
    }

    private class ImageBitmapExtractor extends AsyncTask<Uri, Void, Bitmap> {

        Uri temp;
        @Override
        protected Bitmap doInBackground(Uri... uris) {

            if(uris == null || uris.length == 0) return null;
            temp = uris[0];
            try {
                return Picasso.with(getActivity())
                              .load(uris[0])
                              .resize(350, 350)
                              .centerInside()
                              .get();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap resultBitmap) {
            super.onPostExecute(resultBitmap);

            if(isCancelled() || getActivity() == null || getActivity().isFinishing())
                return;

            if(resultBitmap == null)
                Toast.makeText(getActivity(), "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            else {
                bitmap = resultBitmap;
                uploadText.setVisibility(View.INVISIBLE);
                Picasso.with(getActivity())
                        .load(temp)
                        .resize(350, 350)
                        .centerCrop()
                        .transform(new CircleTransform())
                        .into((ImageView) imageView.findViewById(R.id.displayPic));
                //displayPic.setImageBitmap(bitmap);
            }
        }
    }

    private class SaveUserData extends AsyncTask<String, String, ReachUser> {

        @Override
        protected ReachUser doInBackground(String... strings) {

            publishProgress("Starting profile creation");
            final String gcmId = MiscUtils.autoRetry(new DoWork<String>() {
                @Override
                protected String doWork() throws IOException {

                    if (getActivity() == null)
                        return "QUIT";

                    return GoogleCloudMessaging.getInstance(getActivity())
                            .register("528178870551");
                }
            }, Optional.<Predicate<String>>of(new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String input) {
                    return TextUtils.isEmpty(input);
                }
            })).orNull();
            //0 means total network failure, kill the app
            if(TextUtils.isEmpty(gcmId) || gcmId.equals("QUIT"))
                return null;
            //1 means reInsert photo
            //if(bitmap == null) return null;
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if(bitmap != null)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);

            final ReachUser user = new ReachUser();
            user.setDeviceId(SharedPrefUtils.getDeviceId(getActivity()).trim().replace(" ", "-"));
            user.setMegaBytesReceived(0L);
            user.setMegaBytesSent(0L);
            user.setStatusSong("hello_world");
            user.setGcmId(gcmId);
            user.setUserName(strings[0]);
            user.setPhoneNumber(strings[1].replaceAll("[^0-9]", ""));
            if(bitmap != null)
                user.setImageId(CloudStorageUtils.uploadFile(byteArrayOutputStream.toByteArray(), true));
            else if(!TextUtils.isEmpty(oldImageId))
                user.setImageId(oldImageId);
            else
                user.setImageId("hello_world");
            //publishProgress("Uploading UserData");
            //insert user-object and get the userID
            final long id;
            final String toParse;
            final MyString dataAfterWork = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {
                    return StaticData.userEndpoint.insert(user).execute();
                }
            }, Optional.<Predicate<MyString>>absent()).orNull();
            Log.i("Ayush", "Testing");
            if(dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()))
                id = 0;
            else
                id = Long.parseLong(toParse);

            Log.i("Ayush", "Id received = " + id);
            if(id == 0) return null;
            //finally set the userID, probably unnecessary
            user.setId(id);
            Log.i("Ayush", "Temporary user uploaded");
            return user;
        }

        @Override
        protected void onPostExecute(ReachUser user) {

            super.onPostExecute(user);

            if(isCancelled() || getActivity() == null || getActivity().isFinishing())
                return;

            if(user == null) {
                Toast.makeText(getActivity(), "Network failed !", Toast.LENGTH_LONG).show();
                getActivity().finish();
                return;
            }
            final ReachUser reachUserDatabase = new ReachUser();
            reachUserDatabase.setDeviceId(user.getDeviceId());
            reachUserDatabase.setMegaBytesReceived(user.getMegaBytesReceived());
            reachUserDatabase.setMegaBytesSent(user.getMegaBytesSent());
            reachUserDatabase.setStatusSong(user.getStatusSong());
            reachUserDatabase.setGcmId(user.getGcmId());
            reachUserDatabase.setImageId(user.getImageId());
            reachUserDatabase.setId(user.getId());
            reachUserDatabase.setPhoneNumber(user.getPhoneNumber());
            reachUserDatabase.setUserName(user.getUserName());

            ReachActivity.serverId = user.getId();
            SharedPrefUtils.storeReachUser(getActivity()
                            .getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS).edit(), reachUserDatabase);

            final Intent intent = new Intent(getActivity(), MusicScanner.class);
            intent.putExtra("messenger", new Messenger(incomingHandler = new IncomingHandler()));
            getActivity().startService(intent);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if(isCancelled() || getActivity() == null || getActivity().isFinishing())
                return;
            if(accountCreationWeakReference.get() != null)
                accountCreationWeakReference.get().progress.setText(values[0]);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public static class IncomingHandler extends Handler {

        long songs = 0, playLists = 0;
        @Override
        public void handleMessage(Message message) {

            if(message == null || accountCreationWeakReference.get() == null)
                return;
            if(message.what == MusicScanner.FINISHED) {

                accountCreationWeakReference.get().bottomPart2.setVisibility(View.INVISIBLE);
                accountCreationWeakReference.get().bottomPart3.setVisibility(View.VISIBLE);
                accountCreationWeakReference.get().tele.setText(songs + " songs");
                accountCreationWeakReference.get().next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(accountCreationWeakReference.get() != null)
                            accountCreationWeakReference.get().mListener.onAccountCreated();
                    }
                });
            } else if(message.what == MusicScanner.SONGS) {

                accountCreationWeakReference.get().progress.setText("Found " + message.arg1 + " songs");
                songs = message.arg1 + 1;
            }
            else if(message.what == MusicScanner.PLAY_LISTS) {

                accountCreationWeakReference.get().progress.setText("Found " + message.arg1 + " playLists");
                playLists = message.arg1 + 1;
            }
            else if(message.what == MusicScanner.ALBUM_ARTIST)
                accountCreationWeakReference.get().progress.setText("Creating account");
        }
    }

    @Override
    public void onDestroy() {
        if(incomingHandler != null)
            incomingHandler.removeCallbacksAndMessages(null);
        incomingHandler = null;
        super.onDestroy();
    }
}

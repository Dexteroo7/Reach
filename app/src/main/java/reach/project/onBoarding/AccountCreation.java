package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.OldUserContainer;
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

    //    private static WeakReference<AccountCreation> reference;
    public static Fragment newInstance(Optional<OldUserContainer> container) {

        final AccountCreation fragment = new AccountCreation();

        if (container.isPresent()) {

            final Bundle bundle = new Bundle(2);
            final OldUserContainer userContainer = container.get();
            bundle.putStringArray("oldData", new String[]{
                    (TextUtils.isEmpty(userContainer.getFirstName()) ? "" : userContainer.getFirstName()) +
                            (TextUtils.isEmpty(userContainer.getLastName()) ? "" : userContainer.getLastName()),
                    userContainer.getImageId()
            });
            fragment.setArguments(bundle);
        }
//        reference = new WeakReference<>(fragment);
        return fragment;
    }

    private final int IMAGE_PICKER_SELECT = 999;
    private String imageId = "hello_world";

    private SuperInterface mListener;
    //    private IncomingHandler incomingHandler;
    private TextView uploadText;
    private View profilePhotoSelector;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userName = (EditText) rootView.findViewById(R.id.firstName);
        final TextView progress = (TextView) rootView.findViewById(R.id.syncStatus);

        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        profilePhotoSelector = rootView.findViewById(R.id.profilePhoto);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userName.requestFocus();

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final Bundle arguments;
        final String[] oldData;
        if ((arguments = getArguments()) != null &&
                (oldData = arguments.getStringArray("oldData")) != null &&
                oldData.length == 2) {

//            oldData[0] = name;
//            oldData[1] = imageId;
            if (!TextUtils.isEmpty(oldData[0]))
                userName.setText(oldData[0]);

            if (!TextUtils.isEmpty(oldData[1])) {

                imageId = oldData[1];
                Picasso.with(getActivity())
                        .load(StaticData.cloudStorageImageBaseUrl + imageId)
                        .resize(350, 350)
                        .centerCrop()
                        .transform(new CircleTransform())
                        .into((ImageView) profilePhotoSelector.findViewById(R.id.displayPic));
            }
        }

        rootView.findViewById(R.id.importMusic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (userName.length() == 0) {

                    Toast.makeText(getActivity(), "Please enter your name", Toast.LENGTH_SHORT).show();
                } else {

                    final InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    final String phoneNumber = SharedPrefUtils.getUserNumber(sharedPreferences);
                    inputMethodManager.hideSoftInputFromWindow(userName.getWindowToken(), 0);

                    if (TextUtils.isEmpty(phoneNumber)) {
                        Log.i("Downloader", "Account creation could not find number");
                        mListener.accountCreationError();
                        return;
                    }

                    view.setOnClickListener(null);
                    view.setEnabled(false);
                    if (isRemoving() || isDetached())
                        return;
                    /*try {
                        importDialog.show(getChildFragmentManager(), "import_dialog");
                    } catch (IllegalStateException ignored) {}*/
                    //reset the whole databases
                    sharedPreferences.edit().clear().apply();
                    resetDatabases();
                    Log.i("Ayush", "Cleared everything : AccountCreation underway");
                    profilePhotoSelector.setOnClickListener(null);
                    uploadText.setVisibility(View.GONE);
                    ((TextView) rootView.findViewById(R.id.tourText)).setText(userName.getText().toString().trim());
                    rootView.findViewById(R.id.bottomPart1).setVisibility(View.INVISIBLE);
                    rootView.findViewById(R.id.bottomPart2).setVisibility(View.VISIBLE);
                    progress.setText("Starting Profile Creation");
                    new SaveUserData(
                            rootView.findViewById(R.id.bottomPart2),
                            rootView.findViewById(R.id.bottomPart3),
                            rootView.findViewById(R.id.nextBtn),
                            (TextView) rootView.findViewById(R.id.telephoneNumber),
                            progress).executeOnExecutor(
                            StaticData.threadPool,
                            userName.getText().toString().trim(),
                            phoneNumber);
                }
            }
        });
        return rootView;
    }

    private final View.OnClickListener imagePicker = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            // intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Photo"),
                    IMAGE_PICKER_SELECT);
        }
    };

    private final View.OnClickListener proceed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onAccountCreated();
        }
    };

    private void resetDatabases() {

        //TODO reset notifications

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
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        final FragmentActivity activity;
        if ((activity = getActivity()) == null ||
                activity.isFinishing()) return;
        final Uri mImageUri;
        if (requestCode != IMAGE_PICKER_SELECT ||
                resultCode != Activity.RESULT_OK ||
                (mImageUri = data.getData()) == null) {

            Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        final InputStream stream;
        try {
            stream = activity.getAssets().open("key.p12");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final File tempFile;
        try {
            tempFile = File.createTempFile("profilePhoto", ".jpg");
            Files.copy(new InputSupplier<InputStream>() {
                @Override
                public InputStream getInput() throws IOException {
                    return activity.getContentResolver().openInputStream(mImageUri);
                }
            }, tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            MiscUtils.closeAndIgnore(stream);
            return;
        }

        StaticData.threadPool.submit(new Runnable() {
            @Override
            public void run() {
                Optional<String> newImage = CloudStorageUtils.uploadFile(tempFile, stream);
                if (newImage.isPresent())
                    imageId = newImage.get();
                else
                    imageId = "hello_world";
            }
        });

        uploadText.setVisibility(View.INVISIBLE);
        Picasso.with(activity)
                .load(mImageUri)
                .resize(350, 350)
                .centerCrop()
                .transform(new CircleTransform())
                .into((ImageView) profilePhotoSelector.findViewById(R.id.displayPic));
    }

    private class SaveUserData extends AsyncTask<String, String, ReachUser> {

        final View bottomPart2, bottomPart3, next ;
        final TextView phoneNumber, progress;

        private SaveUserData(View bottomPart2, View bottomPart3, View next, TextView phoneNumber, TextView progress) {
            this.bottomPart2 = bottomPart2;
            this.bottomPart3 = bottomPart3;
            this.next = next;
            this.phoneNumber = phoneNumber;
            this.progress = progress;
        }

        @Override
        protected ReachUser doInBackground(String... strings) {

            final String gcmId = MiscUtils.autoRetry(new DoWork<String>() {
                @Override
                protected String doWork() throws IOException {

                    final FragmentActivity activity = getActivity();
                    if (activity == null || activity.isFinishing())
                        return "QUIT";
                    return GoogleCloudMessaging.getInstance(activity)
                            .register("528178870551");
                }
            }, Optional.<Predicate<String>>of(new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String input) {
                    return TextUtils.isEmpty(input);
                }
            })).orNull();

            final FragmentActivity activity = getActivity();
            if (activity == null || activity.isFinishing() || TextUtils.isEmpty(gcmId) || gcmId.equals("QUIT"))
                return null;
            final ReachUser user = new ReachUser();
            user.setDeviceId(SharedPrefUtils.getDeviceId(activity).trim().replace(" ", "-"));
            user.setMegaBytesReceived(0L);
            user.setMegaBytesSent(0L);
            user.setStatusSong("hello_world");
            user.setGcmId(gcmId);
            user.setUserName(strings[0]);
            user.setPhoneNumber(strings[1]);
            user.setImageId(imageId);
            //insert user-object and get the userID
            final long id;
            final String toParse;
            final MyString dataAfterWork = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {
                    return StaticData.userEndpoint.insert(user).execute();
                }
            }, Optional.<Predicate<MyString>>absent()).orNull();
            if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()))
                id = 0;
            else
                id = Long.parseLong(toParse);
            Log.i("Ayush", "Id received = " + id);
            if (id == 0) return null;
            //finally set the userID, probably unnecessary
            user.setId(id);
            return user;
        }

        @Override
        protected void onPostExecute(ReachUser user) {

            super.onPostExecute(user);
            final FragmentActivity activity = getActivity();
            if (isCancelled() || isRemoving() || activity == null || activity.isFinishing())
                return;
            if (user == null) {
                Toast.makeText(getActivity(), "Network failed !", Toast.LENGTH_LONG).show();
                activity.finish();
                return;
            }

            ReachActivity.serverId = user.getId();
            SharedPrefUtils.storeReachUser(activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS).edit(), user);
            final Intent intent = new Intent(activity, MusicScanner.class);
            intent.putExtra("messenger", messenger);
            activity.startService(intent);
        }

        private final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {

            long songs = 0, playLists = 0;

            @Override
            public boolean handleMessage(Message message) {

                if (message == null)
                    return false;
                if (message.what == MusicScanner.FINISHED) {

                    bottomPart2.setVisibility(View.INVISIBLE);
                    bottomPart3.setVisibility(View.VISIBLE);
                    phoneNumber.setText(songs + " songs");
                    next.setOnClickListener(proceed);
                } else if (message.what == MusicScanner.SONGS) {
                    progress.setText("Found " + message.arg1 + " songs");
                    songs = message.arg1 + 1;
                } else if (message.what == MusicScanner.PLAY_LISTS) {

                    progress.setText("Found " + message.arg1 + " playLists");
                    playLists = message.arg1 + 1;
                } else if (message.what == MusicScanner.ALBUM_ARTIST)
                    progress.setText("Creating account");
                return true;
            }
        }));
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

//    public static class IncomingHandler extends Handler {
//
//        long songs = 0, playLists = 0;
//
//        @Override
//        public void handleMessage(Message message) {
//
//            final AccountCreation creation = reference.get();
//            if (message == null || creation == null)
//                return;
//            if (message.what == MusicScanner.FINISHED) {
//
//                creation.bottomPart2.setVisibility(View.INVISIBLE);
//                creation.bottomPart3.setVisibility(View.VISIBLE);
//                creation.phoneNumber.setText(songs + " songs");
//                creation.next.setOnClickListener(creation.proceed);
//            } else if (message.what == MusicScanner.SONGS) {
//
//                creation.progress.setText("Found " + message.arg1 + " songs");
//                songs = message.arg1 + 1;
//            } else if (message.what == MusicScanner.PLAY_LISTS) {
//
//                creation.progress.setText("Found " + message.arg1 + " playLists");
//                playLists = message.arg1 + 1;
//            } else if (message.what == MusicScanner.ALBUM_ARTIST)
//                creation.progress.setText("Creating account");
//        }
//    }

//    @Override
//    public void onDestroy() {
//        if (incomingHandler != null)
//            incomingHandler.removeCallbacksAndMessages(null);
//        incomingHandler = null;
//        super.onDestroy();
//    }
}

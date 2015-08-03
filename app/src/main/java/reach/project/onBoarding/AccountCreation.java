package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.gms.analytics.HitBuilders;
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
import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.MusicScanner;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.viewHelpers.CircleTransform;

public class AccountCreation extends Fragment {

    public static Fragment newInstance(Optional<OldUserContainerNew> container) {

        final AccountCreation fragment = new AccountCreation();

        if (container.isPresent()) {

            final Bundle bundle = new Bundle(2);
            final OldUserContainerNew userContainer = container.get();
            bundle.putStringArray("oldData", new String[]{
                    userContainer.getName() == null ? "" : userContainer.getName(),
                    userContainer.getImageId() == null ? "" : userContainer.getImageId(),
                    userContainer.getPromoCode() == null ? "" : userContainer.getPromoCode()});
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    private final int IMAGE_PICKER_SELECT = 999;
    private String imageId = "hello_world";
    private SuperInterface mListener;

    private TextView uploadText;
    private View profilePhotoSelector;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userName = (EditText) rootView.findViewById(R.id.firstName);
        final EditText promoCode = (EditText) rootView.findViewById(R.id.rCode);
        final TextView progress = (TextView) rootView.findViewById(R.id.syncStatus);

        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        profilePhotoSelector = rootView.findViewById(R.id.profilePhoto);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userName.requestFocus();

        final FragmentActivity activity = getActivity();
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final Bundle arguments;
        final String[] oldData;
        if ((arguments = getArguments()) != null &&
                (oldData = arguments.getStringArray("oldData")) != null &&
                oldData.length == 3) {
            /**
             * oldData[0] = name;
             * oldData[1] = imageId;
             * oldData[2] = promoCode;
             */
            if (!TextUtils.isEmpty(oldData[0]))
                userName.setText(oldData[0]);
            if (!TextUtils.isEmpty(oldData[1])) {

                imageId = oldData[1];
                Picasso.with(activity)
                        .load(StaticData.cloudStorageImageBaseUrl + imageId)
                        .resize(350, 350)
                        .centerCrop()
                        .transform(new CircleTransform())
                        .into((ImageView) profilePhotoSelector.findViewById(R.id.displayPic));
            }
            if (!TextUtils.isEmpty(oldData[2]))
                promoCode.setText(oldData[2]);
        }

        rootView.findViewById(R.id.importMusic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String name;
                if (TextUtils.isEmpty(userName.getText()) ||
                        TextUtils.isEmpty(name = userName.getText().toString().trim())) {
                    Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }

                final InputMethodManager inputMethodManager =
                        (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                final String phoneNumber = SharedPrefUtils.getUserNumber(sharedPreferences);
                inputMethodManager.hideSoftInputFromWindow(userName.getWindowToken(), 0);

                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.i("Downloader", "Account creation could not find number");
                    mListener.accountCreationError();
                    return;
                }

                view.setOnClickListener(null);
                view.setEnabled(false);
                if (isRemoving() || isDetached() || activity.isFinishing())
                    return;
                //reset the whole databases
                sharedPreferences.edit().clear().apply();
                Log.i("Ayush", "Cleared everything : AccountCreation underway");
                profilePhotoSelector.setOnClickListener(null);
                uploadText.setVisibility(View.GONE);
                ((TextView) rootView.findViewById(R.id.tourText)).setText(name);
                rootView.findViewById(R.id.bottomPart1).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.bottomPart2).setVisibility(View.VISIBLE);
                progress.setText("Starting Profile Creation");

                String code;
                if (TextUtils.isEmpty(promoCode.getText()) ||
                        TextUtils.isEmpty(code = promoCode.getText().toString().trim()))
                    code = "hello_world";

                new SaveUserData(
                        rootView.findViewById(R.id.bottomPart2),
                        rootView.findViewById(R.id.bottomPart3),
                        rootView.findViewById(R.id.nextBtn),
                        (TextView) rootView.findViewById(R.id.telephoneNumber),
                        progress).executeOnExecutor(
                        StaticData.threadPool,
                        name,
                        phoneNumber,
                        code);
                if (!StaticData.debugMode) {
                    ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("Promo Code")
                            .setAction("user Name - " + SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                            .setLabel("Code - " + code)
                            .setValue(1)
                            .build());
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

        final View bottomPart2, bottomPart3, next;
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

            final FragmentActivity activity = getActivity();

            final String gcmId = MiscUtils.autoRetry(new DoWork<String>() {
                @Override
                public String doWork() throws IOException {

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
            user.setPromoCode(strings[2]);
            user.setImageId(imageId);
            //insert user-object and get the userID
            final long id;
            final String toParse;
            final MyString dataAfterWork = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                public MyString doWork() throws IOException {
                    return StaticData.userEndpoint.insert(user).execute();
                }
            }, Optional.<Predicate<MyString>>absent()).orNull();
            if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()))
                id = 0;
            else
                id = Long.parseLong(toParse);
            Log.i("Ayush", "Id received = " + id);
            if (id == 0) //failed
                return null;
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
                Toast.makeText(activity, "Network failed !", Toast.LENGTH_LONG).show();
                activity.finish();
                return;
            }

            ReachActivity.serverId = user.getId();
            SharedPrefUtils.storeReachUser(activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS), user);
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
}

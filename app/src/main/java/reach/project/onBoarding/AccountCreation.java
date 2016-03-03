package reach.project.onBoarding;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.Toast;

import com.appspot.able_door_616.userApi.model.JsonMap;
import com.appspot.able_door_616.userApi.model.UserDataPersistence;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.common.base.Optional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.ContentType;
import reach.project.utils.MiscUtils;

public class AccountCreation extends Fragment {

    private static final int IMAGE_PICKER_SELECT = 999;
    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);

    private static final String USER_NAME = "USER_NAME";
    private static final String OLD_USER_ID = "OLD_USER_ID";
    private static final String OLD_USER_STATES = "OLD_USER_STATES";
    private static final String COVER_PHOTO_ID = "COVER_PHOTO_ID";
    private static final String PROFILE_PHOTO_ID = "PROFILE_PHOTO_ID";

    public static Fragment newInstance(Optional<UserDataPersistence> container) {

        final AccountCreation fragment = new AccountCreation();

        if (container.isPresent()) {

            final Bundle bundle = new Bundle(2);
            final UserDataPersistence userContainer = container.get();

            bundle.putString(USER_NAME, userContainer.getUserName());
            bundle.putLong(OLD_USER_ID, userContainer.getUserId());
            bundle.putString(COVER_PHOTO_ID, userContainer.getCoverPicId());
            bundle.putString(PROFILE_PHOTO_ID, userContainer.getImageId());

            final JsonMap jsonMap = userContainer.getOldContentStates();
            if (jsonMap != null && jsonMap.size() > 0) {

                Log.i("Ayush", "Found old data " + jsonMap.toString());
                final EnumMap<ContentType, Map<String, EnumSet<ContentType.State>>> contentStates =
                        ContentType.State.parseContentStateMap(jsonMap);
                bundle.putSerializable(OLD_USER_STATES, contentStates);
            }

            fragment.setArguments(bundle);
        }

        return fragment;
    }

    @Nullable
    private Uri newProfilePicUri = null, newCoverPicUri = null;
    @Nullable
    private SplashInterface mListener = null;
    @Nullable
    private SimpleDraweeView profilePhotoSelector = null;

    private final View.OnClickListener imagePicker = v -> {

        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICKER_SELECT);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Sorry! Your device does not support this feature", Toast.LENGTH_SHORT).show();
        }
    };

    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userNameEditText = (EditText) rootView.findViewById(R.id.userName);

        profilePhotoSelector = (SimpleDraweeView) rootView.findViewById(R.id.displayPic);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userNameEditText.requestFocus();

        final FragmentActivity activity = getActivity();
        final Bundle arguments = getArguments();
        final String userName = arguments.getString(USER_NAME, "");
        final String profilePhotoId = arguments.getString(PROFILE_PHOTO_ID, "");
        final String coverPhotoID = arguments.getString(COVER_PHOTO_ID, "");

        if (!TextUtils.isEmpty(userName)) {

            userNameEditText.setText(userName);
            userNameEditText.setSelection(userName.length());
        }

        if (!TextUtils.isEmpty(profilePhotoId)) {

            newProfilePicUri = Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + profilePhotoId);
            profilePhotoSelector.setController(MiscUtils.getControllerResize(profilePhotoSelector.getController(),
                    newProfilePicUri, PROFILE_PHOTO_RESIZE));
        }

        if (!TextUtils.isEmpty(coverPhotoID))
            newCoverPicUri = Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + coverPhotoID);

        rootView.findViewById(R.id.verify).setOnClickListener(view -> {

            final String name;
            if (TextUtils.isEmpty(name = userNameEditText.getText().toString().trim())) {
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            //OK
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(userNameEditText.getWindowToken(), 0);
            view.setOnClickListener(null);
            view.setEnabled(false);
            profilePhotoSelector.setOnClickListener(null);

            if (mListener != null)
                mListener.onOpenScan(
                        name,
                        arguments.getLong(OLD_USER_ID, 0),
                        profilePhotoId,
                        coverPhotoID,
                        newProfilePicUri,
                        newCoverPicUri,
                        arguments.getSerializable(OLD_USER_STATES));

            //TODO track
            /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(2);
            simpleParams.put(PostParams.USER_NUMBER, phoneNumber);
            simpleParams.put(PostParams.USER_NAME, name);
            try {
                UsageTracker.trackLogEvent(simpleParams, UsageTracker.NAME_ENTERED);
            } catch (JSONException ignored) {}*/
        });
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        final Activity activity;
        if ((activity = getActivity()) == null || activity.isFinishing()) {

            Log.i("Ayush", "ACTIVITY NOT FOUND !");
            return;
        }

        if (requestCode != IMAGE_PICKER_SELECT || resultCode != Activity.RESULT_OK || (newProfilePicUri = data.getData()) == null) {

            Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profilePhotoSelector != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(
                    profilePhotoSelector.getController(), newProfilePicUri, PROFILE_PHOTO_RESIZE);
            profilePhotoSelector.setController(draweeController);
        }
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SplashInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
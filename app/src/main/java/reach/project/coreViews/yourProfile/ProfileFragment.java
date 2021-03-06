package reach.project.coreViews.yourProfile;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import reach.backend.entities.messaging.model.MyString;
import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.notificationCentre.NotificationActivity;
//import reach.project.player.PlayerActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;

/**
 * Created by gauravsobti on 06/04/16.
 */
public class ProfileFragment extends Fragment {

    public static final String TAG = ProfileFragment.class.getSimpleName();

    private SuperInterface mListener;
    private ProgressBar progress_bar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.activity_profile,container,false);

        sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);

        final Toolbar mToolbar = (Toolbar) v.findViewById(R.id.editProfileToolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationOnClickListener(x -> mListener.removeProfileFragment(ProfileFragment.this));
        mToolbar.inflateMenu(R.menu.yourprofile_menu);
        mToolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
//                case R.id.player_button:
//                    PlayerActivity.openActivity(getActivity());
//                    return true;
                case R.id.notif_button:
                    NotificationActivity.openActivity(getActivity(), NotificationActivity.OPEN_NOTIFICATIONS);
                    return true;
                case R.id.settings_button:
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                    return true;
                default:
                    return false;
            }
        });

        final Bundle bundle = getArguments();
        if (bundle == null || (userId = bundle.getLong("userId", 0L)) == 0) {
            
            mListener.removeProfileFragment(this);
        }

        final Cursor cursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{
                        ReachFriendsHelper.COLUMN_USER_NAME, //0
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //1
                        ReachFriendsHelper.COLUMN_IMAGE_ID, //2
                        ReachFriendsHelper.COLUMN_STATUS, //3
                        ReachFriendsHelper.COLUMN_NUMBER_OF_APPS, //4
                        ReachFriendsHelper.COLUMN_COVER_PIC_ID}, //5
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);

        if (cursor == null) {
            mListener.removeProfileFragment(this);
        }

        if (!cursor.moveToFirst()) {
            cursor.close();
            mListener.removeProfileFragment(this);
        }

        
        progress_bar = (ProgressBar) v.findViewById(R.id.progress_bar);
        final RelativeLayout headerRoot = (RelativeLayout) v.findViewById(R.id.headerRoot);

        ((TextView) headerRoot.findViewById(R.id.userName)).setText(cursor.getString(0));
        ((TextView) headerRoot.findViewById(R.id.userHandle)).setText("@" + cursor.getString(0).toLowerCase().split(" ")[0]);
        ((TextView) headerRoot.findViewById(R.id.musicCount)).setText(cursor.getInt(1) + "");
        SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);
        profilePic.setController(MiscUtils.getControllerResize(profilePic.getController(),
                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(2)), 100, 100));

        SimpleDraweeView coverPic = (SimpleDraweeView) headerRoot.findViewById(R.id.coverPic);
        coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(5)), 500, 300));
//        coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
//                Uri.parse(MiscUtils.getRandomPic()), 500, 500));
        //((TextView) headerRoot.findViewById(R.id.appCount)).setText(cursor.getInt(4) + "");

        final int status = cursor.getInt(3);
        cursor.close();

        requestIcon = (ImageView) v.findViewById(R.id.profileIcon);
        text1 = (TextView) v.findViewById(R.id.text1);
        text2 = (TextView) v.findViewById(R.id.text2);
        sendButton = (TextView) v.findViewById(R.id.sendButton);

        if (status == ReachFriendsHelper.Status.REQUEST_SENT_NOT_GRANTED.getValue()) {
            sendButton.setOnClickListener(cancelRequest);
            setRequestSent();
        } else
            sendButton.setOnClickListener(sendRequest);

        return v;
    }

    private static long userId = 0;

    private SharedPreferences sharedPreferences;

    public static ProfileFragment openProfile(long userId, Context context) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("userId", userId);
        fragment.setArguments(bundle);
        return fragment;
    }

    private TextView sendButton;
    private ImageView requestIcon;
    private TextView text1, text2;

    private final ExecutorService requestSender = MiscUtils.getRejectionExecutor();

    private final View.OnClickListener sendRequest = view -> {

        //send friend request
        new SendRequest(this).executeOnExecutor(
                requestSender,
                userId,
                SharedPrefUtils.getServerId(sharedPreferences));



        //show in view
        //setRequestSent();
    };

    private final View.OnClickListener cancelRequest = view -> {

        new RemoveFriend().execute(userId, SharedPrefUtils.getServerId(sharedPreferences));

        //update locally
        final ContentValues values = new ContentValues();
        values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.Status.REQUEST_NOT_SENT.getValue());
        getActivity().getContentResolver().update(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                values,
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""});

        //show in view
        setRequestNotSent();
    };

    private static class RemoveFriend extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... params) {
            try {
                final reach.backend.entities.userApi.model.MyString response = StaticData.USER_API.removeFriend(params[0], params[1]).execute();
                return !(response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false"));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean)
                Log.d("Ashish", "Friend removed");
        }
    }

    private void setRequestSent() {

        if (requestIcon != null) {
            requestIcon.setImageResource(R.drawable.icon_pending_invite_white);
        }
        if (text1 != null)
            text1.setText("Looks like the user has not accepted your request yet");
        if (text2 != null)
            text2.setText("Friend request pending");
        if (sendButton != null) {
            sendButton.setText("CANCEL FRIEND REQUEST");
            sendButton.setOnClickListener(cancelRequest);
        }
    }

    private void setRequestNotSent() {

        if (requestIcon != null)
            requestIcon.setImageResource(R.drawable.ic_add_friend_white);
        if (text1 != null)
            text1.setText("You need to be friends before you can access their collections");
        if (text2 != null)
            text2.setText("Do you wish to send a request to this user?");
        if (sendButton != null) {
            sendButton.setText("SEND FRIEND REQUEST");
            sendButton.setOnClickListener(sendRequest);
        }
    }

    private static final class SendRequest extends AsyncTask<Long, Void, Long> {

        private WeakReference<ProfileFragment> profileFragmentWeakReference;
        public SendRequest(ProfileFragment profileFragment) {
            profileFragmentWeakReference = new WeakReference<>(profileFragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MiscUtils.useFragment(profileFragmentWeakReference, fragment -> {
                Log.d(TAG, "onPreExecute: Disabling send button");
                fragment.sendButton.setEnabled(false);
                fragment.progress_bar.setVisibility(View.VISIBLE);
            });

        }

        @Override
        protected Long doInBackground(final Long... params) {

            /**
             * params[0] = other id
             * params[1] = my id
             * params[2] = status
             */

            final MyString dataAfterWork = MiscUtils.autoRetry(
                    () -> StaticData.MESSAGING_API.requestAccess(params[1], params[0]).execute(),
                    Optional.of(input -> (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false")))).orNull();

            return MiscUtils.useFragment(profileFragmentWeakReference, fragment -> {
                final String toParse;
                if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()) || toParse.equals("false")){
                    final ContentValues values = new ContentValues();
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.Status.REQUEST_NOT_SENT.getValue());

                    //response becomes the id of failed person
                    fragment.getActivity().getContentResolver().update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + params[0]),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{params[0] + ""});
                    return params[0];
                }

                else {

                    //Log.d(TAG, "doInBackground: Send Request, To Parse = " + toParse);
                    //update locally
                    final ContentValues values = new ContentValues();
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.Status.REQUEST_SENT_NOT_GRANTED.getValue());
                    fragment.getActivity().getContentResolver().update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{userId + ""});

                    return null;
                }
            }).or(0L);
        }

        @Override
        protected void onPostExecute(final Long response) {

            super.onPostExecute(response);

            MiscUtils.useFragment(profileFragmentWeakReference, fragment -> {
                if (response != null && response > 0) {
                    fragment.setRequestNotSent();
                    Toast.makeText(fragment.getActivity(), "Request Failed", Toast.LENGTH_SHORT).show();
                }
                else if(response == null){
                    fragment.setRequestSent();
                }

                Log.d(TAG, "onPostExecute: enabling send button");
                fragment.sendButton.setEnabled(true);
                fragment.progress_bar.setVisibility(View.INVISIBLE);
            });
        }
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
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

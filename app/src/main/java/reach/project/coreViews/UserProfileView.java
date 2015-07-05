package reach.project.coreViews;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.messaging.model.MyString;
import reach.backend.entities.userApi.model.ReachFriend;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.viewHelpers.CircleTransform;

public class UserProfileView extends Fragment {

    public UserProfileView() {
        // Required empty public constructor
    }
    private SuperInterface mListener;

    private static WeakReference<UserProfileView> reference;
    public static UserProfileView newInstance(long id) {

        final Bundle args;
        UserProfileView userProfileView;
        if(reference == null || (userProfileView = reference.get()) == null) {
            reference = new WeakReference<>(userProfileView = new UserProfileView());
            userProfileView.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing UserProfileView object :)");
            args = userProfileView.getArguments();
        }

        args.putLong("id", id);
        return userProfileView;
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement goLibraryListener");
        }
    }

    //TODO automatic update here on request accept
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_user_profle_view, container, false);
        final long userId = getArguments().getLong("id");
        final TextView userName = (TextView) rootView.findViewById(R.id.userName);
        final TextView numberOfSongs = (TextView) rootView.findViewById(R.id.numberOfSongs);
        final ImageView userImage = (ImageView) rootView.findViewById(R.id.userImage);
        final TextView lastActive = (TextView) rootView.findViewById(R.id.lastActive);
        final Button access = (Button) rootView.findViewById(R.id.accessRequest);

        final Cursor friendsCursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                ReachFriendsHelper.projection,
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        if (friendsCursor == null) return rootView;
        if(!friendsCursor.moveToFirst()) {
            friendsCursor.close();
            return rootView;
        }
        final ReachFriend reachFriendsDatabase = ReachFriendsHelper.cursorToProcess(friendsCursor);
        friendsCursor.close();
        if (reachFriendsDatabase == null) return rootView;
        if (((ActionBarActivity)getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(reachFriendsDatabase.getUserName());
        userName.setText(reachFriendsDatabase.getUserName());
        numberOfSongs.setText(reachFriendsDatabase.getNumberofSongs()+" Songs");
        if (!TextUtils.isEmpty(reachFriendsDatabase.getImageId()) && !reachFriendsDatabase.getImageId().equals("hello_world"))
            Picasso.with(container.getContext()).load(StaticData.cloudStorageImageBaseUrl + reachFriendsDatabase.getImageId()).transform(new CircleTransform()).into(userImage);

        if(reachFriendsDatabase.getStatus() < 2) {

            if(reachFriendsDatabase.getStatus() == ReachFriendsHelper.OFFLINE_REQUEST_GRANTED) {

                final long lastActiveTime = System.currentTimeMillis() - reachFriendsDatabase.getLastSeen();
                if(lastActiveTime < StaticData.ONLINE_LIMIT)
                    lastActive.setText("Last Active - "+ MiscUtils.combinationFormatter(reachFriendsDatabase.getLastSeen()) +" ago");
                else
                    lastActive.setText("Offline");
            } else
                lastActive.setText("Online");
            access.setText("Access Library");
            access.setClickable(true);
            access.setBackgroundResource(R.drawable.tour_button_selector);
            access.setTextColor(getResources().getColor(R.color.white));
            access.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.goLibrary(userId);
                }
            });
        } else {

            if(reachFriendsDatabase.getStatus() == ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED) {
                access.setText("Request Sent");
                access.setClickable(false);
                access.setBackgroundResource(R.drawable.button_background_blue);
                access.setTextColor(getResources().getColor(R.color.reach_blue));
            } else {
                access.setText("Request Access");
                access.setClickable(true);
                access.setBackgroundResource(R.drawable.profile_button_selector);
                access.setTextColor(getResources().getColorStateList(R.color.profile_text_selector));
                access.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        new SendRequest(access).executeOnExecutor(
                                StaticData.threadPool,
                                reachFriendsDatabase.getId());
                        Toast.makeText(getActivity(),"Access Request sent",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return rootView;
    }

    private final class SendRequest extends AsyncTask<Long, Void, Long> {

        private final Button access;

        public SendRequest(Button access) {
            this.access = access;
        }

        @Override
        protected Long doInBackground(final Long... params) {

            final long serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
            final MyString myString = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {
                    return StaticData.messagingEndpoint.messagingEndpoint().requestAccess(serverId, params[0]).execute();
                }
            }, Optional.<Predicate<MyString>>of(new Predicate<MyString>() {
                @Override
                public boolean apply(MyString input) {
                    return (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false"));
                }
            })).orNull();
            if(myString == null || TextUtils.isEmpty(myString.getString()) || myString.getString().equals("false"))
                return 0L;
            return params[0];
        }

        @Override
        protected void onPostExecute(Long result) {

            super.onPostExecute(result);

            if(isCancelled() || getActivity() == null || getActivity().isFinishing() || access == null)
                return;

            if(result == null || result == 0) {
                Toast.makeText(getActivity(), "Request Failed", Toast.LENGTH_SHORT).show();
            } else {
                //at least the server got updated, toggle the view
                access.setClickable(false);
                access.setBackgroundResource(R.drawable.button_background_blue);
                access.setText("Request Sent");
                access.setTextColor(getResources().getColor(R.color.reach_blue));
                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
                getActivity().getContentResolver().update(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + result),
                        values,
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{result + ""});
            }

        }
    }
}

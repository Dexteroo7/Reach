package reach.project.coreViews.friends;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.friendsAdapters.FriendsAdapter;
import reach.project.coreViews.friends.invite.InviteActivity;
import reach.project.coreViews.yourProfile.ProfileActivity;
import reach.project.coreViews.yourProfile.YourProfileActivity;
import reach.project.utils.FireOnce;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.HandOverMessage;

public class FriendsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<FriendsAdapter.ClickData> {

    private static WeakReference<FriendsFragment> reference = null;

    public static FriendsFragment getInstance() {

        FriendsFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || MiscUtils.isFragmentDead(fragment)) {
            Log.i("Ayush", "Creating new instance of contacts list fragment");
            reference = new WeakReference<>(fragment = new FriendsFragment());
        } else
            Log.i("Ayush", "Reusing contacts list fragment object :)");

        return fragment;
    }

    public static final View.OnClickListener INVITE_LISTENER =
            view -> view.getContext().startActivity(new Intent(view.getContext(), InviteActivity.class));

    @Nullable
    private FriendsAdapter friendsAdapter = null;
    @Nullable
    private View rootView = null;

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d("Ashish", "FriendsFragment - onCreate");
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        Log.d("Ashish", "FriendsFragment - onDestroy");
//    }

    @Override
    public void onDestroyView() {

        Log.i("Ayush", "Destroying contacts view");

        if (friendsAdapter != null) {

            friendsAdapter.setHorizontalCursor(null);
            friendsAdapter.setVerticalCursor(null);
        }

        friendsAdapter = null;
        getLoaderManager().destroyLoader(StaticData.FRIENDS_VERTICAL_LOADER);
        getLoaderManager().destroyLoader(StaticData.FRIENDS_HORIZONTAL_LOADER);

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d("Ashish", "FriendsFragment - onCreateView");

        final Activity activity = getActivity();
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final long serverId = SharedPrefUtils.getServerId(sharedPreferences);

        rootView = inflater.inflate(R.layout.fragment_friends, container, false);
        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.myReachToolbar);
        mToolbar.setTitle("Friends");
        mToolbar.inflateMenu(R.menu.pager_menu);
        mToolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);

        //gridView = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.contactsList));
        //gridView.setOnItemClickListener(LocalUtils.clickListener);
        //gridView.setOnScrollListener(scrollListener);

        final RelativeLayout inviteContainer = (RelativeLayout) rootView.findViewById(R.id.inviteContainer);
        inviteContainer.setOnClickListener(INVITE_LISTENER);
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.contactsList);
        friendsAdapter = new FriendsAdapter(this.getContext(), this);
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(activity, 2);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int itemType = friendsAdapter.getItemViewType(position);
                if (itemType == FriendsAdapter.VIEW_TYPE_LOCKED)
                    return 2;
                else
                    return 1;
            }
        };
        spanSizeLookup.setSpanIndexCacheEnabled(true);
        gridLayoutManager.setSpanSizeLookup(spanSizeLookup);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(friendsAdapter);

        if (MiscUtils.isOnline(activity))
            FireOnce.sendPing(
                    null,
                    new WeakReference<>(getActivity().getContentResolver()),
                    serverId);

        getLoaderManager().initLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.FRIENDS_HORIZONTAL_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.FRIENDS_VERTICAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    FriendsAdapter.REQUIRED_PROJECTION,
                    ReachFriendsHelper.COLUMN_STATUS + " != ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""},
                    ReachFriendsHelper.COLUMN_USER_NAME + " COLLATE NOCASE ASC");
        else if (id == StaticData.FRIENDS_HORIZONTAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    FriendsAdapter.REQUIRED_PROJECTION,
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""}, null);
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || friendsAdapter == null)
            return;

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(data);

        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (friendsAdapter == null)
            return;
        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(null);
        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(null);
    }

    @Override
    public void handOverMessage(@NonNull FriendsAdapter.ClickData clickData) {

        if (rootView == null)
            return;

        if (clickData.status < ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED) {

            Log.i("Ayush", "Detected status" + clickData.status);
            YourProfileActivity.openProfile(clickData.friendId, getActivity());
            if (clickData.networkType == 5)
                Snackbar.make(rootView, "The user has disabled Uploads", Snackbar.LENGTH_LONG).show();

        } else
            ProfileActivity.openProfile(clickData.friendId, getActivity());
    }

    @Nullable
    private SuperInterface mListener;

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
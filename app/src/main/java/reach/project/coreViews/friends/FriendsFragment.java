package reach.project.coreViews.friends;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.invite.InviteActivity;
import reach.project.utils.FireOnce;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.HandOverMessage;

public class FriendsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<ClickData> {


    private static final String TAG = FriendsFragment.class.getSimpleName();
    private SearchView searchView;
    public static final View.OnClickListener INVITE_LISTENER =
            view -> view.getContext().startActivity(new Intent(view.getContext(), InviteActivity.class));
    public static boolean DISPLAY_FRIEND = false;
    public static long FRIEND_ID = 0;

    @Nullable
    private FriendsAdapter friendsAdapter = null;
    @Nullable
    private View rootView = null;
    @Nullable
    private SuperInterface mListener = null;

    final Bundle bundle = new Bundle();

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
    public void onResume() {
        super.onResume();
        if(DISPLAY_FRIEND){
            if(FRIEND_ID!=0) {
                mListener.displayYourProfileFragment(FRIEND_ID);
            }
            DISPLAY_FRIEND = false;
            FRIEND_ID = 0;
        }
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
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchViewMenuItem = mToolbar.getMenu().findItem(R.id.search);
        searchView = (SearchView) searchViewMenuItem.getActionView();
        searchView.setQueryHint("Search Friends");
        //gridView = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.contactsList));
        //gridView.setOnItemClickListener(LocalUtils.clickListener);
        //gridView.setOnScrollListener(scrollListener);

        final RelativeLayout inviteContainer = (RelativeLayout) rootView.findViewById(R.id.inviteContainer);
        inviteContainer.setOnClickListener(INVITE_LISTENER);
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.contactsList);
        friendsAdapter = new FriendsAdapter(this, sharedPreferences);
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(activity, 2);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int itemType = friendsAdapter.getItemViewType(position);
                if (itemType == FriendsAdapter.VIEW_TYPE_LOCKED || itemType == FriendsAdapter.VIEW_TYPE_EMPTY_OR_ERROR)
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
                    new WeakReference<>(activity.getContentResolver()),
                    serverId);

        getLoaderManager().initLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.FRIENDS_HORIZONTAL_LOADER, null, this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                                return false;
                            }

                                @Override
                        public boolean onQueryTextChange(String newText) {

                                        if(newText == null){
                                        return true;
                                    }

                                final String constraint = "%" + newText.toLowerCase() + "%";
                                bundle.putString("filter",constraint );
                                getLoaderManager().restartLoader(StaticData.FRIENDS_VERTICAL_LOADER, bundle, FriendsFragment.this);
                                getLoaderManager().restartLoader(StaticData.FRIENDS_HORIZONTAL_LOADER, bundle, FriendsFragment.this);
                                return true;
                            }
                    });

                        MenuItemCompat.setOnActionExpandListener(searchViewMenuItem, new MenuItemCompat.OnActionExpandListener() {
                                @Override
                                public boolean onMenuItemActionExpand(MenuItem item) {
                                        Log.d(TAG, "onMenuItemActionExpand: searchview frag is now visible");
                                        if(friendsAdapter!=null){
                                                friendsAdapter.DisplayEmptyImageView(false);
                                            }
                                        return true;
                                    }

                                        @Override
                                public boolean onMenuItemActionCollapse(MenuItem item) {
                                        Log.d(TAG, "onMenuItemActionCollapse: searchview frag is now invisible");
                                        if(friendsAdapter!=null){
                                                friendsAdapter.DisplayEmptyImageView(true);
                                            }
                                        return true;
                                    }
                            });

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.FRIENDS_VERTICAL_LOADER) {
            if (args == null) {
                return new CursorLoader(getActivity(),
                        ReachFriendsProvider.CONTENT_URI,
                        FriendsAdapter.REQUIRED_PROJECTION,
                        ReachFriendsHelper.COLUMN_STATUS + " != ?",
                        new String[]{ReachFriendsHelper.Status.REQUEST_NOT_SENT.getString()},
                        ReachFriendsHelper.COLUMN_USER_NAME + " COLLATE NOCASE ASC");
            } else {
                Log.d(TAG, "filter : " + args.getString("filter"));
                return new CursorLoader(getActivity(),
                        ReachFriendsProvider.CONTENT_URI,
                        FriendsAdapter.REQUIRED_PROJECTION,
                        ReachFriendsHelper.COLUMN_STATUS + " != ? and " + ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ? ",
                        new String[]{ReachFriendsHelper.Status.REQUEST_NOT_SENT.getString(),
                                args.getString("filter")},
                        ReachFriendsHelper.COLUMN_USER_NAME + " COLLATE NOCASE ASC");
            }
        }
        else if (id == StaticData.FRIENDS_HORIZONTAL_LOADER) {
            if(args == null) {
                return new CursorLoader(getActivity(),
                        ReachFriendsProvider.CONTENT_URI,
                        FriendsAdapter.REQUIRED_PROJECTION,
                        ReachFriendsHelper.COLUMN_STATUS + " = ?",
                        new String[]{ReachFriendsHelper.Status.REQUEST_NOT_SENT.getString()}, null);
            }
            else{
                return new CursorLoader(getActivity(),
                        ReachFriendsProvider.CONTENT_URI,
                        FriendsAdapter.REQUIRED_PROJECTION,
                        ReachFriendsHelper.COLUMN_STATUS + " = ? and " + ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ? ",
                        new String[]{ReachFriendsHelper.Status.REQUEST_NOT_SENT.getString(),args.getString("filter")}, null);
            }
        }
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || friendsAdapter == null)
            return;

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER) {
            //if(StaticData.friendsCount < data.getCount()){
            //  MyProfileActivity.countChanged = true;
            StaticData.friendsCount = data.getCount();
            //}
            friendsAdapter.setVerticalCursor(data);
        } else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
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
    public void handOverMessage(@NonNull ClickData clickData) {

        if (rootView == null)
            return;

        final ReachActivity activity = (ReachActivity) getActivity();

        if (clickData.status < ReachFriendsHelper.Status.REQUEST_SENT_NOT_GRANTED.getValue()) {
            Log.i("Ayush", "Detected status" + clickData.status);
            //YourProfileActivity.openProfileWithPlayer(clickData.friendId, activity, activity.player.getCurrentTimeMillis(), activity.currentYTId);
            mListener.displayYourProfileFragment(clickData.friendId);
        } else {
            //ProfileActivity.openProfile(clickData.friendId, activity);
            mListener.displayProfileFragment(clickData.friendId);
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

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.pager_menu,menu);
        MenuItem useOnlyWifi =  menu.findItem(R.id.hello);
        if(SharedPrefUtils.getMobileData(sharedPreferences)) {
            useOnlyWifi.setChecked(true);
        }
        else{
            useOnlyWifi.setChecked(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }*/
}
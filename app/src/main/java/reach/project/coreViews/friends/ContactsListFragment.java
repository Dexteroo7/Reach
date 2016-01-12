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
import android.support.v4.widget.SwipeRefreshLayout;
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


public class ContactsListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<FriendsAdapter.ClickData> {

    private static long serverId;

    private static WeakReference<ContactsListFragment> reference = null;

    public static ContactsListFragment getInstance() {

        ContactsListFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || MiscUtils.isFragmentDead(fragment)) {
            Log.i("Ayush", "Creating new instance of contacts list fragment");
            reference = new WeakReference<>(fragment = new ContactsListFragment());
        } else
            Log.i("Ayush", "Reusing contacts list fragment object :)");

        return fragment;
    }

    private final FriendsAdapter friendsAdapter = new FriendsAdapter(this);

    private View rootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Ashish", "ContactsListFragment - onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Ashish", "ContactsListFragment - onDestroy");
    }

    @Override
    public void onDestroyView() {

        Log.i("Ayush", "Destroying contacts view");

        friendsAdapter.setHorizontalCursor(null);
        friendsAdapter.setVerticalCursor(null);
        getLoaderManager().destroyLoader(StaticData.FRIENDS_VERTICAL_LOADER);
        getLoaderManager().destroyLoader(StaticData.FRIENDS_HORIZONTAL_LOADER);

//        if (inviteAdapter != null)
//            inviteAdapter.cleanUp();

        //listView.setOnScrollListener(null);
        super.onDestroyView();
        Log.d("Ashish", "ContactsListFragment - onDestroyView");
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        Log.d("Ashish", "ContactsListFragment - onCreateView");

        final Activity activity = getActivity();

                /*if (getArguments().getBoolean("first", false))
            new InfoDialog().show(getChildFragmentManager(),"info_dialog");*/
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);

        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.myReachToolbar);
        mToolbar.setTitle("Friends");
        mToolbar.inflateMenu(R.menu.pager_menu);
        mToolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);

        //gridView = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.contactsList));
        //gridView.setOnItemClickListener(LocalUtils.clickListener);
        //gridView.setOnScrollListener(scrollListener);

        final RelativeLayout inviteContainer = (RelativeLayout) rootView.findViewById(R.id.inviteContainer);
        inviteContainer.setOnClickListener(inviteListener);
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.contactsList);
        final GridLayoutManager manager = new GridLayoutManager(activity, 2);

        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                final int itemType = friendsAdapter.getItemViewType(position);
                if (itemType == FriendsAdapter.VIEW_TYPE_FRIEND_LARGE || itemType == FriendsAdapter.VIEW_TYPE_LOCKED)
                    return 2;
                else
                    return 1;
            }
        });
        recyclerView.setLayoutManager(manager);
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
                    FriendsAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " != ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""},
                    ReachFriendsHelper.COLUMN_STATUS + " ASC, " + ReachFriendsHelper.COLUMN_USER_NAME + " COLLATE NOCASE ASC");
        else if (id == StaticData.FRIENDS_HORIZONTAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    FriendsAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""}, null);
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(data);

        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

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

        } else {

            ProfileActivity.openProfile(clickData.friendId, getActivity());
        }
        /*else if (clickData.status == 3) {

            final AlertDialog alertDialog = new AlertDialog.Builder(rootView.getContext())
                    .setMessage("Send a friend request to " + clickData.userName + " ?")
                    .setPositiveButton("Yes", LocalUtils.positiveButton)
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    }).create();

            alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(
                    //set tag to use when positive button click
                    new Object[]{clickData.friendId, clickData.status, new WeakReference<>(rootView)}
            ));
            alertDialog.show();
        }*/
    }

    public final SwipeRefreshLayout.OnRefreshListener refreshListener = () -> {

        //TODO
//            if (MiscUtils.isOnline(activity))
//                FireOnce.sendPing(
//                        null,
//                        new WeakReference<>(getActivity().getContentResolver()),
//                        serverId);
    };

    public final View.OnClickListener inviteListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            view.getContext().startActivity(new Intent(view.getContext(), InviteActivity.class));
        }
    };

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
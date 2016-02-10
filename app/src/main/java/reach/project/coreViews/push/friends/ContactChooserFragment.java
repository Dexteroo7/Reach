package reach.project.coreViews.push.friends;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.push.ContactChooserInterface;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * A placeholder fragment containing a simple view.
 */
public class ContactChooserFragment extends Fragment implements HandOverMessage<Cursor>, LoaderManager.LoaderCallbacks<Cursor> {

    @Nullable
    private ContactChooserInterface chooserInterface = null;
    @Nullable
    private static WeakReference<ContactChooserFragment> reference;

    public static ContactChooserFragment getInstance() {

        ContactChooserFragment fragment;
        Log.i("Ayush", "Creating new instance of contacts list fragment");
        reference = new WeakReference<>(fragment = new ContactChooserFragment());

        return fragment;
    }

    private final ContactChooserAdapter contactChooserAdapter = new ContactChooserAdapter(this, R.layout.push_contact_item);

    public void handOverMessage(@Nonnull Cursor message) {

        final long userId = message.getLong(0);
        final boolean selected = contactChooserAdapter.selectedUsers.contains(userId);

        if (selected) {

            //de-select user
            contactChooserAdapter.selectedUsers.remove(userId);
            contactChooserAdapter.notifyItemChanged(message.getPosition());

        } else {

            //select user
            contactChooserAdapter.selectedUsers.add(userId);
            contactChooserAdapter.notifyItemChanged(message.getPosition());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_contact_chooser, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.chooserToolbar);
        toolbar.setTitle("Choose Friends");
        toolbar.inflateMenu(R.menu.menu_push);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {

                case R.id.push_button:

                    if (contactChooserAdapter.selectedUsers.size() == 0) {

                        Toast.makeText(activity, "Select some friends", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    final long [] serverIds = new long[contactChooserAdapter.selectedUsers.size()];
                    final Iterator<Long> longIterator = contactChooserAdapter.selectedUsers.iterator();

                    int index = 0;
                    while (longIterator.hasNext())
                        serverIds[index++] = longIterator.next();

                    if (chooserInterface != null)
                        chooserInterface.switchToMessageWriter(serverIds);
                    return true;
            }
            return false;
        });
        toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());

        mRecyclerView.setLayoutManager(new CustomGridLayoutManager(activity, 2));
        mRecyclerView.setAdapter(contactChooserAdapter);

        getLoaderManager().initLoader(StaticData.CONTACTS_CHOOSER_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        contactChooserAdapter.setCursor(null);
        getLoaderManager().destroyLoader(StaticData.CONTACTS_CHOOSER_LOADER);
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            chooserInterface = (ContactChooserInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        chooserInterface = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.CONTACTS_CHOOSER_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    contactChooserAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " < ?",
                    new String[]{ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED + ""},
                    ReachFriendsHelper.COLUMN_USER_NAME + " COLLATE NOCASE ASC");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() == StaticData.CONTACTS_CHOOSER_LOADER && data != null && !data.isClosed())
            contactChooserAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.CONTACTS_CHOOSER_LOADER)
            contactChooserAdapter.setCursor(null);
    }
}

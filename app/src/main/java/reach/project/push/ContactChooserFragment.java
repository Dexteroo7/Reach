package reach.project.push;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
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
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Ayush", "Creating new instance of contacts list fragment");
            reference = new WeakReference<>(fragment = new ContactChooserFragment());
        } else
            Log.i("Ayush", "Reusing ContactChooserFragment fragment object :)");

        return fragment;
    }

    private final ContactChooserAdapter contactChooserAdapter = new ContactChooserAdapter(this, R.layout.myreach_item);
    private final View.OnClickListener clickListener = v -> {

        final long [] serverIds = new long[contactChooserAdapter.selectedUsers.size()];
        final Iterator<Long> longIterator = contactChooserAdapter.selectedUsers.iterator();

        int index = 0;
        while (longIterator.hasNext())
            serverIds[index++] = longIterator.next();

        if (chooserInterface != null)
            chooserInterface.switchToMessageWriter(serverIds);
    };

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
        final View proceed = rootView.findViewById(R.id.proceed);
        final Activity activity = getActivity();

        proceed.setOnClickListener(clickListener);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
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
                    new String[]{ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED + ""}, null);
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
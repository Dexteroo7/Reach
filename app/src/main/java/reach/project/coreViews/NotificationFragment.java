package reach.project.coreViews;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.lang.ref.WeakReference;

import reach.project.R;

public class NotificationFragment extends Fragment {

    private static WeakReference<NotificationFragment> reference = null;
    public static NotificationFragment newInstance() {

        NotificationFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());
        return fragment;
    }
    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = (ListView) rootView.findViewById(R.id.listView);

        return rootView;
    }

}

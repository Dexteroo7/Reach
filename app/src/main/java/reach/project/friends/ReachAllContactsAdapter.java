package reach.project.friends;

/**
 * Created by dexter on 1/8/14.
 */

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CircleTransform;

public abstract class ReachAllContactsAdapter extends ArrayAdapter<Contact> {

    private final Context context;
    private final int layoutResourceId;
    private final List<Contact> originalData;
    private final List<Contact> filteredData;
    private final CircleTransform transform = new CircleTransform();

    public ReachAllContactsAdapter(Context context, int ResourceId,
                                   List<Contact> friends) {

        super(context, ResourceId, friends);
        this.context = context;
        this.layoutResourceId = ResourceId;
        this.originalData = friends;
        this.filteredData = new ArrayList<>(friends);
    }

    public int getCount() {
        return filteredData.size();
    }

    public Contact getItem(int position) {
        return filteredData.get(position);
    }

    private final class ViewHolder {

        private final TextView userNameList, userInitials, subTitle;
        private final ImageView profilePhotoList, listToggle;

        private ViewHolder(TextView userNameList, TextView userInitials, TextView subTitle, ImageView profilePhotoList, ImageView listToggle) {
            this.userNameList = userNameList;
            this.userInitials = userInitials;
            this.subTitle = subTitle;
            this.profilePhotoList = profilePhotoList;
            this.listToggle = listToggle;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Contact contact = getItem(position);
        final ViewHolder viewHolder;

        if (convertView == null) {

            convertView = ((Activity) context).getLayoutInflater().inflate(layoutResourceId, parent, false);

            viewHolder = new ViewHolder(
                    (TextView) convertView.findViewById(R.id.userNameList),
                    (TextView) convertView.findViewById(R.id.userInitials),
                    (TextView) convertView.findViewById(R.id.listSubTitle),
                    (ImageView) convertView.findViewById(R.id.profilePhotoList),
                    (ImageView) convertView.findViewById(R.id.listToggle));
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.userNameList.setText(contact.getUserName());
        viewHolder.subTitle.setText(contact.getPhoneNumber());
        viewHolder.userInitials.setText(MiscUtils.generateInitials(contact.getUserName()));
        Picasso.with(context).load(contact.getPhotoUri()).transform(transform).into(viewHolder.profilePhotoList);

        if (contact.isInviteSent())
            Picasso.with(context).load(R.drawable.add_tick).into(viewHolder.listToggle);
        else
            Picasso.with(context).load(R.drawable.icon_invite).into(viewHolder.listToggle);
        return convertView;
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                final FilterResults results = new FilterResults();
                if (TextUtils.isEmpty(charSequence)) {

                    results.values = originalData;
                    results.count = originalData.size();
                    Log.i("Ayush", "Filtering with original data");
                } else {

                    final List<Contact> filterResultsData = new ArrayList<>();
                    //noinspection Convert2streamapi
                    for (Contact data : originalData) {
                        if (data.getUserName().toLowerCase().contains(charSequence.toString().toLowerCase()))
                            filterResultsData.add(data);
                    }
                    results.values = filterResultsData;
                    results.count = filterResultsData.size();
                    Log.i("Ayush", "Filtering with " + charSequence);
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                if (filterResults.values instanceof List) {

                    if (((List) filterResults.values).size() == 0)
                        onEmptyContacts();

                    filteredData.clear();
                    for (Object contact : (List) filterResults.values) {
                        if (contact instanceof Contact)
                            filteredData.add((Contact) contact);
                    }
                    notifyDataSetChanged();
                }
            }

        };
    }

    protected abstract void onEmptyContacts();
}
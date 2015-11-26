package reach.project.coreViews.fileManager;

import android.app.Activity;
import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import reach.backend.entities.userApi.model.CompletedOperation;
import reach.project.R;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachUploadAdapter extends ArrayAdapter<CompletedOperation> {

    private final int layoutResourceId;
    private final LongSparseArray<String> friends;
    private final SparseArray<String> operationSize = new SparseArray<>();

    public ReachUploadAdapter(Context context, int ResourceId, List<CompletedOperation> list, LongSparseArray<String> friends) {

        super(context, ResourceId, list);
        this.layoutResourceId = ResourceId;
        this.friends = friends;
    }

    private final class ViewHolder{

        private final TextView songTitle,songSize,userName;
        private ViewHolder(TextView songTitle, TextView songSize, TextView userName) {
            this.songTitle = songTitle;
            this.songSize = songSize;
            this.userName = userName;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;
        if(convertView==null){

            convertView = ((Activity) parent.getContext()).getLayoutInflater().inflate(layoutResourceId, parent, false);
            viewHolder = new ViewHolder(
                    (TextView) convertView.findViewById(R.id.songTitle),
                    (TextView) convertView.findViewById(R.id.songSize),
                    (TextView) convertView.findViewById(R.id.userName));
            convertView.setTag(viewHolder);
        }
        else
            viewHolder = (ViewHolder) convertView.getTag();

        final CompletedOperation operation = getItem(position);
        int index = operation.getReceiver().size() - 1;
        String name = "";
        while (index >= 0 && TextUtils.isEmpty(name = friends.get(operation.getReceiver().get(index), null)))
            index--;
        viewHolder.userName.setText("to " + name);

        String container = operationSize.get(operation.hashCode(), null);
        if(TextUtils.isEmpty(container)) {
            container = String.format("%.1f", (float)(operation.getSongSize() / 1024000.0f)) + " MB";
            operationSize.append(getHashCode(operation), container);
        }
        viewHolder.songSize.setText(container);
        viewHolder.songTitle.setText(operation.getSongName());
        return convertView;
    }

    public int getHashCode(CompletedOperation operation) {
        int result = operation.getSongName() != null ? operation.getSongName().hashCode() : 0;
        result = 31 * result + (int) (operation.getSongSize() ^ (operation.getSongSize() >>> 32));
        result = 31 * result + (int) (operation.getSenderId() ^ (operation.getSenderId() >>> 32));
        return result;
    }
}

package reach.project.utils.viewHelpers;

import android.content.Context;
import android.util.AttributeSet;

import com.facebook.drawee.view.SimpleDraweeView;

/**
 * Created by ashish on 15/01/16.
 */
public class CustomDraweeView extends SimpleDraweeView {
    public CustomDraweeView(Context context) {
        super(context);
    }

    public CustomDraweeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDraweeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}

//package reach.project.utils.viewHelpers;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapShader;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//
///**
// * Created by ashish on 17/2/15.
// */
//public class CircleTransform implements Transformation {
//
//    @Override
//    public Bitmap transform(Bitmap source) {
//
//        if (source == null)
//            return null;
//
//        final int size = Math.min(source.getWidth(), source.getHeight());
//        final int x = (source.getWidth() - size) / 2;
//        final int y = (source.getHeight() - size) / 2;
//
//        final Bitmap squaredBitmap;
//        try {
//            squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
//        } catch (NullPointerException ignored) {
//            return source;
//        }
//
//        if (squaredBitmap == null)
//            return source;
//
//        if (squaredBitmap != source)
//            source.recycle();
//
//        final Bitmap bitmap;
//        try {
//            bitmap = Bitmap.createBitmap(size, size, source.getConfig());
//        } catch (NullPointerException ignored) {
//            return squaredBitmap;
//        }
//
//        final Paint paint = new Paint();
//        paint.setShader(new BitmapShader(squaredBitmap,
//                BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
//        paint.setAntiAlias(true);
//        final float r = size / 2f;
//        new Canvas(bitmap).drawCircle(r, r, r, paint);
//        squaredBitmap.recycle();
//        return bitmap;
//    }
//
//    @Override
//    public String key() {
//        return "circle";
//    }
//}

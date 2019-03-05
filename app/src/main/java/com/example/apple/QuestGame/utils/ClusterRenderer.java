package com.example.apple.QuestGame.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.Marker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

public class ClusterRenderer extends DefaultClusterRenderer<Marker> {

    private final IconGenerator iconGenerator;
    private final ImageView imageView;
    private final int markerWidth;
    private final int markerHeight;
    private Context mContext;

    public ClusterRenderer(Context context, GoogleMap map, ClusterManager<Marker> clusterManager) {
        super(context, map, clusterManager);

        mContext = context;
        iconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());
        markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
        iconGenerator.setContentView(imageView);
        iconGenerator.setBackground(ContextCompat.getDrawable(mContext.getApplicationContext(), R.drawable.circle_shape));
    }

    @Override
    protected void onBeforeClusterItemRendered(Marker item, MarkerOptions markerOptions) {
        imageView.setImageResource(item.getIconPicture());
        final Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }
}

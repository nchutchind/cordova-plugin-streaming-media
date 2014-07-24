package com.hutchind.cordova.plugins.streamingmedia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.content.Context;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;


public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

	private String uri;
	private ImageView imageView;
	private Context context;

	public ImageLoadTask(String uri, ImageView imageView, Context context) {
		this.uri = uri;
		this.imageView = imageView;
		this.context = context;
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		Bitmap bitmap = null;
		if (uri != null && uri.toLowerCase().startsWith("http")) {
			// Load image from URL
			try {
				URL urlConnection = new URL(uri);
				HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
				connection.setDoInput(true);
				connection.connect();
				InputStream input = connection.getInputStream();
				bitmap = BitmapFactory.decodeStream(input);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// Load image from assets
			InputStream istr;
			try {
				istr = context.getAssets().open(uri);
				bitmap = BitmapFactory.decodeStream(istr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		imageView.setImageBitmap(result);
	}
}
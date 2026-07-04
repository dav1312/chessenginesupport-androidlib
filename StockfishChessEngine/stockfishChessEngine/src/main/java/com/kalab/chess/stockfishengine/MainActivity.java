package com.kalab.chess.stockfishengine;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Toast.makeText(this, "Stockfish Engine ready. Please select this engine inside your Chess GUI settings.", Toast.LENGTH_LONG).show();
		finish();
	}
}

package com.kalab.chess.stockfishengine;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Chess GUI Buttons
		Button btnChessis = findViewById(R.id.btnChessis);
		Button btnDroidFish = findViewById(R.id.btnDroidFish);
		Button btnWikiMobile = findViewById(R.id.btnWikiMobile);

		// Official Resource Buttons
		Button btnDocs = findViewById(R.id.btnDocs);
		Button btnDiscord = findViewById(R.id.btnDiscord);
		Button btnLicense = findViewById(R.id.btnLicense);

		btnChessis.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://play.google.com/store/apps/details?id=com.chessimprovement.chessis");
			}
		});

		btnDroidFish.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://f-droid.org/packages/org.petero.droidfish/");
			}
		});

		btnWikiMobile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://official-stockfish.github.io/docs/stockfish-wiki/Download-and-usage.html#mobile");
			}
		});

		btnDocs.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://official-stockfish.github.io/docs/");
			}
		});

		btnDiscord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://discord.gg/GWDRS3kU6R");
			}
		});

		btnLicense.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://github.com/official-stockfish/Stockfish/blob/master/Copying.txt");
			}
		});
	}

	private void openUrl(String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(intent);
	}
}

package com.kalab.chess.stockfishengine;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

	private static final String CHANNEL_ID = "stockfish_releases_channel";
	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	private ProgressBar progressReleases;
	private TextView txtReleasesError;
	private Button btnRetryReleases;
	private LinearLayout containerReleases;

	private ProgressBar progressBlog;
	private TextView txtBlogError;
	private Button btnRetryBlog;
	private LinearLayout containerBlog;

	public static class ReleaseItem {
		public String name = "";
		public boolean prerelease = false;
		public String publishedAt = "";
		public String htmlUrl = "";
		public String body = "";
		public List<AssetItem> assets = new ArrayList<>();
	}

	public static class AssetItem {
		public String name = "";
		public String browserDownloadUrl = "";
	}

	public static class BlogItem {
		public String title = "";
		public String link = "";
		public String pubDate = "";
		public String description = "";
	}

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
		Button btnGitHub = findViewById(R.id.btnGitHub);
		Button btnBluesky = findViewById(R.id.btnBluesky);
		Button btnTwitter = findViewById(R.id.btnTwitter);

		// Release elements
		progressReleases = findViewById(R.id.progressReleases);
		txtReleasesError = findViewById(R.id.txtReleasesError);
		btnRetryReleases = findViewById(R.id.btnRetryReleases);
		containerReleases = findViewById(R.id.containerReleases);

		// Blog elements
		progressBlog = findViewById(R.id.progressBlog);
		txtBlogError = findViewById(R.id.txtBlogError);
		btnRetryBlog = findViewById(R.id.btnRetryBlog);
		containerBlog = findViewById(R.id.containerBlog);

		// Request POST_NOTIFICATIONS permission on Android 13+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
			}
		}

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

		btnGitHub.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://github.com/official-stockfish/Stockfish");
			}
		});

		btnBluesky.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://bsky.app/profile/stockfishchess.org");
			}
		});

		btnTwitter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl("https://x.com/stockfishchess");
			}
		});

		btnRetryReleases.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startFetchReleases();
			}
		});

		btnRetryBlog.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startFetchBlog();
			}
		});

		// Start async loading tasks
		startFetchReleases();
		startFetchBlog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		executor.shutdown();
	}

	private void openUrl(String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(intent);
	}

	private int dpToPx(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}

	private void startFetchReleases() {
		progressReleases.setVisibility(View.VISIBLE);
		txtReleasesError.setVisibility(View.GONE);
		btnRetryReleases.setVisibility(View.GONE);
		containerReleases.removeAllViews();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final String json = fetchUrl("https://api.github.com/repos/official-stockfish/Stockfish/releases?per_page=2");
					final List<ReleaseItem> releases = parseReleasesJson(json);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressReleases.setVisibility(View.GONE);
							if (releases == null || releases.isEmpty()) {
								txtReleasesError.setVisibility(View.VISIBLE);
								btnRetryReleases.setVisibility(View.VISIBLE);
							} else {
								for (ReleaseItem item : releases) {
									containerReleases.addView(createReleaseView(item));
								}
								checkNewReleaseNotification(releases);
							}
						}
					});
				} catch (final Exception e) {
					Log.e("MainActivity", "Error loading releases", e);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressReleases.setVisibility(View.GONE);
							txtReleasesError.setVisibility(View.VISIBLE);
							txtReleasesError.setText("Error loading releases: " + e.getMessage());
							btnRetryReleases.setVisibility(View.VISIBLE);
						}
					});
				}
			}
		});
	}

	private void startFetchBlog() {
		progressBlog.setVisibility(View.VISIBLE);
		txtBlogError.setVisibility(View.GONE);
		btnRetryBlog.setVisibility(View.GONE);
		containerBlog.removeAllViews();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final String xml = fetchUrl("https://stockfishchess.org/blog/index.xml");
					final List<BlogItem> blogItems = parseBlogRss(xml);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressBlog.setVisibility(View.GONE);
							if (blogItems == null || blogItems.isEmpty()) {
								txtBlogError.setVisibility(View.VISIBLE);
								btnRetryBlog.setVisibility(View.VISIBLE);
							} else {
								int count = 0;
								for (BlogItem item : blogItems) {
									containerBlog.addView(createBlogView(item));
									count++;
									if (count >= 3) {
										break;
									}
								}
							}
						}
					});
				} catch (final Exception e) {
					Log.e("MainActivity", "Error loading blog", e);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressBlog.setVisibility(View.GONE);
							txtBlogError.setVisibility(View.VISIBLE);
							txtBlogError.setText("Error loading blog posts: " + e.getMessage());
							btnRetryBlog.setVisibility(View.VISIBLE);
						}
					});
				}
			}
		});
	}

	private String fetchUrl(String urlString) throws IOException {
		HttpURLConnection conn = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; StockfishChessEngineApp)");

			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream in = conn.getInputStream();
				reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
				return sb.toString();
			} else {
				throw new IOException("HTTP code " + responseCode);
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {}
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private List<ReleaseItem> parseReleasesJson(String json) {
		List<ReleaseItem> items = new ArrayList<>();
		try {
			JSONArray array = new JSONArray(json);
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				ReleaseItem release = new ReleaseItem();
				release.name = obj.optString("name", "");
				if (release.name.isEmpty()) {
					release.name = obj.optString("tag_name", "");
				}
				release.prerelease = obj.optBoolean("prerelease", false);
				release.publishedAt = obj.optString("published_at", "");
				release.htmlUrl = obj.optString("html_url", "");
				release.body = obj.optString("body", "");

				JSONArray assetsArray = obj.optJSONArray("assets");
				if (assetsArray != null) {
					for (int j = 0; j < assetsArray.length(); j++) {
						JSONObject assetObj = assetsArray.getJSONObject(j);
						String assetName = assetObj.optString("name", "");
						if (assetName.toLowerCase().contains("android") || assetName.toLowerCase().contains("apk")) {
							AssetItem asset = new AssetItem();
							asset.name = assetName;
							asset.browserDownloadUrl = assetObj.optString("browser_download_url", "");
							release.assets.add(asset);
						}
					}
				}
				items.add(release);
			}
		} catch (Exception e) {
			Log.e("MainActivity", "Error parsing releases JSON", e);
		}
		return items;
	}

	private List<BlogItem> parseBlogRss(String xml) {
		List<BlogItem> items = new ArrayList<>();
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xml));
			int eventType = parser.getEventType();
			BlogItem currentItem = null;
			String text = "";
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String tagName = parser.getName();
				if (eventType == XmlPullParser.START_TAG) {
					if ("item".equalsIgnoreCase(tagName)) {
						currentItem = new BlogItem();
					}
				} else if (eventType == XmlPullParser.TEXT) {
					text = parser.getText();
				} else if (eventType == XmlPullParser.END_TAG) {
					if (currentItem != null) {
						if ("item".equalsIgnoreCase(tagName)) {
							items.add(currentItem);
							currentItem = null;
						} else if ("title".equalsIgnoreCase(tagName)) {
							currentItem.title = text.trim();
						} else if ("link".equalsIgnoreCase(tagName)) {
							currentItem.link = text.trim();
						} else if ("pubDate".equalsIgnoreCase(tagName)) {
							currentItem.pubDate = text.trim();
						} else if ("description".equalsIgnoreCase(tagName)) {
							currentItem.description = text.trim();
						}
					}
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			Log.e("MainActivity", "Error parsing RSS XML", e);
		}
		return items;
	}

	private View createReleaseView(final ReleaseItem item) {
		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		cardParams.setMargins(0, 0, 0, dpToPx(12));
		card.setLayoutParams(cardParams);
		card.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
		card.setBackgroundColor(0xFF2E2E35);

		// Header block
		LinearLayout headerLayout = new LinearLayout(this);
		headerLayout.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		headerLayout.setLayoutParams(headerParams);

		TextView txtTitle = new TextView(this);
		txtTitle.setText(item.name);
		txtTitle.setTextColor(0xFF43AD6A);
		txtTitle.setTextSize(16);
		txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				0,
				LinearLayout.LayoutParams.WRAP_CONTENT,
				1.0f
		);
		txtTitle.setLayoutParams(titleParams);
		headerLayout.addView(txtTitle);

		if (item.prerelease) {
			TextView txtBadge = new TextView(this);
			txtBadge.setText("Pre-release");
			txtBadge.setTextColor(0xFFE9A13B);
			txtBadge.setTextSize(12);
			txtBadge.setTypeface(null, android.graphics.Typeface.BOLD);
			txtBadge.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
			txtBadge.setBackgroundColor(0x22E9A13B);
			LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);
			txtBadge.setLayoutParams(badgeParams);
			headerLayout.addView(txtBadge);
		}
		card.addView(headerLayout);

		// Published Date
		TextView txtDate = new TextView(this);
		String dateToShow = item.publishedAt;
		if (dateToShow != null && dateToShow.length() >= 10) {
			dateToShow = dateToShow.substring(0, 10);
		}
		txtDate.setText("Published: " + dateToShow);
		txtDate.setTextColor(0xFF999999);
		txtDate.setTextSize(12);
		LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		dateParams.setMargins(0, dpToPx(4), 0, dpToPx(8));
		txtDate.setLayoutParams(dateParams);
		card.addView(txtDate);

		// Body info
		if (!item.body.isEmpty()) {
			TextView txtBody = new TextView(this);
			String bodySnippet = item.body.trim();
			if (bodySnippet.length() > 200) {
				bodySnippet = bodySnippet.substring(0, 200) + "...";
			}
			txtBody.setText(bodySnippet);
			txtBody.setTextColor(0xFFCCCCCC);
			txtBody.setTextSize(14);
			LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);
			bodyParams.setMargins(0, 0, 0, dpToPx(12));
			txtBody.setLayoutParams(bodyParams);
			card.addView(txtBody);
		}

		// Quick Android download list
		if (!item.assets.isEmpty()) {
			TextView txtAssetsTitle = new TextView(this);
			txtAssetsTitle.setText("Download Android Binaries:");
			txtAssetsTitle.setTextColor(0xFFFFFFFF);
			txtAssetsTitle.setTextSize(13);
			txtAssetsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
			LinearLayout.LayoutParams assetsTitleParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);
			assetsTitleParams.setMargins(0, 0, 0, dpToPx(6));
			txtAssetsTitle.setLayoutParams(assetsTitleParams);
			card.addView(txtAssetsTitle);

			for (final AssetItem asset : item.assets) {
				Button btnAsset = new Button(this);
				btnAsset.setText(asset.name);
				btnAsset.setTextSize(13);
				btnAsset.setTextColor(0xFFFFFFFF);
				btnAsset.setAllCaps(false);
				btnAsset.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
				btnAsset.setBackgroundResource(R.drawable.button_primary);
				btnAsset.setStateListAnimator(null);

				LinearLayout.LayoutParams btnAssetParams = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT
				);
				btnAssetParams.setMargins(0, 0, 0, dpToPx(6));
				btnAsset.setLayoutParams(btnAssetParams);

				btnAsset.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openUrl(asset.browserDownloadUrl);
					}
				});
				card.addView(btnAsset);
			}
		}

		// View Release button
		Button btnView = new Button(this);
		btnView.setText("View Release on GitHub");
		btnView.setTextSize(13);
		btnView.setTextColor(0xFFFFFFFF);
		btnView.setAllCaps(false);
		btnView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
		btnView.setBackgroundResource(R.drawable.button_secondary);
		btnView.setStateListAnimator(null);

		LinearLayout.LayoutParams btnViewParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		btnViewParams.setMargins(0, dpToPx(6), 0, 0);
		btnView.setLayoutParams(btnViewParams);

		btnView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUrl(item.htmlUrl);
			}
		});
		card.addView(btnView);

		return card;
	}

	private View createBlogView(final BlogItem item) {
		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		cardParams.setMargins(0, 0, 0, dpToPx(12));
		card.setLayoutParams(cardParams);
		card.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
		card.setBackgroundColor(0xFF2E2E35);

		// Title
		TextView txtTitle = new TextView(this);
		txtTitle.setText(item.title);
		txtTitle.setTextColor(0xFFFFFFFF);
		txtTitle.setTextSize(16);
		txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		txtTitle.setLayoutParams(titleParams);
		card.addView(txtTitle);

		// Date
		TextView txtDate = new TextView(this);
		txtDate.setText(item.pubDate);
		txtDate.setTextColor(0xFF999999);
		txtDate.setTextSize(12);
		LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		dateParams.setMargins(0, dpToPx(4), 0, dpToPx(8));
		txtDate.setLayoutParams(dateParams);
		card.addView(txtDate);

		// Snippet
		if (!item.description.isEmpty()) {
			TextView txtDesc = new TextView(this);
			CharSequence htmlParsed;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				htmlParsed = Html.fromHtml(item.description, Html.FROM_HTML_MODE_LEGACY);
			} else {
				htmlParsed = Html.fromHtml(item.description);
			}

			String cleanText = htmlParsed.toString().trim();
			if (cleanText.length() > 180) {
				cleanText = cleanText.substring(0, 180) + "...";
			}
			txtDesc.setText(cleanText);
			txtDesc.setTextColor(0xFFCCCCCC);
			txtDesc.setTextSize(14);

			LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);
			descParams.setMargins(0, 0, 0, dpToPx(12));
			txtDesc.setLayoutParams(descParams);
			card.addView(txtDesc);
		}

		// Button to post
		Button btnRead = new Button(this);
		btnRead.setText("Read Blog Post");
		btnRead.setTextSize(13);
		btnRead.setTextColor(0xFFFFFFFF);
		btnRead.setAllCaps(false);
		btnRead.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
		btnRead.setBackgroundResource(R.drawable.button_secondary);
		btnRead.setStateListAnimator(null);

		LinearLayout.LayoutParams btnReadParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		btnRead.setLayoutParams(btnReadParams);

		btnRead.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String fullLink = item.link;
				if (fullLink.startsWith("/")) {
					fullLink = "https://stockfishchess.org" + fullLink;
				}
				openUrl(fullLink);
			}
		});
		card.addView(btnRead);

		return card;
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = "Stockfish Releases";
			String description = "Notifications for new Stockfish releases and pre-releases";
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);
			}
		}
	}

	private void showNotification(String title, String message, String url) {
		createNotificationChannel();

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		PendingIntent pendingIntent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		} else {
			pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		}

		android.app.Notification.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder = new android.app.Notification.Builder(this, CHANNEL_ID);
		} else {
			builder = new android.app.Notification.Builder(this);
		}

		builder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(title)
				.setContentText(message)
				.setAutoCancel(true)
				.setContentIntent(pendingIntent);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			builder.setStyle(new android.app.Notification.BigTextStyle().bigText(message));
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.notify(43, builder.build());
		}
	}

	private void checkNewReleaseNotification(List<ReleaseItem> releases) {
		if (releases == null || releases.isEmpty()) {
			return;
		}
		ReleaseItem latest = releases.get(0);
		String latestTag = latest.name;
		if (latestTag == null || latestTag.isEmpty()) {
			return;
		}

		SharedPreferences prefs = getSharedPreferences("stockfish_prefs", MODE_PRIVATE);
		String storedTag = prefs.getString("last_release_tag", null);

		// Only notify if a tag was already stored previously and is different
		if (storedTag != null && !storedTag.equals(latestTag)) {
			String msg = "New version available: " + latestTag;
			showNotification("Stockfish Chess Engine Update", msg, latest.htmlUrl);
		}

		// Store the latest tag to prevent repeating notifications
		prefs.edit().putString("last_release_tag", latestTag).apply();
	}
}

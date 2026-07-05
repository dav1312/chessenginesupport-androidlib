package com.kalab.chess.stockfishengine;

import android.util.Log;
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

public class StockfishFeedParser {

    private static final String TAG = "StockfishFeedParser";

    public static String fetchUrl(String urlString) throws IOException {
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

    public static List<ReleaseItem> parseReleasesJson(String json) {
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
            Log.e(TAG, "Error parsing releases JSON", e);
        }
        return items;
    }

    public static List<BlogItem> parseBlogRss(String xml) {
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
            Log.e(TAG, "Error parsing RSS XML", e);
        }
        return items;
    }
}

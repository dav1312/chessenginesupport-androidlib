package com.kalab.chess.stockfishengine;

import java.util.ArrayList;
import java.util.List;

public class ReleaseItem {
    public String name = "";
    public boolean prerelease = false;
    public String publishedAt = "";
    public String htmlUrl = "";
    public String body = "";
    public List<AssetItem> assets = new ArrayList<>();
}

package com.util;

public abstract class SearchResult {
  public abstract String getFileName();
  public abstract long getFileSize();
  public abstract DownloadInfo createDownloadInfo();
}

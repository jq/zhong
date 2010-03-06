package com.feebe.lib.download;

import java.io.FileOutputStream;

/**
 * Stores information about the file in which a download gets saved.
 */
public class DownloadFileInfo {
    String mFileName;
    FileOutputStream mStream;
    int mStatus;

    public DownloadFileInfo(String fileName, FileOutputStream stream, int status) {
        mFileName = fileName;
        mStream = stream;
        mStatus = status;
    }
}

package com.ecr.helper.tool;

import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public class FileServer extends NanoHTTPD {

    private String filePath;

    public FileServer(int port, String filePath) {
        super(port);
        this.filePath = filePath;
    }

   @Override
    public Response serve(IHTTPSession session) {
        Log.d("Debug", "serve() method called");
        File file = new File(filePath);
        if (!file.exists()) {
            Log.d("Debug", "File not found: " + filePath);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found");
        }

        String fileName = file.getName();
//        long lastModified = file.lastModified() / 1000;
        long timestamp = extractTimestamp(fileName);
        Log.d("Debug", "Fileserver file time: " + timestamp);

        if ("/file".equals(session.getUri())) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
            } catch (IOException ioe) {
                Log.e("Debug", "Error reading file", ioe);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading file");
            }
            return newChunkedResponse(Response.Status.OK, "audio/mp3", fis);
        } else {
            JSONObject jsonResponse = new JSONObject();
            try {
                jsonResponse.put("fileName", fileName);
                jsonResponse.put("lastModified", timestamp);
                jsonResponse.put("downloadUrl", "http://localhost:10087/file");
            } catch (JSONException e) {
                Log.e("Debug", "JSON error", e);
                throw new RuntimeException(e);
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse.toString());
        }
    }




public long extractTimestamp(String filename) {
    Log.d("Debug", "Filename: " + filename);

    // Remove the file extension
    String nameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
    Log.d("Debug", "Name without extension: " + nameWithoutExtension);

    // Split the filename by underscores
    String[] parts = nameWithoutExtension.split("_");

    // The timestamp is the last part of the filename
    String timestampStr = parts[parts.length - 1];
    Log.d("Debug", "Extracted timestamp string: " + timestampStr);

    // Check if the last part is a valid number
    try {
        long timestamp = Long.parseLong(timestampStr);
        Log.d("Debug", "Parsed timestamp: " + timestamp);
        return timestamp;
    } catch (NumberFormatException e) {
        // Use the current system timestamp if the filename does not contain a valid timestamp
        long currentTimestamp = new Date().getTime() / 1000;
        Log.d("Debug", "Invalid timestamp, using current system timestamp: " + currentTimestamp);
        return currentTimestamp;
    }
}
}
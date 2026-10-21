package com.example.pathprobe;

import android.content.Context;
import android.os.StatFs;
import android.text.format.Formatter;

import java.io.File;
import java.io.IOException;

/** Read-only inspection of a returned path. */
final class PathStatus {
	private PathStatus() {
	}

	static String describe(Context context, File file) {
		if (file == null) {
			return "null - no path returned";
		}

		StringBuilder out = new StringBuilder();
		boolean exists = file.exists();

		if (exists) {
			out.append(file.isDirectory() ? "exists | directory" : "exists | file");
			out.append(file.canRead() ? " | readable" : " | not readable");
			out.append(file.canWrite() ? " | writable" : " | not writable");
			appendSpace(context, out, file);
		} else {
			out.append("missing | no file at this path");
		}

		try {
			String canonical = file.getCanonicalPath();
			if (!canonical.equals(file.getAbsolutePath())) {
				out.append("\ncanonical: ").append(canonical);
			}
		} catch (IOException ignored) {
			// Canonicalization is best-effort; the absolute path is still shown.
		}

		return out.toString();
	}

	static int colorFor(Context context, File file) {
		if (file == null || !file.exists()) {
			return context.getColor(R.color.status_missing);
		}
		if (!file.canRead() || !file.canWrite()) {
			return context.getColor(R.color.status_limited);
		}
		return context.getColor(R.color.status_ok);
	}

	private static void appendSpace(Context context, StringBuilder out, File file) {
		try {
			StatFs stat = new StatFs(file.getAbsolutePath());
			long total = stat.getTotalBytes();
			if (total <= 0) {
				return;
			}
			long available = stat.getAvailableBytes();
			out.append(" | ")
					.append(Formatter.formatFileSize(context, available))
					.append(" free of ")
					.append(Formatter.formatFileSize(context, total));
		} catch (IllegalArgumentException | SecurityException ignored) {
			// Not every returned path is a mounted filesystem.
		}
	}
}

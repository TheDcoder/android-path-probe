package com.example.pathprobe;

import java.io.File;

/** One path-returning API call and its result. */
final class PathEntry {
	final String label;
	final File file;

	PathEntry(String label, File file) {
		this.label = label;
		this.file = file;
	}

	String path() {
		return file == null ? "null" : file.getAbsolutePath();
	}
}

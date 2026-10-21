package com.example.pathprobe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** A titled group of path entries. */
final class Section {
	final String title;
	final List<PathEntry> entries = new ArrayList<>();

	Section(String title) {
		this.title = title;
	}

	Section add(String label, File file) {
		entries.add(new PathEntry(label, file));
		return this;
	}

	Section addPath(String label, String path) {
		entries.add(new PathEntry(label, path == null ? null : new File(path)));
		return this;
	}

	Section addAll(String label, File[] files) {
		if (files == null) {
			return add(label, null);
		}
		if (files.length == 0) {
			return add(label, null);
		}
		for (int i = 0; i < files.length; i++) {
			add(label + "[" + i + "]", files[i]);
		}
		return this;
	}

	Section addAllPaths(String label, String[] paths) {
		if (paths == null) {
			return add(label, null);
		}
		if (paths.length == 0) {
			return add(label, null);
		}
		for (int i = 0; i < paths.length; i++) {
			addPath(label + "[" + i + "]", paths[i]);
		}
		return this;
	}
}

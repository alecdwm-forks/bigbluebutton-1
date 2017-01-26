package org.bigbluebutton.deskshare.server.util;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;

public class FilenameGenerator implements IStreamFilenameGenerator {

	/** Path that will store recorded videos */
	public String recordPath = "streams";

	/** Path that contains VOD files */
	public String playbackPath = "streams";

	/** Set if the path is absolute or relative */
	public boolean resolvesAbsolutePath = true;

	public String generateFilename(IScope scope, String name, GenerationType type) {
		// Generate the file name without the extension
		return generateFilename(scope, name, null, type);
	}

	public String generateFilename(IScope scope, String name, String extension, GenerationType type) {
		String filename;
		if (type == GenerationType.RECORD) {
			filename = recordPath + "/" + name;
		} else {
			filename = playbackPath + "/" + name;
		}
		if (extension != null) {
			// add the extension
			filename += extension;
		}
		return filename;
	}

	public boolean resolvesToAbsolutePath() {
		return resolvesAbsolutePath;
	}

	public void setRecordPath(String path) {
		recordPath = path;
	}

	public void setPlaybackPath(String path) {
		playbackPath = path;
	}

	public void setAbsolutePath(boolean absolute) {
		resolvesAbsolutePath = absolute;
	}
} 
package data;

import java.util.Date;
import java.util.UUID;

public class ImageMetadata {
	UUID id = UUID.randomUUID();
	Resolution resolution = Resolution.ORIGIN;
	Date uploadedDate = new Date();
}

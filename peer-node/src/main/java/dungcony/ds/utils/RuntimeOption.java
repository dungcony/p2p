package dungcony.ds.utils;

import java.nio.file.Path;

public record RuntimeOption(Path dataRoot, Integer peerPort) {
}
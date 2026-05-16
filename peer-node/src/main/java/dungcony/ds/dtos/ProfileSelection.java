package dungcony.ds.dtos;

import dungcony.ds.config.PeerConfig;

public record ProfileSelection(
        PeerConfig config,
        boolean editBeforeStart) {
}

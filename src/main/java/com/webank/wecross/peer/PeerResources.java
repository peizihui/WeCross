package com.webank.wecross.peer;

import com.webank.wecross.p2p.netty.common.Peer;
import com.webank.wecross.resource.ResourceInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerResources {
    private Logger logger = LoggerFactory.getLogger(PeerResources.class);

    private Set<PeerInfo> peerInfos;
    private Map<String, Map<String, Set<PeerInfo>>> path2Checksum2PeerInfos = new HashMap<>();

    private Map<String, String> resource2Checksum = new HashMap<>();
    private Map<String, Set<Peer>> resource2Peers = new HashMap<>();
    private boolean hasMyselfResource = false;
    private boolean dirty = true;

    public PeerResources(Set<PeerInfo> peerInfos) {
        this.peerInfos = peerInfos;
    }

    public void updateMyselfResources(Map<String, ResourceInfo> resourceInfoMap) {
        if (resourceInfoMap != null) {
            PeerInfo myself = new PeerInfo(new Peer("myself"));

            Set<ResourceInfo> resourceInfos = new HashSet<>();
            for (ResourceInfo info : resourceInfoMap.values()) {
                resourceInfos.add(info);
            }

            myself.setResourceInfos(resourceInfos);

            peerInfos.add(myself);
        }
        hasMyselfResource = true;
        dirty = true;
    }

    private void parse() {
        if (!hasMyselfResource) {
            logger.error("Myself resources has not been set.");
            return;
        }

        // parse path2Checksum2PeerInfos
        for (PeerInfo peerInfo : peerInfos) {
            for (ResourceInfo resourceInfo : peerInfo.getResourceInfos()) {
                String path = resourceInfo.getPath();
                String checksum = resourceInfo.getChecksum();

                Map<String, Set<PeerInfo>> theChecksum2PeerInfos =
                        path2Checksum2PeerInfos.get(path);
                if (theChecksum2PeerInfos == null) {
                    theChecksum2PeerInfos = new HashMap<>();
                }

                Set<PeerInfo> thePeerInfos = theChecksum2PeerInfos.get(checksum);
                if (thePeerInfos == null) {
                    thePeerInfos = new HashSet<>();
                }

                thePeerInfos.add(peerInfo);

                theChecksum2PeerInfos.putIfAbsent(checksum, thePeerInfos);
                path2Checksum2PeerInfos.putIfAbsent(path, theChecksum2PeerInfos);
            }
        }

        // parse resource2Checksum and resource2Peers
        for (Map.Entry<String, Map<String, Set<PeerInfo>>> entry :
                path2Checksum2PeerInfos.entrySet()) {
            String path = entry.getKey();
            Map<String, Set<PeerInfo>> checksum2PeerInfos = entry.getValue();
            if (checksum2PeerInfos.size() > 1) {
                // ignore invalid resources
                continue;
            }

            for (Map.Entry<String, Set<PeerInfo>> subEntry : checksum2PeerInfos.entrySet()) {
                // Only 1 loop

                // update resource2Checksum
                resource2Checksum.put(path, subEntry.getKey());

                // update resource2Peers
                Set<Peer> peers = resource2Peers.get(path);
                if (peers == null) {
                    peers = new HashSet<>();
                }

                if (subEntry.getValue() == null) {
                    continue;
                }

                for (PeerInfo peerInfo : subEntry.getValue()) {
                    peers.add(peerInfo.getPeer());
                }

                resource2Peers.put(path, peers);
                break;
            }
        }

        dirty = false;
    }

    public void loggingInvalidResources() {
        if (dirty) {
            parse();
        }

        for (Map.Entry<String, Map<String, Set<PeerInfo>>> entry :
                path2Checksum2PeerInfos.entrySet()) {
            if (entry.getValue().size() > 1) {
                // same path has not unique checksum

                String warningContent =
                        "Receive same path with diffrent checksum, path: " + entry.getKey() + " [";
                for (Map.Entry<String, Set<PeerInfo>> errorEntry : entry.getValue().entrySet()) {
                    for (PeerInfo errorPeerInfo : errorEntry.getValue()) {
                        warningContent +=
                                "{checksum: "
                                        + errorEntry.getKey()
                                        + ", peer: "
                                        + errorPeerInfo.toString()
                                        + "},";
                    }
                }
                warningContent += "]";
                logger.warn(warningContent);
            }
        }
    }

    public Map<String, String> getResource2Checksum() {
        if (dirty) {
            parse();
        }
        return resource2Checksum;
    }

    public Map<String, Set<Peer>> getResource2Peers() {
        if (dirty) {
            parse();
        }
        return resource2Peers;
    }

    public void noteDirty() {
        dirty = true;
    }
}
